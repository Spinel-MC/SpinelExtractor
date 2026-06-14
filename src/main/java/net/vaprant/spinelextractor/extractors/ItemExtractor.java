package net.vaprant.spinelextractor.extractors;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemExtractor {
    private static final String ITEM_OUTPUT_FILE_PATH = "spinel_extractor/items.json";
    private final RegistryOps<JsonElement> registryJsonOps;

    public ItemExtractor(MinecraftServer server) {
        registryJsonOps = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
    }

    public static void extract(MinecraftServer server) {
        ItemExtractor extractor = new ItemExtractor(server);
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
        DataComponentMap components = item.components();
        String blockItem = item instanceof BlockItem blockItemValue
                ? BuiltInRegistries.BLOCK.getKey(blockItemValue.getBlock()).getPath()
                : null;
        return new ItemDefinition(
                BuiltInRegistries.ITEM.getId(item),
                BuiltInRegistries.ITEM.getKey(item).getPath(),
                blockItem,
                components.getOrDefault(DataComponents.MAX_STACK_SIZE, 64),
                extractComponents(components)
        );
    }

    private JsonElement extractComponents(DataComponentMap components) {
        return DataComponentMap.CODEC.encodeStart(registryJsonOps, components).getOrThrow();
    }

    private record ItemDefinition(
            int id,
            String name,
            String blockItem,
            int maxStackSize,
            JsonElement components
    ) {}
}
