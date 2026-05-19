package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StaticRegistryTagExtractor {
    private static final String TAGS_OUTPUT_FILE_PATH = "spinel_extractor/static_registry_tags.json";

    public static void extract() {
        StaticRegistryTagExtractor extractor = new StaticRegistryTagExtractor();
        new JsonExtractionRepository(TAGS_OUTPUT_FILE_PATH).save(extractor.extractTagFile());
    }

    private Map<String, Object> extractTagFile() {
        Map<String, Object> registries = new LinkedHashMap<>();
        registries.put("block", extractTags(BuiltInRegistries.BLOCK));
        registries.put("item", extractTags(BuiltInRegistries.ITEM));
        return registries;
    }

    private Map<String, List<String>> extractTags(Registry<?> registry) {
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
}
