package net.vaprant.spinelextractor.protocol.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PacketCodecContractTest {

    @Test
    void completePrimitivePacketProducesOrderedExactWireOperations() {
        List<FieldDefinition> fields = List.of(
                FieldDefinition.of("entityId", "VarInt"),
                FieldDefinition.of("flags", "UnsignedByte")
        );

        PacketCodecContract contract = PacketCodecContract.from(PrimitivePacket.class, fields, false);

        assertEquals(PacketCodecStatus.EXACT, contract.status());
        assertEquals(
                List.of(
                        WireOperation.field("entityId", "var_int"),
                        WireOperation.field("flags", "unsigned_byte")
                ),
                contract.operations()
        );
        assertTrue(contract.unresolvedReasons().isEmpty());
    }

    @Test
    void string_backed_fields_produce_distinct_exact_wire_operations() {
        List<FieldDefinition> fields = List.of(
                FieldDefinition.of("host", "String"),
                FieldDefinition.of("key", "Identifier")
        );

        PacketCodecContract contract = PacketCodecContract.from(
                StringBackedPacket.class,
                fields,
                false
        );

        assertEquals(PacketCodecStatus.EXACT, contract.status());
        assertEquals(
                List.of(
                        WireOperation.field("host", "string_utf8"),
                        WireOperation.field("key", "identifier")
                ),
                contract.operations()
        );
        assertTrue(contract.unresolvedReasons().isEmpty());
    }

    @Test
    void direct_buffer_families_preserve_their_exact_framing() {
        List<FieldDefinition> fields = List.of(
                FieldDefinition.of("containerId", "ContainerId"),
                FieldDefinition.of("action", "Enum"),
                FieldDefinition.of("timestamp", "Instant"),
                FieldDefinition.of("bytes", "ByteArray"),
                FieldDefinition.of("ints", "VarIntArray"),
                FieldDefinition.of("longs", "LongArray")
        );

        PacketCodecContract contract = PacketCodecContract.from(
                DirectBufferPacket.class,
                fields,
                false
        );

        assertEquals(PacketCodecStatus.EXACT, contract.status());
        assertEquals(
                List.of(
                        WireOperation.field("containerId", "var_int"),
                        WireOperation.field("action", "var_int_enum_ordinal"),
                        WireOperation.field("timestamp", "signed_long_epoch_millis"),
                        WireOperation.field("bytes", "var_int_length_prefixed_bytes"),
                        WireOperation.field("ints", "var_int_length_prefixed_var_ints"),
                        WireOperation.field("longs", "var_int_length_prefixed_longs")
                ),
                contract.operations()
        );
        assertTrue(contract.unresolvedReasons().isEmpty());
    }

    @Test
    void registry_ids_and_chunk_positions_preserve_exact_wire_operations() {
        List<FieldDefinition> fields = List.of(
                FieldDefinition.of("block", "RegistryValue<minecraft:block>"),
                FieldDefinition.of("chunkPos", "ChunkPos")
        );

        PacketCodecContract contract = PacketCodecContract.from(
                RegistryAndChunkPacket.class,
                fields,
                false
        );

        assertEquals(PacketCodecStatus.EXACT, contract.status());
        assertEquals(
                List.of(
                        WireOperation.registryField(
                                "block",
                                "registry_var_int_id",
                                "minecraft:block"
                        ),
                        WireOperation.field("chunkPos", "packed_chunk_position")
                ),
                contract.operations()
        );
        assertTrue(contract.unresolvedReasons().isEmpty());
    }

    @Test
    void nullable_known_types_preserve_presence_prefix_and_inner_operation() {
        List<FieldDefinition> fields = List.of(
                FieldDefinition.of("payload", "Nullable<ByteArray>"),
                FieldDefinition.of("objective", "Nullable<String>"),
                FieldDefinition.of("tab", "Nullable<Identifier>")
        );

        PacketCodecContract contract = PacketCodecContract.from(
                NullablePacket.class,
                fields,
                false
        );

        assertEquals(PacketCodecStatus.EXACT, contract.status());
        assertEquals(
                List.of(
                        WireOperation.nullable(
                                "payload",
                                "var_int_length_prefixed_bytes"
                        ),
                        WireOperation.nullable("objective", "string_utf8"),
                        WireOperation.nullable("tab", "identifier")
                ),
                contract.operations()
        );
        assertTrue(contract.unresolvedReasons().isEmpty());
    }

    @Test
    void omittedPacketComponentMakesCodecContractUnresolved() {
        List<FieldDefinition> partialFields = List.of(FieldDefinition.of("id", "VarInt"));

        PacketCodecContract contract = PacketCodecContract.from(
                EntityMetadataPacket.class,
                partialFields,
                false
        );

        assertEquals(PacketCodecStatus.UNRESOLVED, contract.status());
        assertEquals(List.of(WireOperation.field("id", "var_int")), contract.operations());
        assertTrue(
                contract.unresolvedReasons()
                        .contains("record component trackedValues has no extracted wire operation")
        );
    }

    @Test
    void optional_known_types_preserve_presence_prefix_and_inner_operation() {
        PacketCodecContract contract = PacketCodecContract.from(
                OptionalPacket.class,
                List.of(FieldDefinition.of("id", "Optional<UUID>")),
                false
        );

        assertEquals(
                new PacketCodecContract(
                        PacketCodecStatus.EXACT,
                        List.of(WireOperation.optional("id", "uuid")),
                        List.of()
                ),
                contract
        );
    }

    @Test
    void sentinel_terminated_sequence_preserves_terminator_without_claiming_element_parity() {
        List<EntityDataSerializerDefinition> serializerVariants = List.of(
                new EntityDataSerializerDefinition(
                        0,
                        "byte",
                        PacketCodecStatus.EXACT,
                        "signed_byte"
                ),
                new EntityDataSerializerDefinition(
                        7,
                        "item_stack",
                        PacketCodecStatus.UNRESOLVED,
                        "unknown"
                )
        );
        List<FieldDefinition> fields = List.of(
                FieldDefinition.of("id", "VarInt"),
                FieldDefinition.nested(
                        "packedItems",
                        "SentinelTerminatedArray<EntityMetadataEntry, 255>",
                        List.of(
                                FieldDefinition.of("metadataIndex", "UnsignedByte"),
                                FieldDefinition.of("serializerId", "VarInt"),
                                FieldDefinition.of("value", "EntityDataSerializerDispatch")
                                        .withSerializerVariants(serializerVariants)
                        )
                )
        );

        PacketCodecContract contract = PacketCodecContract.from(SetEntityDataPacket.class, fields, false);

        assertEquals(PacketCodecStatus.UNRESOLVED, contract.status());
        assertEquals(
                List.of(
                        WireOperation.field("id", "var_int"),
                        WireOperation.sentinelTerminatedArray(
                                "packedItems",
                                255,
                                "entity_metadata_entry",
                                List.of(
                                        WireOperation.field("metadataIndex", "unsigned_byte"),
                                        WireOperation.field("serializerId", "var_int"),
                                        WireOperation.dispatch(
                                                "value",
                                                "entity_data_serializer_dispatch",
                                                serializerVariants
                                        )
                                )
                        )
                ),
                contract.operations()
        );
        assertTrue(
                contract.unresolvedReasons()
                        .contains(
                                "field packedItems.value requires unresolved entity data serializer dispatch"
                        )
        );
    }

    private record PrimitivePacket(int entityId, int flags) {}

    private record StringBackedPacket(String host, Object key) {}

    private record DirectBufferPacket(
            int containerId,
            Object action,
            Object timestamp,
            Object bytes,
            Object ints,
            Object longs
    ) {}

    private record RegistryAndChunkPacket(Object block, Object chunkPos) {}

    private record NullablePacket(Object payload, Object objective, Object tab) {}

    private record OptionalPacket(Object id) {}

    private record EntityMetadataPacket(int id, List<Object> trackedValues) {}

    private record SetEntityDataPacket(int id, List<Object> packedItems) {}
}
