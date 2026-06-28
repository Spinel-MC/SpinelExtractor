package net.vaprant.spinelextractor.protocol.persistence;

import com.google.gson.Gson;
import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;
import net.vaprant.spinelextractor.protocol.definition.PacketCodecContract;
import net.vaprant.spinelextractor.protocol.definition.PacketDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class PacketDefinitionJsonTest {

    @Test
    void codecContractRemainsInsideExistingNestedPacketEntry() {
        List<FieldDefinition> fields = List.of(FieldDefinition.of("entityId", "VarInt"));
        PacketDefinition packet = new PacketDefinition(
                "0x61",
                fields,
                PacketCodecContract.from(EntityPacket.class, fields, false)
        );
        Map<String, Object> root = nestedPacketCatalog(packet);

        Map<?, ?> decodedRoot = new Gson().fromJson(new Gson().toJson(root), Map.class);
        Map<?, ?> packets = map(decodedRoot.get("packets"));
        Map<?, ?> clientbound = map(packets.get("clientbound"));
        Map<?, ?> play = map(clientbound.get("play"));
        Map<?, ?> setEntityData = map(play.get("set_entity_data"));

        assertEquals("0x61", setEntityData.get("id"));
        assertNotNull(setEntityData.get("fields"));
        assertNotNull(setEntityData.get("codec"));
    }

    private static Map<String, Object> nestedPacketCatalog(PacketDefinition packet) {
        Map<String, Object> play = new LinkedHashMap<>();
        play.put("set_entity_data", packet);
        Map<String, Object> clientbound = new LinkedHashMap<>();
        clientbound.put("play", play);
        Map<String, Object> packets = new LinkedHashMap<>();
        packets.put("clientbound", clientbound);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("packets", packets);
        return root;
    }

    private static Map<?, ?> map(Object value) {
        return (Map<?, ?>) value;
    }

    private record EntityPacket(int entityId) {}
}
