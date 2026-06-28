package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MobEffectExtractor {
    private static final String OUTPUT_FILE_PATH = "spinel_extractor/mob_effects.json";

    public static void extract(MinecraftServer server) {
        new JsonExtractionRepository(OUTPUT_FILE_PATH).save(Map.of("mob_effects", new MobEffectExtractor(server).extractEntries()));
    }

    private final Registry<MobEffect> mobEffects;

    private MobEffectExtractor(MinecraftServer server) {
        this.mobEffects = server.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
    }

    private List<MobEffectDefinition> extractEntries() {
        return mobEffects.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> mobEffects.getId(entry.getValue())))
                .map(entry -> new MobEffectDefinition(
                        mobEffects.getId(entry.getValue()),
                        entry.getKey().identifier().getPath(),
                        entry.getValue().getDescriptionId(),
                        entry.getValue().getColor(),
                        entry.getValue().isInstantenous()
                ))
                .toList();
    }

    private record MobEffectDefinition(
            int id,
            String name,
            String translationKey,
            int color,
            boolean instantaneous
    ) {}
}