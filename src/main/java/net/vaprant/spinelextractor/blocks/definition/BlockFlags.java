package net.vaprant.spinelextractor.blocks.definition;

public record BlockFlags(
        boolean air,
        boolean solid,
        boolean liquid,
        boolean replaceable
) {
}
