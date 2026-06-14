package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SoundEventExtractor {
    private static final String SOUND_EVENT_OUTPUT_FILE_PATH = "spinel_extractor/sound_events.json";

    public static void extract() {
        SoundEventExtractor extractor = new SoundEventExtractor();
        new JsonExtractionRepository(SOUND_EVENT_OUTPUT_FILE_PATH).save(extractor.extractSoundEventsFile());
    }

    private Map<String, Object> extractSoundEventsFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("soundEvents", extractSoundEvents());
        return extractionResult;
    }

    private List<SoundEventDefinition> extractSoundEvents() {
        return BuiltInRegistries.SOUND_EVENT
                .stream()
                .sorted(Comparator.comparingInt(BuiltInRegistries.SOUND_EVENT::getId))
                .map(this::extractSoundEvent)
                .toList();
    }

    private SoundEventDefinition extractSoundEvent(SoundEvent soundEvent) {
        return new SoundEventDefinition(
                BuiltInRegistries.SOUND_EVENT.getId(soundEvent),
                BuiltInRegistries.SOUND_EVENT.getKey(soundEvent).getPath()
        );
    }

    private record SoundEventDefinition(
            int id,
            String name
    ) {}
}
