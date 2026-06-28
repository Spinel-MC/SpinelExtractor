package net.vaprant.spinelextractor.protocol.verification;

import java.util.List;

public record PacketFixture(
        String id,
        List<Object> fields,
        List<List<Integer>> payloads
) {}
