package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnchantmentExtractor {
    private static final String OUTPUT_FILE_PATH = "spinel_extractor/enchantments.json";

    public static void extract(MinecraftServer server) {
        new JsonExtractionRepository(OUTPUT_FILE_PATH).save(new EnchantmentExtractor(server).extractEntries());
    }

    private final RegistryOps<Tag> registryNbtOps;
    private final Registry<Enchantment> enchantments;

    private EnchantmentExtractor(MinecraftServer server) {
        this.registryNbtOps = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
        this.enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    }

    private Map<String, Object> extractEntries() {
        Map<String, Object> entries = new LinkedHashMap<>();
        enchantments.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().identifier().toString()))
                .forEach(entry -> entries.put(
                        entry.getKey().identifier().toString(),
                        Enchantment.DIRECT_CODEC.encodeStart(registryNbtOps, entry.getValue()).getOrThrow().toString()
                ));
        return entries;
    }
}