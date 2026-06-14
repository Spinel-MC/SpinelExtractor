package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StaticProtocolRegistryExtractor {
    private static final String OUTPUT_FILE_PATH = "spinel_extractor/static_protocol_registries.json";

    public static void extract() {
        StaticProtocolRegistryExtractor extractor = new StaticProtocolRegistryExtractor();
        new JsonExtractionRepository(OUTPUT_FILE_PATH).save(extractor.extractRegistryFile());
    }

    private Map<String, Object> extractRegistryFile() {
        Map<String, Object> registries = new LinkedHashMap<>();
        registries.put("villager_type", extractEntries(BuiltInRegistries.VILLAGER_TYPE));
        registries.put("villager_profession", extractEntries(BuiltInRegistries.VILLAGER_PROFESSION));
        return registries;
    }

    private <T> List<StaticProtocolRegistryEntry> extractEntries(Registry<T> registry) {
        return registry
                .stream()
                .sorted(Comparator.comparingInt(registry::getId))
                .map(entry -> new StaticProtocolRegistryEntry(registry.getId(entry), registry.getKey(entry).toString()))
                .toList();
    }

    private record StaticProtocolRegistryEntry(int id, String name) {}
}
