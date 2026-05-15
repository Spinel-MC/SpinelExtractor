package net.vaprant.spinelextractor.extractors;

import net.vaprant.spinelextractor.protocol.analysis.PacketClassFinder;
import net.vaprant.spinelextractor.protocol.analysis.ProtocolExtractor;
import net.vaprant.spinelextractor.protocol.analysis.ProtocolNavigator;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

public final class PacketExtractor {

    private PacketExtractor() {}

    public static void extract() {
        ProtocolNavigator navigator = new ProtocolNavigator();
        PacketClassFinder finder = new PacketClassFinder();
        JsonExtractionRepository repository = new JsonExtractionRepository();

        ProtocolExtractor orchestrator = new ProtocolExtractor(
                navigator,
                finder,
                repository
        );

        orchestrator.runExtraction();
    }
}
