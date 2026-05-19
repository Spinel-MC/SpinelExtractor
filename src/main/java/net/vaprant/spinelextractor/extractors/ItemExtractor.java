package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemExtractor {
    private static final String ITEM_OUTPUT_FILE_PATH = "spinel_extractor/items.json";

    public static void extract() {
        ItemExtractor extractor = new ItemExtractor();
        new JsonExtractionRepository(ITEM_OUTPUT_FILE_PATH).save(extractor.extractItemsFile());
    }

    private Map<String, Object> extractItemsFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("items", extractItems());
        return extractionResult;
    }

    private List<ItemDefinition> extractItems() {
        return BuiltInRegistries.ITEM
                .stream()
                .sorted(Comparator.comparingInt(BuiltInRegistries.ITEM::getId))
                .map(this::extractItem)
                .toList();
    }

    private ItemDefinition extractItem(Item item) {
        return new ItemDefinition(
                BuiltInRegistries.ITEM.getId(item),
                BuiltInRegistries.ITEM.getKey(item).getPath()
        );
    }

    private record ItemDefinition(int id, String name) {}
}
