package net.vaprant.spinelextractor.protocol.analysis;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class TypeFormatter {

    private final Consumer<Class<?>> complexTypeDiscoveryCallback;

    public TypeFormatter(Consumer<Class<?>> complexTypeDiscoveryCallback) {
        this.complexTypeDiscoveryCallback = complexTypeDiscoveryCallback;
    }

    public String formatType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return formatParameterizedType(parameterizedType);
        }

        if (type instanceof Class<?> classType) {
            return formatClassType(classType);
        }

        if (type instanceof WildcardType wildcardType) {
            return formatWildcardType(wildcardType);
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            return typeVariable.getName();
        }

        return type.getTypeName();
    }

    private String formatParameterizedType(ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        String rawTypeName = formatType(rawType);

        boolean isMinecraftInternal = rawTypeName.equals("net.minecraft.resources.Identifier") 
            || rawTypeName.equals("VarInt");

        if (isMinecraftInternal) {
            return rawTypeName;
        }

        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length == 0) {
            return rawTypeName;
        }

        List<String> formattedArguments = new ArrayList<>();
        for (Type argument : typeArguments) {
            String argumentName = formatType(argument);
            formattedArguments.add(argumentName);
        }

        String joinedArguments = String.join(", ", formattedArguments);
        return rawTypeName + "<" + joinedArguments + ">";
    }

    private String formatClassType(Class<?> classType) {
        if (classType.isArray()) {
            Class<?> componentType = classType.getComponentType();
            String componentName = formatType(componentType);
            return componentName + "[]";
        }

        String qualifiedName = classType.getName();
        String standardName = mapWellKnownMinecraftTypes(qualifiedName);
        if (standardName != null) {
            return standardName;
        }

        if (isComplexDomainType(classType)) {
            complexTypeDiscoveryCallback.accept(classType);
        }

        return qualifiedName;
    }

    private String mapWellKnownMinecraftTypes(String qualifiedName) {
        return switch (qualifiedName) {
            case "it.unimi.dsi.fastutil.ints.IntList" -> "java.util.List<int>";
            case "it.unimi.dsi.fastutil.ints.Int2ObjectMap" -> "java.util.Map<int, ?>";
            case "net.minecraft.world.level.block.entity.BlockEntityType" -> "VarInt";
            default -> null;
        };
    }

    private String formatWildcardType(WildcardType wildcardType) {
        Type[] upperBounds = wildcardType.getUpperBounds();
        boolean hasUpperBounds = upperBounds.length > 0;
        boolean isNotObjectBound = hasUpperBounds && upperBounds[0] != Object.class;

        if (isNotObjectBound) {
            return formatType(upperBounds[0]);
        }

        return "?";
    }

    private boolean isComplexDomainType(Class<?> classType) {
        if (classType.isPrimitive()) {
            return false;
        }

        String className = classType.getName();
        boolean isJavaStandard = className.startsWith("java.lang.") 
            || className.startsWith("java.util.") 
            || className.startsWith("java.nio.");

        if (isJavaStandard) {
            return false;
        }

        boolean isExternalLibrary = className.startsWith("com.google.gson.") 
            || className.startsWith("it.unimi.dsi.fastutil.");

        if (isExternalLibrary) {
            return false;
        }

        if (isInternalMinecraftPrimitive(className)) {
            return false;
        }

        boolean isMinecraftCodebase = className.startsWith("net.minecraft.") 
            || className.startsWith("com.mojang.");
        boolean isAuthenticationService = className.startsWith("com.google.auth.");

        return isMinecraftCodebase || isAuthenticationService;
    }

    private boolean isInternalMinecraftPrimitive(String className) {
        return className.equals("net.minecraft.resources.Identifier")
            || className.equals("net.minecraft.core.BlockPos")
            || className.equals("net.minecraft.network.chat.Component")
            || className.equals("net.minecraft.nbt.CompoundTag")
            || className.equals("net.minecraft.world.item.ItemStack")
            || className.contains(".server.")
            || className.contains(".mixin.")
            || className.contains(".asm.")
            || className.endsWith("Packet");
    }
}
