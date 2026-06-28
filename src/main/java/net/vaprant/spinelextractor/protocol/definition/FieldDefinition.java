package net.vaprant.spinelextractor.protocol.definition;

import java.util.List;
import java.util.Map;

public record FieldDefinition(
    String name,
    String type,
    List<FieldDefinition> nestedFields,
    List<Map<String, Object>> polymorphicVariants,
    List<EntityDataSerializerDefinition> serializerVariants
) {
    public FieldDefinition(
            String name,
            String type,
            List<FieldDefinition> nestedFields,
            List<Map<String, Object>> polymorphicVariants
    ) {
        this(name, type, nestedFields, polymorphicVariants, null);
    }

    public static FieldDefinition of(String name, String type) {
        return new FieldDefinition(name, type, null, null, null);
    }

    public static FieldDefinition nested(
            String name,
            String type,
            List<FieldDefinition> nestedFields
    ) {
        return new FieldDefinition(name, type, nestedFields, null, null);
    }

    public FieldDefinition withNestedFields(List<FieldDefinition> replacementNestedFields) {
        return new FieldDefinition(
                name,
                type,
                replacementNestedFields,
                polymorphicVariants,
                serializerVariants
        );
    }

    public FieldDefinition withSerializerVariants(
            List<EntityDataSerializerDefinition> replacementSerializerVariants
    ) {
        return new FieldDefinition(
                name,
                type,
                nestedFields,
                polymorphicVariants,
                replacementSerializerVariants
        );
    }
}
