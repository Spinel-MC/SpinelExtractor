package net.vaprant.spinelextractor.protocol.analysis;

import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BufferBytecodeAnalyzer {

    public List<FieldDefinition> analyze(Class<?> packetClass) {
        Method writeMethod = findWriteMethod(packetClass);
        if (writeMethod == null) {
            return List.of();
        }

        MethodNode methodNode = readMethodNode(packetClass, writeMethod);
        if (methodNode == null) {
            return List.of();
        }

        Map<String, String> orderedFields = extractFieldWrites(packetClass, methodNode);
        if (orderedFields.isEmpty()) {
            return List.of();
        }

        List<FieldDefinition> fields = new ArrayList<>();
        for (Map.Entry<String, String> entry : orderedFields.entrySet()) {
            fields.add(FieldDefinition.of(entry.getKey(), entry.getValue()));
        }
        return fields;
    }

    public boolean isKnownZeroFieldPacket(Class<?> packetClass) {
        if (packetClass.isRecord() && packetClass.getRecordComponents().length == 0) {
            return true;
        }

        return findWriteMethod(packetClass) == null;
    }

    private Method findWriteMethod(Class<?> packetClass) {
        for (Method method : packetClass.getDeclaredMethods()) {
            boolean isVoid = method.getReturnType() == Void.TYPE;
            boolean hasOneParameter = method.getParameterCount() == 1;
            boolean nameLooksRight = method.getName().equals("write");
            if (!isVoid || !hasOneParameter || !nameLooksRight) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            if (isByteBufType(parameterType)) {
                return method;
            }
        }

        return null;
    }

    private MethodNode readMethodNode(Class<?> packetClass, Method targetMethod) {
        String resourceName = "/" + packetClass.getName().replace('.', '/') + ".class";
        try (InputStream classStream = packetClass.getResourceAsStream(resourceName)) {
            if (classStream == null) {
                return null;
            }

            ClassReader reader = new ClassReader(classStream);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);

            String targetDescriptor = Type.getMethodDescriptor(targetMethod);
            for (MethodNode methodNode : classNode.methods) {
                if (methodNode.name.equals(targetMethod.getName()) && methodNode.desc.equals(targetDescriptor)) {
                    return methodNode;
                }
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private Map<String, String> extractFieldWrites(Class<?> packetClass, MethodNode methodNode) {
        Map<String, String> fieldTypes = new LinkedHashMap<>();
        String packetInternalName = packetClass.getName().replace('.', '/');

        String currentFieldName = null;
        String pendingCodecType = null;
        String pendingRegistryKey = null;

        for (AbstractInsnNode instruction = methodNode.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {

            if (instruction instanceof FieldInsnNode fieldInsn) {
                if (fieldInsn.getOpcode() == Opcodes.GETFIELD && fieldInsn.owner.equals(packetInternalName)) {
                    currentFieldName = fieldInsn.name;
                } else if (fieldInsn.getOpcode() == Opcodes.GETSTATIC && fieldInsn.owner.contains("Registries")) {
                    pendingRegistryKey = normalizeRegistryKey(fieldInsn.name);
                }
                continue;
            }

            if (instruction instanceof LdcInsnNode ldcInsn && ldcInsn.cst instanceof String stringConstant) {
                pendingRegistryKey = normalizeRegistryKey(stringConstant);
                continue;
            }

            if (!(instruction instanceof MethodInsnNode methodInsn)) {
                continue;
            }

            String owner = methodInsn.owner;
            String methodName = methodInsn.name;

            if (isRegistryCodecFactory(owner, methodName)) {
                String registryType = pendingRegistryKey == null ? "unknown" : "RegistryValue<" + pendingRegistryKey + ">";
                pendingCodecType = registryType;
                continue;
            }

            if (isBufferWrite(owner, methodName)) {
                if (currentFieldName != null) {
                    fieldTypes.putIfAbsent(currentFieldName, mapWriteMethod(methodName));
                }
                currentFieldName = null;
                pendingCodecType = null;
                pendingRegistryKey = null;
                continue;
            }

            if (methodName.equals("encode") && currentFieldName != null && pendingCodecType != null) {
                fieldTypes.putIfAbsent(currentFieldName, pendingCodecType);
                currentFieldName = null;
                pendingCodecType = null;
                pendingRegistryKey = null;
            }
        }

        return fieldTypes;
    }

    private boolean isBufferWrite(String owner, String methodName) {
        if (!methodName.startsWith("write")) {
            return false;
        }

        return owner.contains("ByteBuf")
                || owner.contains("PacketByteBuf")
                || owner.contains("FriendlyByteBuf")
                || owner.contains("RegistryByteBuf")
                || owner.contains("RegistryFriendlyByteBuf");
    }

    private boolean isRegistryCodecFactory(String owner, String methodName) {
        return (owner.contains("ByteBufCodecs") || owner.contains("PacketCodecs")) && methodName.contains("registry");
    }

    private boolean isByteBufType(Class<?> type) {
        String className = type.getName();
        return className.contains("ByteBuf");
    }

    private String normalizeRegistryKey(String value) {
        return "minecraft:" + value.toLowerCase();
    }

    private String mapWriteMethod(String methodName) {
        return switch (methodName) {
            case "writeBoolean" -> "Boolean";
            case "writeByte" -> "Byte";
            case "writeShort" -> "Short";
            case "writeInt" -> "Int";
            case "writeLong" -> "Long";
            case "writeFloat" -> "Float";
            case "writeDouble" -> "Double";
            case "writeVarInt" -> "VarInt";
            case "writeVarLong" -> "VarLong";
            case "writeUtf", "writeString" -> "String";
            case "writeUUID", "writeUuid" -> "UUID";
            case "writeIdentifier" -> "Identifier";
            case "writeResourceKey" -> "Identifier";
            case "writeBlockPos" -> "Position";
            case "writeChunkPos" -> "ChunkPos";
            case "writeGlobalPos" -> "GlobalPos";
            case "writeVector3f" -> "Vector3f";
            case "writeQuaternion" -> "Quaternion";
            case "writeVec3" -> "Vec3";
            case "writeLpVec3" -> "LpVec3";
            case "writeNbt" -> "NBT";
            case "writeByteArray" -> "ByteArray";
            case "writeVarIntArray" -> "VarIntArray";
            case "writeLongArray", "writeFixedSizeLongArray" -> "LongArray";
            case "writeBitSet" -> "BitSet";
            case "writeFixedBitSet" -> "FixedBitSet";
            case "writeInstant" -> "Instant";
            case "writePublicKey" -> "PublicKey";
            case "writeBlockHitResult" -> "BlockHitResult";
            case "writeContainerId" -> "ContainerId";
            case "writeCollection", "writeList" -> "PrefixedArray<unknown>";
            case "writeMap" -> "Map<unknown, unknown>";
            case "writeOptional" -> "Optional<unknown>";
            case "writeNullable" -> "Nullable<unknown>";
            case "writeEither" -> "Either<unknown, unknown>";
            case "writeEnum" -> "Enum";
            case "writeEnumSet" -> "EnumSet<unknown>";
            default -> "unknown";
        };
    }
}
