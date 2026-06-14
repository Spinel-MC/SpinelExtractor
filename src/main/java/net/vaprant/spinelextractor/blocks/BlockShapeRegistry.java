package net.vaprant.spinelextractor.blocks;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.vaprant.spinelextractor.blocks.definition.BlockShapeBoxDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockShapeRegistry {
    private final Map<List<BlockShapeBoxDefinition>, Integer> shapeIds = new LinkedHashMap<>();

    public int register(VoxelShape shape) {
        List<BlockShapeBoxDefinition> shapeBoxes = shape.toAabbs()
                .stream()
                .map(this::extractShapeBox)
                .toList();
        Integer existingShapeId = shapeIds.get(shapeBoxes);
        if (existingShapeId != null) {
            return existingShapeId;
        }
        int shapeId = shapeIds.size();
        shapeIds.put(shapeBoxes, shapeId);
        return shapeId;
    }

    public List<List<BlockShapeBoxDefinition>> shapes() {
        return new ArrayList<>(shapeIds.keySet());
    }

    private BlockShapeBoxDefinition extractShapeBox(AABB box) {
        return new BlockShapeBoxDefinition(
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ
        );
    }
}
