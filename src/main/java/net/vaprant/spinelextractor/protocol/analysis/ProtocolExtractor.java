package net.vaprant.spinelextractor.protocol.analysis;

import net.minecraft.SharedConstants;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;
import net.vaprant.spinelextractor.protocol.definition.PacketDefinition;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProtocolExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolExtractor.class);

    private final ProtocolNavigator protocolNavigator;
    private final PacketClassFinder packetClassFinder;
    private final JsonExtractionRepository extractionRepository;
    private final WireTypeFormatter typeFormatter;
    private final CodecDeconstructor codecDeconstructor;
    private final FriendlyByteBufTypeCatalog typeCatalog;
    private final BufferBytecodeAnalyzer bufferBytecodeAnalyzer;

    public ProtocolExtractor(
            ProtocolNavigator protocolNavigator,
            PacketClassFinder packetClassFinder,
            JsonExtractionRepository extractionRepository
    ) {
        this.protocolNavigator = protocolNavigator;
        this.packetClassFinder = packetClassFinder;
        this.extractionRepository = extractionRepository;
        this.typeFormatter = new WireTypeFormatter();
        this.codecDeconstructor = new CodecDeconstructor(typeFormatter);
        this.typeCatalog = new FriendlyByteBufTypeCatalog();
        this.bufferBytecodeAnalyzer = new BufferBytecodeAnalyzer();
    }

    public void runExtraction() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        int protocolVersion = SharedConstants.getProtocolVersion();
        extractionResult.put("version", protocolVersion);

        Map<String, Map<String, Object>> serverboundProtocols = new LinkedHashMap<>();
        Map<String, Map<String, Object>> clientboundProtocols = new LinkedHashMap<>();

        Map<ConnectionProtocol, String> protocolRegistry = createProtocolRegistry();

        for (Map.Entry<ConnectionProtocol, String> registryEntry : protocolRegistry.entrySet()) {
            ConnectionProtocol protocol = registryEntry.getKey();
            String protocolName = registryEntry.getValue();
            processProtocol(protocol, protocolName, serverboundProtocols, clientboundProtocols);
        }

        Map<String, Object> packetData = new LinkedHashMap<>();
        packetData.put("serverbound", serverboundProtocols);
        packetData.put("clientbound", clientboundProtocols);
        extractionResult.put("packets", packetData);
        extractionResult.put("types", typeCatalog.buildTypesSection());

        extractionRepository.save(extractionResult);
    }

    private void processProtocol(
            ConnectionProtocol protocol,
            String protocolName,
            Map<String, Map<String, Object>> serverboundCollection,
            Map<String, Map<String, Object>> clientboundCollection
    ) {
        Map<String, Object> serverboundPackets = extractPacketsFromFlow(protocol, PacketFlow.SERVERBOUND);
        if (!serverboundPackets.isEmpty()) {
            serverboundCollection.put(protocolName, serverboundPackets);
        }

        Map<String, Object> clientboundPackets = extractPacketsFromFlow(protocol, PacketFlow.CLIENTBOUND);
        if (!clientboundPackets.isEmpty()) {
            clientboundCollection.put(protocolName, clientboundPackets);
        }
    }

    private Map<String, Object> extractPacketsFromFlow(ConnectionProtocol protocol, PacketFlow flow) {
        Map<String, Object> flowPacketData = new LinkedHashMap<>();
        Object protocolSource = getProtocolSource(protocol, flow);

        if (protocolSource == null) {
            return flowPacketData;
        }

        try {
            List<?> packetEntries = protocolNavigator.findPacketEntries(protocolSource);
            Map<Integer, Class<?>> idToClassMapping = protocolNavigator.findIdToClassMapping(protocolSource);

            for (int packetIndex = 0; packetIndex < packetEntries.size(); packetIndex++) {
                Object entryObject = packetEntries.get(packetIndex);
                processProtocolEntry(entryObject, packetIndex, idToClassMapping, flowPacketData);
            }
        } catch (Exception extractionException) {
            String failureMessage = extractionException.getMessage();
            LOG.error("Failed to extract protocol {} {}: {}", protocol, flow, failureMessage);
        }

        return flowPacketData;
    }

    private void processProtocolEntry(Object entry, int index, Map<Integer, Class<?>> classMap, Map<String, Object> flowData) {
        String packetIdentifier = "unknown_at_index_" + index;
        try {
            PacketType<?> packetType = (PacketType<?>) ReflectionAccess.getHiddenFieldValue(entry, "type");
            if (packetType == null) {
                LOG.error("[Packet: {}] Reason: Could not grab 'type' field from entry", packetIdentifier);
                return;
            }
            
            net.minecraft.resources.Identifier typeIdentifier = packetType.id();
            packetIdentifier = typeIdentifier.getPath();

            Object packetSerializer = ReflectionAccess.getHiddenFieldValue(entry, "serializer");
            if (packetSerializer == null) {
                LOG.error("[Packet: {}] Reason: Could not grab 'serializer' field", packetIdentifier);
                return;
            }

            PacketDiscoveryContext discoveryContext = new PacketDiscoveryContext(packetSerializer, packetType, classMap, index);
            Class<?> packetClass = packetClassFinder.findPacketClass(discoveryContext);
            if (packetClass == null) {
                LOG.error("[Packet: {}] Reason: Could not identify packet class", packetIdentifier);
                return;
            }

            List<FieldDefinition> extractedFields = codecDeconstructor.deconstruct(packetSerializer);
            if (extractedFields.isEmpty()) {
                extractedFields = bufferBytecodeAnalyzer.analyze(packetClass);
            }

            if (extractedFields.isEmpty()) {
                String className = packetClass.getName();
                if (!bufferBytecodeAnalyzer.isKnownZeroFieldPacket(packetClass)) {
                    LOG.warn("[Packet: {}] Reason: No FriendlyByteBuf-backed fields extracted (Class: {})", packetIdentifier, className);
                }
            }

            String hexadecimalId = String.format("0x%02x", index);
            PacketDefinition packetDefinition = new PacketDefinition(hexadecimalId, extractedFields);
            flowData.put(packetIdentifier, packetDefinition);
        } catch (Exception processingException) {
            String errorMessage = processingException.getMessage();
            LOG.error("[Packet: {}] Reason: {}", packetIdentifier, errorMessage);
        }
    }

    private Object getProtocolSource(ConnectionProtocol protocol, PacketFlow flow) {
        return switch (protocol) {
            case HANDSHAKING -> flow == PacketFlow.SERVERBOUND ? HandshakeProtocols.SERVERBOUND : null;
            case PLAY -> flow == PacketFlow.SERVERBOUND ? GameProtocols.SERVERBOUND_TEMPLATE : GameProtocols.CLIENTBOUND_TEMPLATE;
            case STATUS -> flow == PacketFlow.SERVERBOUND ? StatusProtocols.SERVERBOUND : StatusProtocols.CLIENTBOUND;
            case LOGIN -> flow == PacketFlow.SERVERBOUND ? LoginProtocols.SERVERBOUND : LoginProtocols.CLIENTBOUND;
            case CONFIGURATION -> flow == PacketFlow.SERVERBOUND ? ConfigurationProtocols.SERVERBOUND : ConfigurationProtocols.CLIENTBOUND;
        };
    }

    private Map<ConnectionProtocol, String> createProtocolRegistry() {
        Map<ConnectionProtocol, String> registryMap = new LinkedHashMap<>();
        registryMap.put(ConnectionProtocol.HANDSHAKING, "handshake");
        registryMap.put(ConnectionProtocol.STATUS, "status");
        registryMap.put(ConnectionProtocol.LOGIN, "login");
        registryMap.put(ConnectionProtocol.CONFIGURATION, "configuration");
        registryMap.put(ConnectionProtocol.PLAY, "play");
        return registryMap;
    }
}
