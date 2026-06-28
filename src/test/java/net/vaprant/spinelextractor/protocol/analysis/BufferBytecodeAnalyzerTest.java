package net.vaprant.spinelextractor.protocol.analysis;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BufferBytecodeAnalyzerTest {

    @Test
    void only_zero_component_records_are_proven_zero_field_packets() {
        BufferBytecodeAnalyzer analyzer = new BufferBytecodeAnalyzer();

        assertTrue(analyzer.isKnownZeroFieldPacket(EmptyPacket.class));
        assertFalse(analyzer.isKnownZeroFieldPacket(OpaquePacket.class));
    }

    @Test
    void set_entity_data_exposes_its_sentinel_terminated_metadata_sequence() {
        BufferBytecodeAnalyzer analyzer = new BufferBytecodeAnalyzer();

        List<FieldDefinition> fields = analyzer.analyze(ClientboundSetEntityDataPacket.class);

        assertEquals(
                List.of(
                        FieldDefinition.of("id", "VarInt"),
                        FieldDefinition.nested(
                                "packedItems",
                                "SentinelTerminatedArray<EntityMetadataEntry, 255>",
                                List.of(
                                        FieldDefinition.of("metadataIndex", "UnsignedByte"),
                                        FieldDefinition.of("serializerId", "VarInt"),
                                        FieldDefinition.of("value", "EntityDataSerializerDispatch")
                                )
                        )
                ),
                fields
        );
    }

    @Test
    void fixed_and_length_prefixed_long_arrays_remain_distinct() {
        BufferBytecodeAnalyzer analyzer = new BufferBytecodeAnalyzer();

        List<FieldDefinition> fields = analyzer.analyze(ArrayPacket.class);

        assertEquals(
                List.of(
                        FieldDefinition.of("lengthPrefixedLongs", "LongArray"),
                        FieldDefinition.of("fixedLongs", "FixedLongArray")
                ),
                fields
        );
    }

    @Test
    void simple_non_record_packet_completeness_requires_only_direct_field_writes() {
        BufferBytecodeAnalyzer analyzer = new BufferBytecodeAnalyzer();
        List<FieldDefinition> directFields = analyzer.analyze(DirectWritePacket.class);
        List<FieldDefinition> constantFields = analyzer.analyze(ConstantWritePacket.class);
        List<FieldDefinition> delegatedFields = analyzer.analyze(DelegatedWritePacket.class);
        List<FieldDefinition> opaqueFields = analyzer.analyze(OpaqueObjectWritePacket.class);

        assertTrue(analyzer.isCompleteDirectWritePacket(DirectWritePacket.class, directFields));
        assertFalse(analyzer.isCompleteDirectWritePacket(ConstantWritePacket.class, constantFields));
        assertFalse(analyzer.isCompleteDirectWritePacket(DelegatedWritePacket.class, delegatedFields));
        assertFalse(analyzer.isCompleteDirectWritePacket(OpaqueObjectWritePacket.class, opaqueFields));
    }

    @Test
    void nullable_direct_fields_preserve_their_known_inner_wire_types() {
        BufferBytecodeAnalyzer analyzer = new BufferBytecodeAnalyzer();

        assertEquals(
                List.of(
                        FieldDefinition.of("key", "Identifier"),
                        FieldDefinition.of("payload", "Nullable<ByteArray>")
                ),
                analyzer.analyze(ServerboundCookieResponsePacket.class)
        );
        assertEquals(
                List.of(
                        FieldDefinition.of("owner", "String"),
                        FieldDefinition.of("objectiveName", "Nullable<String>")
                ),
                analyzer.analyze(ClientboundResetScorePacket.class)
        );
        assertEquals(
                List.of(FieldDefinition.of("tab", "Nullable<Identifier>")),
                analyzer.analyze(ClientboundSelectAdvancementsTabPacket.class)
        );
    }

    @Test
    void optional_direct_fields_preserve_their_known_inner_wire_types() {
        BufferBytecodeAnalyzer analyzer = new BufferBytecodeAnalyzer();

        assertEquals(
                List.of(FieldDefinition.of("id", "Optional<UUID>")),
                analyzer.analyze(ClientboundResourcePackPopPacket.class)
        );
    }

    private record EmptyPacket() {}

    private static final class OpaquePacket {}

    private record ArrayPacket(long[] lengthPrefixedLongs, long[] fixedLongs) {
        void write(FriendlyByteBuf buffer) {
            buffer.writeLongArray(lengthPrefixedLongs);
            buffer.writeFixedSizeLongArray(fixedLongs);
        }
    }

    private static final class DirectWritePacket {
        private final int id;
        private final boolean enabled;

        private DirectWritePacket(int id, boolean enabled) {
            this.id = id;
            this.enabled = enabled;
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(id);
            buffer.writeBoolean(enabled);
        }
    }

    private static final class ConstantWritePacket {
        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(0);
        }
    }

    private static final class DelegatedWritePacket {
        private final int id;

        private DelegatedWritePacket(int id) {
            this.id = id;
        }

        void write(FriendlyByteBuf buffer) {
            writeId(buffer, id);
        }

        private static void writeId(FriendlyByteBuf buffer, int id) {
            buffer.writeVarInt(id);
        }
    }

    private static final class OpaqueObjectWritePacket {
        void write(FriendlyByteBuf buffer) {
            encode(buffer, this);
        }

        private static void encode(Object buffer, Object packet) {}
    }
}
