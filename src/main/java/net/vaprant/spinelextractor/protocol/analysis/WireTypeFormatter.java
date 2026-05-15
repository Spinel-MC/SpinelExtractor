package net.vaprant.spinelextractor.protocol.analysis;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public final class WireTypeFormatter {

    public String formatType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return formatParameterizedType(parameterizedType);
        }

        if (type instanceof Class<?> classType) {
            return formatClassType(classType);
        }

        if (type instanceof WildcardType wildcardType) {
            Type[] upperBounds = wildcardType.getUpperBounds();
            return upperBounds.length > 0 && upperBounds[0] != Object.class ? formatType(upperBounds[0]) : "?";
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            return typeVariable.getName();
        }

        return "unknown";
    }

    private String formatParameterizedType(ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        Type[] arguments = parameterizedType.getActualTypeArguments();
        String rawTypeName = rawType.getTypeName();

        if (isCollectionType(rawTypeName) && arguments.length == 1) {
            return "PrefixedArray<" + formatType(arguments[0]) + ">";
        }

        if (isMapType(rawTypeName) && arguments.length == 2) {
            return "Map<" + formatType(arguments[0]) + ", " + formatType(arguments[1]) + ">";
        }

        if (rawTypeName.equals("java.util.Optional") && arguments.length == 1) {
            return "Optional<" + formatType(arguments[0]) + ">";
        }

        if (rawTypeName.equals("com.mojang.datafixers.util.Either") && arguments.length == 2) {
            return "Either<" + formatType(arguments[0]) + ", " + formatType(arguments[1]) + ">";
        }

        if (rawTypeName.equals("java.util.EnumSet") && arguments.length == 1) {
            return "EnumSet<" + formatType(arguments[0]) + ">";
        }

        return formatClassType((Class<?>) rawType);
    }

    private String formatClassType(Class<?> classType) {
        if (classType.isArray()) {
            Class<?> componentType = classType.getComponentType();
            if (componentType == byte.class) {
                return "ByteArray";
            }
            if (componentType == int.class) {
                return "VarIntArray";
            }
            if (componentType == long.class) {
                return "LongArray";
            }
            return formatClassType(componentType) + "[]";
        }

        if (classType.isEnum()) {
            return "Enum<" + classType.getSimpleName() + ">";
        }

        if (classType.isPrimitive()) {
            return switch (classType.getName()) {
                case "boolean" -> "Boolean";
                case "byte" -> "Byte";
                case "short" -> "Short";
                case "int" -> "Int";
                case "long" -> "Long";
                case "float" -> "Float";
                case "double" -> "Double";
                default -> classType.getSimpleName();
            };
        }

        String qualifiedName = classType.getName();
        return switch (qualifiedName) {
            case "java.lang.Boolean" -> "Boolean";
            case "java.lang.Byte" -> "Byte";
            case "java.lang.Short" -> "Short";
            case "java.lang.Integer" -> "Int";
            case "java.lang.Long" -> "Long";
            case "java.lang.Float" -> "Float";
            case "java.lang.Double" -> "Double";
            case "java.lang.String" -> "String";
            case "java.util.UUID" -> "UUID";
            case "java.time.Instant" -> "Instant";
            case "java.security.PublicKey" -> "PublicKey";
            case "java.util.BitSet" -> "BitSet";
            case "it.unimi.dsi.fastutil.ints.IntList" -> "VarIntArray";
            case "net.minecraft.resources.Identifier" -> "Identifier";
            case "net.minecraft.resources.ResourceKey" -> "Identifier";
            case "net.minecraft.core.BlockPos" -> "Position";
            case "net.minecraft.world.level.ChunkPos" -> "ChunkPos";
            case "net.minecraft.core.GlobalPos" -> "GlobalPos";
            case "net.minecraft.nbt.Tag", "net.minecraft.nbt.CompoundTag" -> "NBT";
            case "org.joml.Vector3f", "org.joml.Vector3fc" -> "Vector3f";
            case "org.joml.Quaternionf", "org.joml.Quaternionfc" -> "Quaternion";
            case "net.minecraft.world.phys.Vec3" -> "Vec3";
            case "net.minecraft.world.phys.BlockHitResult" -> "BlockHitResult";
            case "net.minecraft.world.level.block.Block" -> "RegistryValue<minecraft:block>";
            case "net.minecraft.world.item.Item" -> "RegistryValue<minecraft:item>";
            case "net.minecraft.world.level.material.Fluid" -> "RegistryValue<minecraft:fluid>";
            case "net.minecraft.sounds.SoundEvent" -> "RegistryValue<minecraft:sound_event>";
            case "net.minecraft.world.level.block.entity.BlockEntityType" -> "RegistryValue<minecraft:block_entity_type>";
            default -> "unknown";
        };
    }

    private boolean isCollectionType(String rawTypeName) {
        return rawTypeName.equals("java.util.List")
                || rawTypeName.equals("java.util.Set")
                || rawTypeName.equals("java.util.Collection")
                || rawTypeName.equals("java.util.ArrayList")
                || rawTypeName.equals("java.util.LinkedHashSet");
    }

    private boolean isMapType(String rawTypeName) {
        return rawTypeName.equals("java.util.Map")
                || rawTypeName.equals("java.util.HashMap")
                || rawTypeName.equals("java.util.LinkedHashMap");
    }
}
