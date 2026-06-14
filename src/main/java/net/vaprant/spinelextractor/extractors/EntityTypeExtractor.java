package net.vaprant.spinelextractor.extractors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.vaprant.spinelextractor.protocol.persistence.JsonExtractionRepository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class EntityTypeExtractor {
    private static final String ENTITY_TYPE_OUTPUT_FILE_PATH = "spinel_extractor/entity_types.json";

    public static void extract() {
        EntityTypeExtractor extractor = new EntityTypeExtractor();
        new JsonExtractionRepository(ENTITY_TYPE_OUTPUT_FILE_PATH).save(extractor.extractEntityTypesFile());
    }

    private Map<String, Object> extractEntityTypesFile() {
        Map<String, Object> extractionResult = new LinkedHashMap<>();
        extractionResult.put("entityTypes", extractEntityTypes());
        return extractionResult;
    }

    private List<EntityTypeDefinition> extractEntityTypes() {
        return BuiltInRegistries.ENTITY_TYPE
                .stream()
                .sorted(Comparator.comparingInt(BuiltInRegistries.ENTITY_TYPE::getId))
                .map(this::extractEntityType)
                .toList();
    }

    private EntityTypeDefinition extractEntityType(EntityType<?> entityType) {
        EntityDimensions dimensions = entityType.getDimensions();
        return new EntityTypeDefinition(
                BuiltInRegistries.ENTITY_TYPE.getId(entityType),
                BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath(),
                entityType.getDescriptionId(),
                packetType(entityType),
                entityType.getWidth(),
                entityType.getHeight(),
                dimensions.eyeHeight(),
                entityType.clientTrackingRange(),
                entityType.fireImmune(),
                extractDefaultAttributes(entityType),
                extractAttachments(dimensions)
        );
    }

    private String packetType(EntityType<?> entityType) {
        Class<?> entityClass = entityClass(entityType);
        if (Player.class.isAssignableFrom(entityClass)) {
            return "player";
        }
        if (LivingEntity.class.isAssignableFrom(entityClass)) {
            return "living";
        }
        return "entity";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> extractDefaultAttributes(EntityType<?> entityType) {
        if (!DefaultAttributes.hasSupplier(entityType)) {
            return Map.of();
        }
        AttributeSupplier defaultAttributes = DefaultAttributes.getSupplier(
                (EntityType<? extends LivingEntity>) entityType
        );
        Map<String, Double> attributes = new LinkedHashMap<>();
        BuiltInRegistries.ATTRIBUTE
                .stream()
                .sorted(Comparator.comparingInt(BuiltInRegistries.ATTRIBUTE::getId))
                .map(BuiltInRegistries.ATTRIBUTE::wrapAsHolder)
                .filter(defaultAttributes::hasAttribute)
                .forEach(attribute -> attributes.put(
                        BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()).getPath(),
                        defaultAttributes.getBaseValue(attribute)
                ));
        return attributes;
    }

    private Class<?> entityClass(EntityType<?> entityType) {
        return Arrays.stream(EntityType.class.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> fieldValueIsEntityType(field, entityType))
                .map(Field::getGenericType)
                .map(this::entityClassFromGenericType)
                .filter(entityClass -> entityClass != null)
                .findFirst()
                .orElse(entityType.getBaseClass());
    }

    private boolean fieldValueIsEntityType(Field field, EntityType<?> entityType) {
        try {
            return field.get(null) == entityType;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    private Class<?> entityClassFromGenericType(Type genericType) {
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        Type entityTypeArgument = parameterizedType.getActualTypeArguments()[0];
        if (entityTypeArgument instanceof Class<?> entityClass) {
            return entityClass;
        }
        return null;
    }

    private Map<String, List<Double>> extractAttachments(EntityDimensions dimensions) {
        Map<String, List<Double>> attachments = new LinkedHashMap<>();
        Arrays.stream(EntityAttachment.values())
                .sorted(Comparator.comparing(Enum::name))
                .forEach(attachment -> extractAttachment(dimensions, attachment, attachments));
        return attachments;
    }

    private void extractAttachment(
            EntityDimensions dimensions,
            EntityAttachment attachment,
            Map<String, List<Double>> attachments
    ) {
        Vec3 offset = dimensions.attachments().getNullable(attachment, 0, 1.0F);
        if (offset == null) {
            return;
        }
        attachments.put(
                attachment.name().toLowerCase(),
                List.of(offset.x(), offset.y(), offset.z())
        );
    }

    private record EntityTypeDefinition(
            int id,
            String name,
            String translationKey,
            String packetType,
            float width,
            float height,
            float eyeHeight,
            int clientTrackingRange,
            boolean fireImmune,
            Map<String, Double> defaultAttributes,
            Map<String, List<Double>> attachments
    ) {}
}
