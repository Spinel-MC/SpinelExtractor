package net.vaprant.spinelextractor.protocol.definition;

import java.util.List;
import java.util.Map;

public record FieldDefinition(
    String name,
    String type,
    List<FieldDefinition> nestedFields,
    List<Map<String, Object>> polymorphicVariants
) {
    public static FieldDefinition of(String name, String type) {
        return new FieldDefinition(name, type, null, null);
    }
}
