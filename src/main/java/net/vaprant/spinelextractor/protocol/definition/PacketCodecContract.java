package net.vaprant.spinelextractor.protocol.definition;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PacketCodecContract(
        PacketCodecStatus status,
        List<WireOperation> operations,
        List<String> unresolvedReasons
) {
    private static final String SENTINEL_TERMINATED_ARRAY_PREFIX = "SentinelTerminatedArray<";
    private static final String REGISTRY_VALUE_PREFIX = "RegistryValue<";
    private static final String NULLABLE_PREFIX = "Nullable<";
    private static final String OPTIONAL_PREFIX = "Optional<";
    private static final Map<String, String> EXACT_OPERATIONS = Map.ofEntries(
            Map.entry("Boolean", "boolean"),
            Map.entry("Byte", "signed_byte"),
            Map.entry("UnsignedByte", "unsigned_byte"),
            Map.entry("Short", "signed_short"),
            Map.entry("UnsignedShort", "unsigned_short"),
            Map.entry("Int", "signed_int"),
            Map.entry("VarInt", "var_int"),
            Map.entry("Long", "signed_long"),
            Map.entry("VarLong", "var_long"),
            Map.entry("Float", "float"),
            Map.entry("Double", "double"),
            Map.entry("String", "string_utf8"),
            Map.entry("Identifier", "identifier"),
            Map.entry("UUID", "uuid"),
            Map.entry("ContainerId", "var_int"),
            Map.entry("Enum", "var_int_enum_ordinal"),
            Map.entry("Instant", "signed_long_epoch_millis"),
            Map.entry("ByteArray", "var_int_length_prefixed_bytes"),
            Map.entry("VarIntArray", "var_int_length_prefixed_var_ints"),
            Map.entry("LongArray", "var_int_length_prefixed_longs"),
            Map.entry("Position", "block_position"),
            Map.entry("ChunkPos", "packed_chunk_position")
    );

    public static PacketCodecContract from(
            Class<?> packetClass,
            List<FieldDefinition> fields,
            boolean isPacketCompletenessProven
    ) {
        List<WireOperation> operations = new ArrayList<>();
        List<String> unresolvedReasons = new ArrayList<>();

        fields.forEach(field -> {
            SentinelTerminatedArray sentinelTerminatedArray = parseSentinelTerminatedArray(field.type());
            if (sentinelTerminatedArray != null) {
                List<WireOperation> elementOperations = new ArrayList<>();
                appendOperations(
                        field.name(),
                        field.nestedFields(),
                        elementOperations,
                        unresolvedReasons
                );
                String elementOperation = sentinelTerminatedArray.elementType().equals("EntityMetadataEntry")
                        ? "entity_metadata_entry"
                        : EXACT_OPERATIONS.getOrDefault(
                                sentinelTerminatedArray.elementType(),
                                "unknown"
                        );
                operations.add(WireOperation.sentinelTerminatedArray(
                        field.name(),
                        sentinelTerminatedArray.terminator(),
                        elementOperation,
                        elementOperations.isEmpty() ? null : elementOperations
                ));
                if (elementOperation.equals("unknown")) {
                    unresolvedReasons.add(
                            "field " + field.name() + " has unresolved sentinel element codec"
                    );
                }
                return;
            }

            WireOperation operation = exactOperation(field);
            if (operation == null) {
                unresolvedReasons.add(
                        "field " + field.name() + " uses unresolved wire type " + field.type()
                );
                return;
            }
            operations.add(operation);
        });

        appendMissingRecordComponentReasons(
                packetClass,
                fields,
                isPacketCompletenessProven,
                unresolvedReasons
        );

        PacketCodecStatus status = unresolvedReasons.isEmpty()
                ? PacketCodecStatus.EXACT
                : PacketCodecStatus.UNRESOLVED;
        return new PacketCodecContract(status, operations, unresolvedReasons);
    }

    private static void appendOperations(
            String ownerPath,
            List<FieldDefinition> fields,
            List<WireOperation> operations,
            List<String> unresolvedReasons
    ) {
        if (fields == null) {
            return;
        }

        fields.forEach(field -> {
            if (field.type().equals("EntityDataSerializerDispatch")) {
                operations.add(WireOperation.dispatch(
                        field.name(),
                        "entity_data_serializer_dispatch",
                        field.serializerVariants()
                ));
                unresolvedReasons.add(
                        "field " + ownerPath + "." + field.name()
                                + " requires unresolved entity data serializer dispatch"
                );
                return;
            }

            WireOperation operation = exactOperation(field);
            if (operation == null) {
                unresolvedReasons.add(
                        "field " + ownerPath + "." + field.name()
                                + " uses unresolved wire type " + field.type()
                );
                return;
            }
            operations.add(operation);
        });
    }

    private static WireOperation exactOperation(FieldDefinition field) {
        String nullableElementType = parseWrappedType(field.type(), NULLABLE_PREFIX);
        if (nullableElementType != null) {
            String elementOperation = EXACT_OPERATIONS.get(nullableElementType);
            return elementOperation == null
                    ? null
                    : WireOperation.nullable(field.name(), elementOperation);
        }

        String optionalElementType = parseWrappedType(field.type(), OPTIONAL_PREFIX);
        if (optionalElementType != null) {
            String elementOperation = EXACT_OPERATIONS.get(optionalElementType);
            return elementOperation == null
                    ? null
                    : WireOperation.optional(field.name(), elementOperation);
        }

        String registry = parseRegistryValue(field.type());
        if (registry != null) {
            return WireOperation.registryField(field.name(), "registry_var_int_id", registry);
        }

        String operation = EXACT_OPERATIONS.get(field.type());
        return operation == null ? null : WireOperation.field(field.name(), operation);
    }

    private static String parseRegistryValue(String fieldType) {
        return parseWrappedType(fieldType, REGISTRY_VALUE_PREFIX);
    }

    private static String parseWrappedType(String fieldType, String prefix) {
        boolean hasPrefix = fieldType.startsWith(prefix);
        boolean hasClosingDelimiter = fieldType.endsWith(">");
        return hasPrefix && hasClosingDelimiter
                ? fieldType.substring(prefix.length(), fieldType.length() - 1)
                : null;
    }

    private static SentinelTerminatedArray parseSentinelTerminatedArray(String fieldType) {
        boolean hasSentinelPrefix = fieldType.startsWith(SENTINEL_TERMINATED_ARRAY_PREFIX);
        boolean hasClosingDelimiter = fieldType.endsWith(">");
        if (!hasSentinelPrefix || !hasClosingDelimiter) {
            return null;
        }

        String arguments = fieldType.substring(
                SENTINEL_TERMINATED_ARRAY_PREFIX.length(),
                fieldType.length() - 1
        );
        int separatorIndex = arguments.lastIndexOf(',');
        if (separatorIndex < 0) {
            return null;
        }

        String elementType = arguments.substring(0, separatorIndex).trim();
        String terminator = arguments.substring(separatorIndex + 1).trim();
        try {
            return new SentinelTerminatedArray(elementType, Integer.parseInt(terminator));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void appendMissingRecordComponentReasons(
            Class<?> packetClass,
            List<FieldDefinition> fields,
            boolean isPacketCompletenessProven,
            List<String> unresolvedReasons
    ) {
        if (isPacketCompletenessProven) {
            return;
        }
        if (!packetClass.isRecord()) {
            unresolvedReasons.add("packet class is not a record and completeness could not be proven");
            return;
        }

        Set<String> extractedFieldNames = new LinkedHashSet<>();
        fields.forEach(field -> extractedFieldNames.add(field.name()));
        for (RecordComponent component : packetClass.getRecordComponents()) {
            if (!extractedFieldNames.contains(component.getName())) {
                unresolvedReasons.add(
                        "record component " + component.getName() + " has no extracted wire operation"
                );
            }
        }
    }

    private record SentinelTerminatedArray(String elementType, int terminator) {}
}
