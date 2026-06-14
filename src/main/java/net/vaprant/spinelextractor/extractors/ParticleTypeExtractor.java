package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParticleTypeExtractor {
    private static final String OUTPUT_FILE_PATH = "spinel_extractor/particle_types.json";

    public static void extract() {
        ParticleTypeExtractor extractor = new ParticleTypeExtractor();
        new JsonExtractionRepository(OUTPUT_FILE_PATH).save(extractor.extractParticleTypesFile());
    }

    private Map<String, Object> extractParticleTypesFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("particle_types", extractParticleTypes());
        return extractionResult;
    }

    private List<ParticleTypeDefinition> extractParticleTypes() {
        return BuiltInRegistries.PARTICLE_TYPE
                .stream()
                .sorted(Comparator.comparingInt(BuiltInRegistries.PARTICLE_TYPE::getId))
                .map(this::extractParticleType)
                .toList();
    }

    private ParticleTypeDefinition extractParticleType(ParticleType<?> particleType) {
        var key = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
        return new ParticleTypeDefinition(
                BuiltInRegistries.PARTICLE_TYPE.getId(particleType),
                key.toString(),
                payloadShape(key.getPath())
        );
    }

    private String payloadShape(String particleName) {
        return switch (particleName) {
            case "block", "block_marker", "falling_dust", "dust_pillar", "block_crumble" -> "block_state";
            case "dust" -> "color_scale";
            case "dust_color_transition" -> "color_transition_scale";
            case "item" -> "item_stack";
            case "entity_effect", "tinted_leaves", "flash" -> "alpha_color";
            case "sculk_charge", "dragon_breath" -> "float";
            case "shriek" -> "var_int";
            case "vibration" -> "vibration";
            case "trail" -> "trail";
            case "effect", "instant_effect" -> "color_power";
            default -> "unit";
        };
    }

    private record ParticleTypeDefinition(
            int id,
            String name,
            String payloadShape
    ) {}
}
