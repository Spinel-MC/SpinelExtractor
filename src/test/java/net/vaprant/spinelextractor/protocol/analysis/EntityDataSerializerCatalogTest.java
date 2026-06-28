package net.vaprant.spinelextractor.protocol.analysis;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.vaprant.spinelextractor.protocol.definition.EntityDataSerializerDefinition;
import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;
import net.vaprant.spinelextractor.protocol.definition.PacketCodecStatus;
import net.vaprant.spinelextractor.protocol.definition.WireOperation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class EntityDataSerializerCatalogTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void runtime_serializer_registry_preserves_contiguous_ids_and_wire_descriptors() {
        List<EntityDataSerializerDefinition> serializers =
                new EntityDataSerializerCatalog().extract();

        assertEquals(39, serializers.size());
        assertEquals(
                IntStream.range(0, 39).boxed().toList(),
                serializers.stream().map(EntityDataSerializerDefinition::id).toList()
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        0,
                        "byte",
                        PacketCodecStatus.EXACT,
                        "signed_byte"
                ),
                serializers.get(0)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        1,
                        "int",
                        PacketCodecStatus.EXACT,
                        "var_int"
                ),
                serializers.get(1)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        5,
                        "component",
                        PacketCodecStatus.EXACT,
                        "nbt_tag"
                ),
                serializers.get(5)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        6,
                        "optional_component",
                        PacketCodecStatus.EXACT,
                        "optional_boolean_nbt_tag"
                ),
                serializers.get(6)
        );
        assertEquals("boolean", serializers.get(8).name());
        assertEquals(
                new EntityDataSerializerDefinition(
                        9,
                        "rotations",
                        PacketCodecStatus.EXACT,
                        "three_floats"
                ),
                serializers.get(9)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        10,
                        "block_pos",
                        PacketCodecStatus.EXACT,
                        "packed_block_position"
                ),
                serializers.get(10)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        11,
                        "optional_block_pos",
                        PacketCodecStatus.EXACT,
                        "optional_boolean_packed_block_position"
                ),
                serializers.get(11)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        12,
                        "direction",
                        PacketCodecStatus.EXACT,
                        "var_int_id"
                ),
                serializers.get(12)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        13,
                        "optional_living_entity_reference",
                        PacketCodecStatus.EXACT,
                        "optional_boolean_uuid"
                ),
                serializers.get(13)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        14,
                        "block_state",
                        PacketCodecStatus.EXACT,
                        "var_int_id"
                ),
                serializers.get(14)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        15,
                        "optional_block_state",
                        PacketCodecStatus.EXACT,
                        "zero_sentinel_var_int_id"
                ),
                serializers.get(15)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        18,
                        "villager_data",
                        PacketCodecStatus.EXACT,
                        "composite",
                        null,
                        List.of(
                                WireOperation.registryField(
                                        "type",
                                        "registry_holder_var_int",
                                        "minecraft:villager_type"
                                ),
                                WireOperation.registryField(
                                        "profession",
                                        "registry_holder_var_int",
                                        "minecraft:villager_profession"
                                ),
                                WireOperation.field("level", "var_int")
                        )
                ),
                serializers.get(18)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        19,
                        "optional_unsigned_int",
                        PacketCodecStatus.EXACT,
                        "plus_one_optional_var_int"
                ),
                serializers.get(19)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        20,
                        "pose",
                        PacketCodecStatus.EXACT,
                        "var_int_id"
                ),
                serializers.get(20)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        21,
                        "cat_variant",
                        PacketCodecStatus.EXACT,
                        "registry_holder_var_int",
                        "minecraft:cat_variant"
                ),
                serializers.get(21)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        27,
                        "chicken_variant",
                        PacketCodecStatus.EXACT,
                        "registry_holder_var_int",
                        "minecraft:chicken_variant"
                ),
                serializers.get(27)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        29,
                        "optional_global_pos",
                        PacketCodecStatus.EXACT,
                        "optional_boolean_identifier_packed_block_position"
                ),
                serializers.get(29)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        30,
                        "painting_variant",
                        PacketCodecStatus.EXACT,
                        "zero_inline_plus_one_registry_holder",
                        "minecraft:painting_variant",
                        List.of(
                                WireOperation.field("width", "var_int"),
                                WireOperation.field("height", "var_int"),
                                WireOperation.field("assetId", "identifier"),
                                WireOperation.field("title", "optional_boolean_nbt_tag"),
                                WireOperation.field("author", "optional_boolean_nbt_tag")
                        )
                ),
                serializers.get(30)
        );
        assertEquals(
                new EntityDataSerializerDefinition(
                        38,
                        "humanoid_arm",
                        PacketCodecStatus.EXACT,
                        "var_int_id"
                ),
                serializers.get(38)
        );
    }

    @Test
    void serializer_registry_attaches_only_to_entity_data_dispatch_fields() {
        EntityDataSerializerCatalog catalog = new EntityDataSerializerCatalog();
        List<FieldDefinition> fields = List.of(
                FieldDefinition.of("serializerId", "VarInt"),
                FieldDefinition.of("value", "EntityDataSerializerDispatch")
        );

        List<FieldDefinition> enrichedFields = catalog.attach(fields);

        assertNull(enrichedFields.get(0).serializerVariants());
        assertEquals(catalog.extract(), enrichedFields.get(1).serializerVariants());
    }
}
