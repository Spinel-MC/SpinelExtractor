package net.vaprant.spinelextractor.protocol.analysis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.vaprant.spinelextractor.protocol.definition.EntityDataSerializerDefinition;
import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;
import net.vaprant.spinelextractor.protocol.definition.PacketCodecStatus;
import net.vaprant.spinelextractor.protocol.definition.WireOperation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class EntityDataSerializerCatalog {

    public List<EntityDataSerializerDefinition> extract() {
        return Arrays.stream(EntityDataSerializers.class.getFields())
                .filter(this::isPublicStaticSerializer)
                .map(this::definition)
                .sorted(Comparator.comparingInt(EntityDataSerializerDefinition::id))
                .toList();
    }

    public List<FieldDefinition> attach(List<FieldDefinition> fields) {
        boolean hasEntityDataDispatch = fields.stream().anyMatch(this::hasEntityDataDispatch);
        if (!hasEntityDataDispatch) {
            return fields;
        }

        List<EntityDataSerializerDefinition> serializers = extract();
        return fields.stream()
                .map(field -> attach(field, serializers))
                .toList();
    }

    private boolean hasEntityDataDispatch(FieldDefinition field) {
        if (field.type().equals("EntityDataSerializerDispatch")) {
            return true;
        }
        return field.nestedFields() != null
                && field.nestedFields().stream().anyMatch(this::hasEntityDataDispatch);
    }

    private FieldDefinition attach(
            FieldDefinition field,
            List<EntityDataSerializerDefinition> serializers
    ) {
        FieldDefinition nestedField = field.nestedFields() == null
                ? field
                : field.withNestedFields(
                        field.nestedFields().stream()
                                .map(childField -> attach(childField, serializers))
                                .toList()
                );
        if (!nestedField.type().equals("EntityDataSerializerDispatch")) {
            return nestedField;
        }
        return nestedField.withSerializerVariants(serializers);
    }

    private boolean isPublicStaticSerializer(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && EntityDataSerializer.class.isAssignableFrom(field.getType());
    }

    private EntityDataSerializerDefinition definition(Field field) {
        try {
            EntityDataSerializer<?> serializer = (EntityDataSerializer<?>) field.get(null);
            int serializerId = EntityDataSerializers.getSerializedId(serializer);
            String operation = operation(serializer.codec());
            String registry = registry(serializer.codec());
            List<WireOperation> operations = operations(serializer.codec());
            PacketCodecStatus status = operation.equals("unknown")
                    ? PacketCodecStatus.UNRESOLVED
                    : PacketCodecStatus.EXACT;
            return new EntityDataSerializerDefinition(
                    serializerId,
                    field.getName().toLowerCase(),
                    status,
                    operation,
                    registry,
                    operations
            );
        } catch (IllegalAccessException accessException) {
            throw new ReflectionExtractionException(
                    "Could not access entity data serializer " + field.getName(),
                    accessException
            );
        }
    }

    private String operation(StreamCodec<?, ?> codec) {
        if (codec == ByteBufCodecs.BYTE) {
            return "signed_byte";
        }
        if (codec == ByteBufCodecs.VAR_INT) {
            return "var_int";
        }
        if (codec == ByteBufCodecs.VAR_LONG) {
            return "var_long";
        }
        if (codec == ByteBufCodecs.FLOAT) {
            return "float";
        }
        if (codec == ByteBufCodecs.STRING_UTF8) {
            return "string_utf8";
        }
        if (codec == ComponentSerialization.TRUSTED_STREAM_CODEC) {
            return "nbt_tag";
        }
        if (codec == ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC) {
            return "optional_boolean_nbt_tag";
        }
        if (codec == ByteBufCodecs.BOOL) {
            return "boolean";
        }
        if (codec == Rotations.STREAM_CODEC) {
            return "three_floats";
        }
        if (codec == BlockPos.STREAM_CODEC) {
            return "packed_block_position";
        }
        if (codec == EntityDataSerializers.OPTIONAL_BLOCK_POS.codec()) {
            return "optional_boolean_packed_block_position";
        }
        if (codec.getClass() == Direction.STREAM_CODEC.getClass()) {
            return "var_int_id";
        }
        if (codec == EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE.codec()) {
            return "optional_boolean_uuid";
        }
        if (codec == EntityDataSerializers.OPTIONAL_BLOCK_STATE.codec()) {
            return "zero_sentinel_var_int_id";
        }
        if (codec == EntityDataSerializers.OPTIONAL_UNSIGNED_INT.codec()) {
            return "plus_one_optional_var_int";
        }
        if (codec == EntityDataSerializers.OPTIONAL_GLOBAL_POS.codec()) {
            return "optional_boolean_identifier_packed_block_position";
        }
        if (codec == EntityDataSerializers.VILLAGER_DATA.codec()) {
            return "composite";
        }
        if (codec == EntityDataSerializers.PAINTING_VARIANT.codec()) {
            return "zero_inline_plus_one_registry_holder";
        }
        if (isRegistryHolderCodec(codec)) {
            return "registry_holder_var_int";
        }
        if (codec == ByteBufCodecs.VECTOR3F) {
            return "vector3f";
        }
        if (codec == ByteBufCodecs.QUATERNIONF) {
            return "quaternionf";
        }
        return "unknown";
    }

    private List<WireOperation> operations(StreamCodec<?, ?> codec) {
        if (codec != EntityDataSerializers.VILLAGER_DATA.codec()) {
            return paintingVariantOperations(codec);
        }

        return List.of(
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
        );
    }

    private List<WireOperation> paintingVariantOperations(StreamCodec<?, ?> codec) {
        if (codec != EntityDataSerializers.PAINTING_VARIANT.codec()) {
            return null;
        }

        return List.of(
                WireOperation.field("width", "var_int"),
                WireOperation.field("height", "var_int"),
                WireOperation.field("assetId", "identifier"),
                WireOperation.field("title", "optional_boolean_nbt_tag"),
                WireOperation.field("author", "optional_boolean_nbt_tag")
        );
    }

    private String registry(StreamCodec<?, ?> codec) {
        if (!isRegistryHolderCodec(codec)
                && codec != EntityDataSerializers.PAINTING_VARIANT.codec()) {
            return null;
        }

        try {
            Field registryKeyField = codec.getClass().getDeclaredField("val$registryKey");
            registryKeyField.setAccessible(true);
            ResourceKey<?> registryKey = (ResourceKey<?>) registryKeyField.get(codec);
            return registryKey.identifier().toString();
        } catch (ReflectiveOperationException reflectionException) {
            throw new ReflectionExtractionException(
                    "Could not extract registry identity from " + codec.getClass().getName(),
                    reflectionException
            );
        }
    }

    private boolean isRegistryHolderCodec(StreamCodec<?, ?> codec) {
        return codec.getClass() == EntityDataSerializers.CAT_VARIANT.codec().getClass();
    }
}
