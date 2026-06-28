package net.vaprant.spinelextractor.protocol.verification;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.vaprant.spinelextractor.protocol.analysis.ProtocolNavigator;
import net.vaprant.spinelextractor.protocol.analysis.ReflectionAccess;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public final class VanillaPacketCrossDecodeTest {

    private final ProtocolNavigator protocolNavigator = new ProtocolNavigator();

    @Test
    void vanillaDecodesEverySpinelPacketFixture() throws Exception {
        Path fixturePath = Path.of(System.getProperty("spinel.packetFixtures"));
        PacketFixtureCatalog catalog = new Gson().fromJson(
                Files.readString(fixturePath),
                PacketFixtureCatalog.class
        );
        SharedConstants.tryDetectVersion();
        assertEquals(SharedConstants.getProtocolVersion(), catalog.version());

        Bootstrap.bootStrap();
        RegistryAccess registryAccess = RegistryLayer.createRegistryAccess().compositeAccess();
        VerificationResult result = verifyPackets(catalog, registryAccess);
        List<String> incompatiblePackets = result.incompatiblePackets();
        if (!incompatiblePackets.isEmpty()) {
            fail(verificationFailure(incompatiblePackets));
        }
    }

    private VerificationResult verifyPackets(
            PacketFixtureCatalog catalog,
            RegistryAccess registryAccess
    ) {
        List<String> incompatiblePackets = new ArrayList<>();
        catalog.packets().forEach((direction, states) ->
                states.forEach((state, packets) ->
                        packets.forEach((resourceId, fixture) -> {
                            String packetPath = direction + "/" + state + "/" + resourceId;
                            DecodeOutcome outcome = vanillaDecodes(
                                    direction,
                                    state,
                                    resourceId,
                                    fixture,
                                    registryAccess
                            );
                            if (outcome == DecodeOutcome.INCOMPATIBLE) {
                                incompatiblePackets.add(packetPath);
                            }
                        })
                )
        );
        return new VerificationResult(incompatiblePackets);
    }

    private DecodeOutcome vanillaDecodes(
            String direction,
            String state,
            String resourceId,
            PacketFixture fixture,
            RegistryAccess registryAccess
    ) {
        Object packetEntry = findPacketEntry(direction, state, resourceId);
        if (packetEntry == null) {
            return DecodeOutcome.INCOMPATIBLE;
        }

        Object serializer = ReflectionAccess.getHiddenFieldValue(packetEntry, "serializer");
        boolean hasMissingRegistryCandidate = false;
        for (List<Integer> payload : fixture.payloads()) {
            DecodeOutcome outcome = vanillaDecodes(serializer, payload, registryAccess);
            if (outcome == DecodeOutcome.COMPATIBLE) {
                return DecodeOutcome.COMPATIBLE;
            }
            if (outcome == DecodeOutcome.MISSING_REGISTRY) {
                hasMissingRegistryCandidate = true;
            }
        }
        return hasMissingRegistryCandidate ? DecodeOutcome.COMPATIBLE : DecodeOutcome.INCOMPATIBLE;
    }

    private Object findPacketEntry(String direction, String state, String resourceId) {
        Object protocolSource = protocolSource(state, direction);
        return protocolNavigator.findPacketEntries(protocolSource)
                .stream()
                .filter(packetEntry -> packetResourceId(packetEntry).equals(resourceId))
                .findFirst()
                .orElse(null);
    }

    private String packetResourceId(Object packetEntry) {
        PacketType<?> packetType = (PacketType<?>) ReflectionAccess.getHiddenFieldValue(
                packetEntry,
                "type"
        );
        return packetType.id().getPath();
    }

    private DecodeOutcome vanillaDecodes(
            Object serializer,
            List<Integer> payload,
            RegistryAccess registryAccess
    ) {
        try {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.wrappedBuffer(payloadBytes(payload)),
                    registryAccess
            );
            decode(serializer, buffer);
            return buffer.readableBytes() == 0
                    ? DecodeOutcome.COMPATIBLE
                    : DecodeOutcome.INCOMPATIBLE;
        } catch (Throwable failure) {
            if (isMissingRegistryFailure(failure)) {
                return DecodeOutcome.MISSING_REGISTRY;
            }
            return DecodeOutcome.INCOMPATIBLE;
        }
    }

    private boolean isMissingRegistryFailure(Throwable failure) {
        Throwable currentFailure = failure;
        while (currentFailure != null) {
            String message = currentFailure.getMessage();
            if (message != null && message.startsWith("Missing registry:")) {
                return true;
            }
            currentFailure = currentFailure.getCause();
        }
        return false;
    }

    private String verificationFailure(
            List<String> incompatiblePackets
    ) {
        List<String> sections = new ArrayList<>();
        if (!incompatiblePackets.isEmpty()) {
            sections.add("Vanilla rejected Spinel packet fixtures:\n"
                    + String.join("\n", incompatiblePackets));
        }
        return String.join("\n\n", sections);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void decode(Object serializer, RegistryFriendlyByteBuf buffer) {
        ((StreamCodec) serializer).decode(buffer);
    }

    private Object protocolSource(String state, String direction) {
        PacketFlow flow = direction.equals("serverbound")
                ? PacketFlow.SERVERBOUND
                : PacketFlow.CLIENTBOUND;
        return switch (state) {
            case "handshake" -> flow == PacketFlow.SERVERBOUND ? HandshakeProtocols.SERVERBOUND : null;
            case "status" -> flow == PacketFlow.SERVERBOUND ? StatusProtocols.SERVERBOUND : StatusProtocols.CLIENTBOUND;
            case "login" -> flow == PacketFlow.SERVERBOUND ? LoginProtocols.SERVERBOUND : LoginProtocols.CLIENTBOUND;
            case "configuration" -> flow == PacketFlow.SERVERBOUND
                    ? ConfigurationProtocols.SERVERBOUND
                    : ConfigurationProtocols.CLIENTBOUND;
            case "play" -> flow == PacketFlow.SERVERBOUND
                    ? GameProtocols.SERVERBOUND_TEMPLATE
                    : GameProtocols.CLIENTBOUND_TEMPLATE;
            default -> throw new IllegalArgumentException("unknown connection state " + state);
        };
    }

    private byte[] payloadBytes(List<Integer> payload) {
        byte[] payloadBytes = new byte[payload.size()];
        for (int index = 0; index < payload.size(); index++) {
            payloadBytes[index] = payload.get(index).byteValue();
        }
        return payloadBytes;
    }

    private enum DecodeOutcome {
        COMPATIBLE,
        INCOMPATIBLE,
        MISSING_REGISTRY
    }

    private record VerificationResult(
            List<String> incompatiblePackets
    ) {}
}
