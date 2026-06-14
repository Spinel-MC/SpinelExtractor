package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.vaprant.spinelextractor.blocks.BlockShapeRegistry;
import net.vaprant.spinelextractor.blocks.definition.BlockDefinition;
import net.vaprant.spinelextractor.blocks.definition.BlockFlags;
import net.vaprant.spinelextractor.blocks.definition.BlockStateDefinition;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockExtractor {
    private static final String BLOCK_OUTPUT_FILE_PATH = "spinel_extractor/blocks.json";
    private final BlockShapeRegistry blockShapeRegistry = new BlockShapeRegistry();

    public static void extract() {
        BlockExtractor extractor = new BlockExtractor();
        JsonExtractionRepository repository = new JsonExtractionRepository(BLOCK_OUTPUT_FILE_PATH);
        repository.save(extractor.extractBlocksFile());
    }

    private Map<String, Object> extractBlocksFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("blocks", extractBlocks());
        extractionResult.put("block_shapes", blockShapeRegistry.shapes());
        return extractionResult;
    }

    private Map<String, BlockDefinition> extractBlocks() {
        Map<String, BlockDefinition> blocks = new LinkedHashMap<>();
        BuiltInRegistries.BLOCK.forEach(block -> blocks.put(blockRegistryName(block), extractBlock(block)));
        return blocks;
    }

    private BlockDefinition extractBlock(Block block) {
        BlockState defaultState = block.defaultBlockState();

        return new BlockDefinition(
                BuiltInRegistries.BLOCK.getId(block),
                BuiltInRegistries.BLOCK.getKey(block).getPath(),
                block.getDescriptionId(),
                Block.getId(defaultState),
                extractBlockEntityType(defaultState),
                extractProperties(block),
                extractStates(block),
                block.defaultDestroyTime(),
                block.getFriction(),
                defaultState.requiresCorrectToolForDrops(),
                extractFlags(defaultState)
        );
    }

    private String extractBlockEntityType(BlockState blockState) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.stream()
                .filter(blockEntityType -> blockEntityType.isValid(blockState))
                .findFirst()
                .map(blockEntityType -> BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType).getPath())
                .orElse(null);
    }

    private Map<String, List<String>> extractProperties(Block block) {
        Map<String, List<String>> properties = new LinkedHashMap<>();

        block.getStateDefinition().getProperties()
                .stream()
                .sorted(Comparator.comparing(Property::getName))
                .forEach(property -> properties.put(property.getName(), extractPropertyValues(property)));

        return properties;
    }

    private <T extends Comparable<T>> List<String> extractPropertyValues(Property<T> property) {
        return property.getPossibleValues()
                .stream()
                .map(property::getName)
                .toList();
    }

    private Map<String, BlockStateDefinition> extractStates(Block block) {
        Map<String, BlockStateDefinition> states = new LinkedHashMap<>();

        block.getStateDefinition().getPossibleStates()
                .stream()
                .sorted(Comparator.comparingInt(Block::getId))
                .forEach(blockState -> states.put(stateQuery(blockState), extractState(blockState)));

        return states;
    }

    private BlockStateDefinition extractState(BlockState blockState) {
        return new BlockStateDefinition(
                Block.getId(blockState),
                extractStateProperties(blockState),
                blockState.getLightEmission(),
                blockState.getLightBlock(),
                blockState.propagatesSkylightDown(),
                blockState.useShapeForLightOcclusion(),
                blockShapeRegistry.register(blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)),
                blockShapeRegistry.register(blockState.getOcclusionShape()),
                extractFaceOcclusionShapes(blockState)
        );
    }

    private Map<String, Integer> extractFaceOcclusionShapes(BlockState blockState) {
        Map<String, Integer> faceOcclusionShapes = new LinkedHashMap<>();
        for (Direction direction : Direction.values()) {
            faceOcclusionShapes.put(
                    direction.getName(),
                    blockShapeRegistry.register(blockState.getFaceOcclusionShape(direction))
            );
        }
        return faceOcclusionShapes;
    }

    private Map<String, String> extractStateProperties(BlockState blockState) {
        Map<String, String> properties = new LinkedHashMap<>();

        blockState.getProperties()
                .stream()
                .sorted(Comparator.comparing(Property::getName))
                .forEach(property -> properties.put(property.getName(), propertyValue(blockState, property)));

        return properties;
    }

    private <T extends Comparable<T>> String propertyValue(BlockState blockState, Property<T> property) {
        return property.getName(blockState.getValue(property));
    }

    private String stateQuery(BlockState blockState) {
        Map<String, String> stateProperties = extractStateProperties(blockState);

        return stateProperties.entrySet()
                .stream()
                .map(property -> property.getKey() + "=" + property.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private BlockFlags extractFlags(BlockState defaultState) {
        return new BlockFlags(
                defaultState.isAir(),
                defaultState.isSolid(),
                defaultState.liquid(),
                defaultState.canBeReplaced()
        );
    }

    private String blockRegistryName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}
