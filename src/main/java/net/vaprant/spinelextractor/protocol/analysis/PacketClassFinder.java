package net.vaprant.spinelextractor.protocol.analysis;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PacketClassFinder {

    private static final Logger LOG = LoggerFactory.getLogger(PacketClassFinder.class);

    public Class<?> findPacketClass(PacketDiscoveryContext context) {
        Map<Integer, Class<?>> classMap = context.classMap();
        int packetIndex = context.index();

        if (classMap != null && classMap.containsKey(packetIndex)) {
            return classMap.get(packetIndex);
        }

        Class<?> genericMatch = findByGenericType(context.serializer());
        if (genericMatch != null) {
            return genericMatch;
        }

        Class<?> namingMatch = findByNamingConvention(context.type());
        if (namingMatch != null) {
            return namingMatch;
        }

        return findBySerializer(context.serializer());
    }

    private Class<?> findByGenericType(Object serializer) {
        if (serializer == null) {
            return null;
        }

        try {
            Class<?> serializerClass = serializer.getClass();
            for (Type genericInterface : serializerClass.getGenericInterfaces()) {
                if (!(genericInterface instanceof ParameterizedType parameterizedType)) {
                    continue;
                }

                Type rawType = parameterizedType.getRawType();
                String rawName = rawType.getTypeName();
                boolean isCodecType = rawName.contains("StreamCodec") || rawName.contains("PacketCodec");
                if (!isCodecType) {
                    continue;
                }

                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                boolean hasTargetArgument = typeArguments.length >= 2 && typeArguments[1] instanceof Class<?> targetClass;

                if (hasTargetArgument) {
                    Class<?> targetClass = (Class<?>) typeArguments[1];
                    if (isPacketOrProtocolClass(targetClass)) {
                        return targetClass;
                    }
                }

                for (Type argument : typeArguments) {
                    if (argument instanceof Class<?> argumentClass && isPacketOrProtocolClass(argumentClass)) {
                        return argumentClass;
                    }
                }
            }
        } catch (Exception exception) {
            LOG.warn("Failed to find packet class by generic type", exception);
        }
        return null;
    }

    private Class<?> findBySerializer(Object serializer) {
        List<Object> visitedObjects = new ArrayList<>();
        return findBySerializerRecursive(serializer, visitedObjects);
    }

    private Class<?> findBySerializerRecursive(Object serializer, List<Object> visited) {
        if (serializer == null || visited.contains(serializer)) {
            return null;
        }
        visited.add(serializer);

        Class<?> serializerClass = serializer.getClass();
        for (Field field : serializerClass.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(serializer);
                if (fieldValue == null) {
                    continue;
                }

                Class<?> discoveredClass = inspectSerializerField(field, fieldValue);
                if (discoveredClass != null) {
                    return discoveredClass;
                }

                if (isPotentialCodecContainer(field, fieldValue)) {
                    Class<?> recursiveResult = findBySerializerRecursive(fieldValue, visited);
                    if (recursiveResult != null) {
                        return recursiveResult;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Class<?> inspectSerializerField(Field field, Object fieldValue) {
        Class<?> fieldValueClass = fieldValue.getClass();
        String valueClassName = fieldValueClass.getName();
        if (valueClassName.contains("$$Lambda")) {
            int lambdaIdentifierIndex = valueClassName.indexOf("$$Lambda");
            String hostClassName = valueClassName.substring(0, lambdaIdentifierIndex);
            try {
                Class<?> hostClass = Class.forName(hostClassName);
                if (isPacketOrProtocolClass(hostClass)) {
                    return hostClass;
                }
            } catch (Exception ignored) {}
        }

        Class<?> fieldType = field.getType();
        String fieldSimpleName = fieldType.getSimpleName();
        if (fieldSimpleName.endsWith("Packet")) {
            return fieldType;
        }

        String fieldName = field.getName();
        boolean isMetadataField = fieldName.equals("type") 
            || fieldName.equals("clazz") 
            || fieldName.equals("packetClass") 
            || fieldName.equals("target");

        if (isMetadataField && fieldValue instanceof Class<?> targetClass) {
            if (isPacketOrProtocolClass(targetClass)) {
                return targetClass;
            }
        }

        if (fieldValue instanceof Class<?> directClass && isPacketOrProtocolClass(directClass)) {
            return directClass;
        }

        return null;
    }

    private boolean isPacketOrProtocolClass(Class<?> targetClass) {
        if (targetClass.isInterface() || targetClass.isEnum()) {
            return false;
        }
        String simpleName = targetClass.getSimpleName();
        boolean isInfrastructure = simpleName.endsWith("Protocols") 
            || simpleName.endsWith("Protocol") 
            || simpleName.equals("Packet");

        if (isInfrastructure) {
            return false;
        }

        String fullName = targetClass.getName();
        return simpleName.endsWith("Packet") || fullName.contains(".protocol.");
    }

    private boolean isPotentialCodecContainer(Field field, Object fieldValue) {
        Class<?> valueClass = fieldValue.getClass();
        String className = valueClass.getName();
        String fieldName = field.getName();

        return className.contains("StreamCodec")
                || className.contains("PacketCodec")
                || fieldName.startsWith("field_")
                || fieldName.startsWith("val$")
                || fieldName.equals("codec");
    }

    private Class<?> findByNamingConvention(PacketType<?> packetType) {
        net.minecraft.resources.Identifier packetIdentifier = packetType.id();
        String path = packetIdentifier.getPath();
        String pascalName = toPascalCase(path);
        PacketFlow flow = packetType.flow();

        List<String> variations = generateNamingVariations(pascalName);
        String[] possiblePrefixes = flow == PacketFlow.SERVERBOUND ? new String[]{"Serverbound", "Handshake", ""} : new String[]{"Clientbound", ""};
        String[] protocolPackages = {
            "net.minecraft.network.protocol.game",
            "net.minecraft.network.protocol.handshake",
            "net.minecraft.network.protocol.login",
            "net.minecraft.network.protocol.status",
            "net.minecraft.network.protocol.configuration",
            "net.minecraft.network.protocol.common"
        };

        for (String packageName : protocolPackages) {
            for (String prefix : possiblePrefixes) {
                for (String variation : variations) {
                    Class<?> classAttempt = tryLoadClass(packageName + "." + prefix + variation + "Packet");
                    if (classAttempt != null) {
                        return classAttempt;
                    }

                    Class<?> fallbackAttempt = tryLoadClass(packageName + "." + variation + "Packet");
                    if (fallbackAttempt != null) {
                        return fallbackAttempt;
                    }
                }
            }
        }
        return null;
    }

    private List<String> generateNamingVariations(String name) {
        List<String> variations = new ArrayList<>();
        variations.add(name);

        if (name.endsWith("Intention")) {
            String stripped = name.substring(0, name.length() - 9);
            variations.add(stripped + "Intent");
        }
        if (name.endsWith("Response")) {
            String stripped = name.substring(0, name.length() - 8);
            variations.add(stripped);
        }
        if (name.endsWith("Request")) {
            String stripped = name.substring(0, name.length() - 7);
            variations.add(stripped);
        }

        List<String> pingPongList = new ArrayList<>(variations);
        for (String variant : variations) {
            if (variant.contains("Pong")) {
                pingPongList.add(variant.replace("Pong", "Ping"));
            }
            if (variant.contains("Ping")) {
                pingPongList.add(variant.replace("Ping", "Pong"));
            }
        }
        return pingPongList;
    }

    private Class<?> tryLoadClass(String fullName) {
        try {
            return Class.forName(fullName);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private String toPascalCase(String snakeCaseInput) {
        StringBuilder builder = new StringBuilder();
        boolean shouldCapitalizeNext = true;
        for (char character : snakeCaseInput.toCharArray()) {
            if (character == '_') {
                shouldCapitalizeNext = true;
                continue;
            }
            builder.append(shouldCapitalizeNext ? Character.toUpperCase(character) : character);
            shouldCapitalizeNext = false;
        }
        return builder.toString();
    }
}
