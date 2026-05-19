package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.server.MinecraftServer;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DimensionTypeExtractor {
    private static final String DIMENSION_TYPE_OUTPUT_FILE_PATH = "spinel_extractor/dimension_types.json";

    public static void extract(MinecraftServer server) {
        DimensionTypeExtractor extractor = new DimensionTypeExtractor(server);
        JsonExtractionRepository repository = new JsonExtractionRepository(DIMENSION_TYPE_OUTPUT_FILE_PATH);
        repository.save(extractor.extractDimensionTypesFile());
    }

    private final MinecraftServer server;

    private DimensionTypeExtractor(MinecraftServer server) {
        this.server = server;
    }

    private Map<String, Object> extractDimensionTypesFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("dimension_types", extractDimensionTypes());
        return extractionResult;
    }

    private Map<String, Object> extractDimensionTypes() {
        Registry<DimensionType> dimensionTypeRegistry = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
        Map<String, Object> dimensionTypes = new LinkedHashMap<>();
        dimensionTypeRegistry.entrySet()
                .stream()
                .sorted((left, right) -> left.getKey().identifier().toString().compareTo(right.getKey().identifier().toString()))
                .forEach(entry -> dimensionTypes.put(entry.getKey().identifier().toString(), encodeDimensionType(entry.getValue())));
        return dimensionTypes;
    }

    private Map<String, Object> encodeDimensionType(DimensionType dimensionType) {
        Map<String, Object> encodedDimensionType = new LinkedHashMap<>();
        encodedDimensionType.put("has_fixed_time", dimensionType.hasFixedTime());
        encodedDimensionType.put("has_skylight", dimensionType.hasSkyLight());
        encodedDimensionType.put("has_ceiling", dimensionType.hasCeiling());
        encodedDimensionType.put("coordinate_scale", dimensionType.coordinateScale());
        encodedDimensionType.put("min_y", dimensionType.minY());
        encodedDimensionType.put("height", dimensionType.height());
        encodedDimensionType.put("logical_height", dimensionType.logicalHeight());
        encodedDimensionType.put("infiniburn", "#" + dimensionType.infiniburn().location());
        encodedDimensionType.put("ambient_light", dimensionType.ambientLight());
        encodedDimensionType.put("monster_spawn_light_level", encodeMonsterSpawnLightLevel(dimensionType.monsterSpawnLightTest()));
        encodedDimensionType.put("monster_spawn_block_light_limit", dimensionType.monsterSpawnBlockLightLimit());
        encodedDimensionType.put("skybox", dimensionType.skybox().getSerializedName());
        encodedDimensionType.put("cardinal_light_type", dimensionType.cardinalLightType().getSerializedName());
        return encodedDimensionType;
    }

    private Object encodeMonsterSpawnLightLevel(IntProvider monsterSpawnLightLevel) {
        int minimumLightLevel = monsterSpawnLightLevel.getMinValue();
        int maximumLightLevel = monsterSpawnLightLevel.getMaxValue();
        if (minimumLightLevel == maximumLightLevel) {
            return minimumLightLevel;
        }

        Map<String, Object> encodedMonsterSpawnLightLevel = new LinkedHashMap<>();
        encodedMonsterSpawnLightLevel.put("type", "minecraft:uniform");
        encodedMonsterSpawnLightLevel.put("min_inclusive", minimumLightLevel);
        encodedMonsterSpawnLightLevel.put("max_inclusive", maximumLightLevel);
        return encodedMonsterSpawnLightLevel;
    }
}
