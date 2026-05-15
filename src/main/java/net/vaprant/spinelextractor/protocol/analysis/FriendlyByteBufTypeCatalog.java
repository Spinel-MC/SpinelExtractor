package net.vaprant.spinelextractor.protocol.analysis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FriendlyByteBufTypeCatalog {

    public Map<String, Object> buildTypesSection() {
        Map<String, Object> types = new LinkedHashMap<>();

        addType(types, "Boolean", "readBoolean", "writeBoolean");
        addType(types, "Byte", "readByte", "writeByte");
        addType(types, "UnsignedByte", "readUnsignedByte", "writeByte");
        addType(types, "Short", "readShort", "writeShort");
        addType(types, "UnsignedShort", "readUnsignedShort", null);
        addType(types, "Int", "readInt", "writeInt");
        addType(types, "VarInt", "readVarInt", "writeVarInt");
        addType(types, "Long", "readLong", "writeLong");
        addType(types, "VarLong", "readVarLong", "writeVarLong");
        addType(types, "Float", "readFloat", "writeFloat");
        addType(types, "Double", "readDouble", "writeDouble");
        addType(types, "String", "readUtf", "writeUtf");
        addType(types, "UUID", "readUUID", "writeUUID");
        addType(types, "Identifier", "readIdentifier", "writeIdentifier");
        addType(types, "Position", "readBlockPos", "writeBlockPos");
        addType(types, "ChunkPos", "readChunkPos", "writeChunkPos");
        addType(types, "GlobalPos", "readGlobalPos", "writeGlobalPos");
        addType(types, "Vector3f", "readVector3f", "writeVector3f");
        addType(types, "Quaternion", "readQuaternion", "writeQuaternion");
        addType(types, "Vec3", "readVec3", "writeVec3");
        addType(types, "LpVec3", "readLpVec3", "writeLpVec3");
        addType(types, "NBT", "readNbt", "writeNbt");
        addType(types, "ByteArray", "readByteArray", "writeByteArray");
        addType(types, "VarIntArray", "readVarIntArray", "writeVarIntArray");
        addType(types, "LongArray", "readLongArray", "writeLongArray");
        addType(types, "BitSet", "readBitSet", "writeBitSet");
        addType(types, "FixedBitSet", "readFixedBitSet", "writeFixedBitSet");
        addType(types, "Instant", "readInstant", "writeInstant");
        addType(types, "PublicKey", "readPublicKey", "writePublicKey");
        addType(types, "BlockHitResult", "readBlockHitResult", "writeBlockHitResult");
        addType(types, "ContainerId", "readContainerId", "writeContainerId");
        addType(types, "ResourceKey", "readResourceKey", "writeResourceKey");
        addType(types, "RegistryKey", "readRegistryKey", null);
        addType(types, "PrefixedArray<T>", List.of("readCollection", "readList"), "writeCollection");
        addType(types, "Map<K, V>", "readMap", "writeMap");
        addType(types, "Optional<T>", "readOptional", "writeOptional");
        addType(types, "Nullable<T>", "readNullable", "writeNullable");
        addType(types, "Either<L, R>", "readEither", "writeEither");
        addType(types, "Enum<E>", "readEnum", "writeEnum");
        addType(types, "EnumSet<E>", "readEnumSet", "writeEnumSet");
        addType(types, "RegistryValue<T>", List.of(), null);

        return types;
    }

    private void addType(Map<String, Object> types, String typeName, String readMethod, String writeMethod) {
        List<String> readMethods = readMethod == null ? List.of() : List.of(readMethod);
        addType(types, typeName, readMethods, writeMethod);
    }

    private void addType(Map<String, Object> types, String typeName, List<String> readMethods, String writeMethod) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("read", readMethods);
        definition.put("write", writeMethod == null ? List.of() : List.of(writeMethod));
        types.put(typeName, definition);
    }
}
