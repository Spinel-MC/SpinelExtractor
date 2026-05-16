package net.vaprant.spinelextractor.blocks.definition;

import java.util.Map;

public record BlockStateDefinition(
        int id,
        Map<String, String> properties
) {
}
