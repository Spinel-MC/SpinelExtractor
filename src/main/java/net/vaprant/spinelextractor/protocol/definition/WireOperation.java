package net.vaprant.spinelextractor.protocol.definition;

import java.util.List;

public record WireOperation(
        String name,
        String operation,
        Integer terminator,
        String elementOperation,
        List<WireOperation> elementOperations,
        List<EntityDataSerializerDefinition> serializerVariants,
        String registry
) {
    public static WireOperation field(String name, String operation) {
        return new WireOperation(name, operation, null, null, null, null, null);
    }

    public static WireOperation registryField(
            String name,
            String operation,
            String registry
    ) {
        return new WireOperation(name, operation, null, null, null, null, registry);
    }

    public static WireOperation nullable(String name, String elementOperation) {
        return new WireOperation(
                name,
                "nullable_boolean_prefixed",
                null,
                elementOperation,
                null,
                null,
                null
        );
    }

    public static WireOperation optional(String name, String elementOperation) {
        return new WireOperation(
                name,
                "optional_boolean_prefixed",
                null,
                elementOperation,
                null,
                null,
                null
        );
    }

    public static WireOperation dispatch(
            String name,
            String operation,
            List<EntityDataSerializerDefinition> serializerVariants
    ) {
        return new WireOperation(
                name,
                operation,
                null,
                null,
                null,
                serializerVariants,
                null
        );
    }

    public static WireOperation sentinelTerminatedArray(
            String name,
            int terminator,
            String elementOperation
    ) {
        return sentinelTerminatedArray(name, terminator, elementOperation, null);
    }

    public static WireOperation sentinelTerminatedArray(
            String name,
            int terminator,
            String elementOperation,
            List<WireOperation> elementOperations
    ) {
        return new WireOperation(
                name,
                "sentinel_terminated_array",
                terminator,
                elementOperation,
                elementOperations,
                null,
                null
        );
    }
}
