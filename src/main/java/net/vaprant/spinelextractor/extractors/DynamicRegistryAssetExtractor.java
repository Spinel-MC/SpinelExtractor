package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DynamicRegistryAssetExtractor {
    private static final String KEYS_OUTPUT_FILE_PATH = "spinel_extractor/dynamic_registry_keys.json";
    private static final String TAGS_OUTPUT_FILE_PATH = "spinel_extractor/dynamic_registry_tags.json";
    private static final List<RegistrySpec> REGISTRY_SPECS = List.of(
            new RegistrySpec("chat_type", Registries.CHAT_TYPE),
            new RegistrySpec("damage_type", Registries.DAMAGE_TYPE),
            new RegistrySpec("banner_pattern", Registries.BANNER_PATTERN),
            new RegistrySpec("cat_variant", Registries.CAT_VARIANT),
            new RegistrySpec("chicken_variant", Registries.CHICKEN_VARIANT),
            new RegistrySpec("cow_variant", Registries.COW_VARIANT),
            new RegistrySpec("frog_variant", Registries.FROG_VARIANT),
            new RegistrySpec("pig_variant", Registries.PIG_VARIANT),
            new RegistrySpec("painting_variant", Registries.PAINTING_VARIANT),
            new RegistrySpec("trim_material", Registries.TRIM_MATERIAL),
            new RegistrySpec("trim_pattern", Registries.TRIM_PATTERN),
            new RegistrySpec("instrument", Registries.INSTRUMENT),
            new RegistrySpec("jukebox_song", Registries.JUKEBOX_SONG),
            new RegistrySpec("wolf_variant", Registries.WOLF_VARIANT),
            new RegistrySpec("wolf_sound_variant", Registries.WOLF_SOUND_VARIANT),
            new RegistrySpec("dialog", Registries.DIALOG),
            new RegistrySpec("enchantment", Registries.ENCHANTMENT),
            new RegistrySpec("timeline", Registries.TIMELINE),
            new RegistrySpec("zombie_nautilus_variant", Registries.ZOMBIE_NAUTILUS_VARIANT),
            new RegistrySpec("worldgen/biome", Registries.BIOME),
            new RegistrySpec("dimension_type", Registries.DIMENSION_TYPE)
    );

    public static void extract(MinecraftServer server) {
        DynamicRegistryAssetExtractor extractor = new DynamicRegistryAssetExtractor(server);
        new JsonExtractionRepository(KEYS_OUTPUT_FILE_PATH).save(extractor.extractKeyFile());
        new JsonExtractionRepository(TAGS_OUTPUT_FILE_PATH).save(extractor.extractTagFile());
    }

    private final MinecraftServer server;

    private DynamicRegistryAssetExtractor(MinecraftServer server) {
        this.server = server;
    }

    private Map<String, Object> extractKeyFile() {
        Map<String, Object> registries = new LinkedHashMap<>();
        REGISTRY_SPECS.forEach(spec -> registries.put(spec.path(), extractKeys(spec)));
        return registries;
    }

    private Map<String, Object> extractTagFile() {
        Map<String, Object> registries = new LinkedHashMap<>();
        REGISTRY_SPECS.forEach(spec -> registries.put(spec.path(), extractTags(spec)));
        return registries;
    }

    private List<String> extractKeys(RegistrySpec spec) {
        Registry<?> registry = registry(spec);
        return registry
                .entrySet()
                .stream()
                .map(entry -> entry.getKey().identifier().toString())
                .sorted()
                .toList();
    }

    private Map<String, List<String>> extractTags(RegistrySpec spec) {
        Registry<?> registry = registry(spec);
        Map<String, List<String>> tags = new LinkedHashMap<>();
        registry
                .getTags()
                .sorted(Comparator.comparing(tag -> tag.key().location().toString()))
                .forEach(tag -> tags.put(tag.key().location().toString(), extractTagValues(tag)));
        return tags;
    }

    private List<String> extractTagValues(HolderSet.Named<?> tag) {
        return tag
                .stream()
                .map(Holder::unwrapKey)
                .flatMap(Optional::stream)
                .map(key -> key.identifier().toString())
                .sorted()
                .toList();
    }

    private Registry<?> registry(RegistrySpec spec) {
        return server.registryAccess().lookupOrThrow(spec.key());
    }

    private record RegistrySpec(String path, ResourceKey<? extends Registry<?>> key) {}
}
