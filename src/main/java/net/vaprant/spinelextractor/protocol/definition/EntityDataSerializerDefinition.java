package net.vaprant.spinelextractor.protocol.definition;

import java.util.List;

public record EntityDataSerializerDefinition(
        int id,
        String name,
        PacketCodecStatus status,
        String operation,
        String registry,
        List<WireOperation> operations
) {
    public EntityDataSerializerDefinition(
            int id,
            String name,
            PacketCodecStatus status,
            String operation
    ) {
        this(id, name, status, operation, null, null);
    }

    public EntityDataSerializerDefinition(
            int id,
            String name,
            PacketCodecStatus status,
            String operation,
            String registry
    ) {
        this(id, name, status, operation, registry, null);
    }
}
