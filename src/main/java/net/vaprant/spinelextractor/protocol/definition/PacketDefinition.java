package net.vaprant.spinelextractor.protocol.definition;

import java.util.List;

public record PacketDefinition(String id, List<FieldDefinition> fields) {}
