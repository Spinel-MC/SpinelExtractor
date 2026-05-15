package net.vaprant.spinelextractor.protocol.analysis;

import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TypeProcessor {

    private final Set<Class<?>> pendingTypes = new HashSet<>();

    public void queueType(Class<?> domainClass) {
        pendingTypes.add(domainClass);
    }

    public Map<String, Object> processSingleType(Class<?> domainClass, TypeFormatter formatter) {
        if (domainClass.isEnum()) {
            return createEnumDefinition(domainClass);
        }

        int classModifiers = domainClass.getModifiers();
        boolean isAbstractClass = Modifier.isAbstract(classModifiers) && !domainClass.isInterface();

        if (isAbstractClass) {
            Map<String, Object> polymorphicDefinition = tryCreatePolymorphicDefinition(domainClass, formatter);
            if (polymorphicDefinition != null) {
                return polymorphicDefinition;
            }
        }

        Map<String, Object> standardDefinition = new LinkedHashMap<>();
        List<FieldDefinition> extractedFields = extractFields(domainClass, formatter);
        standardDefinition.put("fields", extractedFields);

        return standardDefinition;
    }

    private Map<String, Object> createEnumDefinition(Class<?> enumClass) {
        Map<String, Object> enumDefinition = new LinkedHashMap<>();
        enumDefinition.put("type", "enum");

        List<String> enumVariants = new ArrayList<>();
        for (Object enumConstant : enumClass.getEnumConstants()) {
            String variantName = enumConstant.toString();
            enumVariants.add(variantName);
        }

        enumDefinition.put("variants", enumVariants);
        return enumDefinition;
    }

    private Map<String, Object> tryCreatePolymorphicDefinition(Class<?> baseClass, TypeFormatter formatter) {
        List<Class<?>> specificSubclasses = new ArrayList<>();

        if (baseClass.isSealed()) {
            Class<?>[] permittedClasses = baseClass.getPermittedSubclasses();
            specificSubclasses.addAll(Arrays.asList(permittedClasses));
        } else {
            for (Class<?> innerClass : baseClass.getDeclaredClasses()) {
                boolean isAssignable = baseClass.isAssignableFrom(innerClass) && baseClass != innerClass;
                if (isAssignable) {
                    specificSubclasses.add(innerClass);
                }
            }
        }

        if (specificSubclasses.isEmpty()) {
            return null;
        }

        Map<String, Object> polymorphicDefinition = new LinkedHashMap<>();
        List<Map<String, Object>> variantDefinitions = new ArrayList<>();

        for (Class<?> subclass : specificSubclasses) {
            Map<String, Object> variantEntry = new LinkedHashMap<>();
            variantEntry.put("type", subclass.getSimpleName());
            variantEntry.put("fields", extractFields(subclass, formatter));
            variantDefinitions.add(variantEntry);
            queueType(subclass);
        }

        polymorphicDefinition.put("type", "Polymorphic");
        polymorphicDefinition.put("common_fields", extractFields(baseClass, formatter));
        polymorphicDefinition.put("variants", variantDefinitions);

        return polymorphicDefinition;
    }

    public Map<String, Object> process(TypeFormatter formatter) {
        Set<Class<?>> completedTypes = new HashSet<>();
        Map<String, Object> extractionResults = new LinkedHashMap<>();

        while (!pendingTypes.isEmpty()) {
            List<Class<?>> currentBatch = new ArrayList<>(pendingTypes);
            pendingTypes.clear();

            for (Class<?> targetClass : currentBatch) {
                if (completedTypes.contains(targetClass)) {
                    continue;
                }
                completedTypes.add(targetClass);

                Map<String, Object> classDefinition = processSingleType(targetClass, formatter);
                String fullClassName = targetClass.getName();

                if (classDefinition.containsKey("fields")) {
                    Object fieldList = classDefinition.get("fields");
                    extractionResults.put(fullClassName, fieldList);
                } else {
                    extractionResults.put(fullClassName, classDefinition);
                }
            }
        }

        return extractionResults;
    }

    private List<FieldDefinition> extractFields(Class<?> domainClass, TypeFormatter formatter) {
        List<FieldDefinition> fieldDefinitions = new ArrayList<>();

        if (domainClass.isEnum()) {
            for (Object enumConstant : domainClass.getEnumConstants()) {
                String variantName = enumConstant.toString();
                FieldDefinition variantField = FieldDefinition.of(variantName, "EnumVariant");
                fieldDefinitions.add(variantField);
            }
            return fieldDefinitions;
        }

        Class<?> inspectionClass = domainClass;
        while (inspectionClass != null && inspectionClass != Object.class) {
            for (Field field : inspectionClass.getDeclaredFields()) {
                int fieldModifiers = field.getModifiers();
                boolean isStaticOrSynthetic = Modifier.isStatic(fieldModifiers) || field.isSynthetic();

                if (isStaticOrSynthetic) {
                    continue;
                }

                String fieldName = field.getName();
                java.lang.reflect.Type genericType = field.getGenericType();
                String fieldTypeName = formatter.formatType(genericType);

                FieldDefinition fieldDefinition = FieldDefinition.of(fieldName, fieldTypeName);
                fieldDefinitions.add(fieldDefinition);
            }
            inspectionClass = inspectionClass.getSuperclass();
        }

        return fieldDefinitions;
    }
}
