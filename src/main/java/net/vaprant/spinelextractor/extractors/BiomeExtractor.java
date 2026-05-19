package net.vaprant.spinelextractor.extractors;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.MinecraftServer;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BiomeExtractor {
    private static final String BIOME_OUTPUT_FILE_PATH = "spinel_extractor/biomes.json";

    public static void extract(MinecraftServer server) {
        BiomeExtractor extractor = new BiomeExtractor(server);
        JsonExtractionRepository repository = new JsonExtractionRepository(BIOME_OUTPUT_FILE_PATH);
        repository.save(extractor.extractBiomesFile());
    }

    private final MinecraftServer server;

    private BiomeExtractor(MinecraftServer server) {
        this.server = server;
    }

    private Map<String, Object> extractBiomesFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("biomes", extractBiomes());
        return extractionResult;
    }

    private Map<String, JsonElement> extractBiomes() {
        Registry<Biome> biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Map<String, JsonElement> biomes = new LinkedHashMap<>();
        biomeRegistry.entrySet()
                .stream()
                .sorted((left, right) -> left.getKey().identifier().toString().compareTo(right.getKey().identifier().toString()))
                .forEach(entry -> biomes.put(entry.getKey().identifier().toString(), encodeBiome(entry.getValue())));
        return biomes;
    }

    private JsonElement encodeBiome(Biome biome) {
        return Biome.NETWORK_CODEC.encodeStart(JsonOps.INSTANCE, biome).getOrThrow();
    }
}
