package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageType;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DamageTypeExtractor {
    private static final String DAMAGE_TYPE_OUTPUT_FILE_PATH = "spinel_extractor/damage_types.json";

    public static void extract(MinecraftServer server) {
        DamageTypeExtractor extractor = new DamageTypeExtractor(server);
        JsonExtractionRepository repository = new JsonExtractionRepository(DAMAGE_TYPE_OUTPUT_FILE_PATH);
        repository.save(extractor.extractDamageTypesFile());
    }

    private final MinecraftServer server;

    private DamageTypeExtractor(MinecraftServer server) {
        this.server = server;
    }

    private Map<String, Object> extractDamageTypesFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("damage_types", extractDamageTypes());
        return extractionResult;
    }

    private Map<String, Object> extractDamageTypes() {
        Registry<DamageType> damageTypeRegistry = server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        Map<String, Object> damageTypes = new LinkedHashMap<>();
        damageTypeRegistry.entrySet()
                .stream()
                .sorted((left, right) -> left.getKey().identifier().toString().compareTo(right.getKey().identifier().toString()))
                .forEach(entry -> damageTypes.put(entry.getKey().identifier().toString(), encodeDamageType(entry.getValue())));
        return damageTypes;
    }

    private Map<String, Object> encodeDamageType(DamageType damageType) {
        Map<String, Object> encodedDamageType = new LinkedHashMap<>();
        encodedDamageType.put("message_id", damageType.msgId());
        encodedDamageType.put("scaling", damageType.scaling().getSerializedName());
        encodedDamageType.put("exhaustion", damageType.exhaustion());
        encodedDamageType.put("effects", damageType.effects().getSerializedName());
        encodedDamageType.put("death_message_type", damageType.deathMessageType().getSerializedName());
        return encodedDamageType;
    }
}
