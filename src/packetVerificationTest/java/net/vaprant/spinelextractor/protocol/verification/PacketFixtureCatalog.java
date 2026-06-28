package net.vaprant.spinelextractor.protocol.verification;

import java.util.Map;

public record PacketFixtureCatalog(
        int version,
        Map<String, Map<String, Map<String, PacketFixture>>> packets
) {}
