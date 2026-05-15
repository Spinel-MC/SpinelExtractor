package net.vaprant.spinelextractor.protocol.analysis;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProtocolNavigator {

    public List<?> findPacketEntries(Object protocolSource) {
        List<Object> visited = new ArrayList<>();
        List<?> packetEntries = findPacketEntryListRecursive(protocolSource, visited);

        if (packetEntries == null) {
            Class<?> sourceClass = protocolSource.getClass();
            String sourceName = sourceClass.getName();
            throw new ReflectionExtractionException("Could not find packet list in " + sourceName, null);
        }

        return packetEntries;
    }

    private List<?> findPacketEntryListRecursive(Object container, List<Object> visited) {
        if (visited.contains(container)) {
            return null;
        }
        visited.add(container);

        Class<?> inspectionClass = container.getClass();
        while (inspectionClass != null) {
            for (Field field : inspectionClass.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(container);
                    if (fieldValue == null) {
                        continue;
                    }

                    if (fieldValue instanceof List<?> list && !list.isEmpty()) {
                        Object firstEntry = list.get(0);
                        if (ReflectionAccess.hasFieldWithName(firstEntry, "type")) {
                            return list;
                        }
                    }

                    String fieldName = field.getName();
                    if (isContainerFieldName(fieldName)) {
                        List<?> nestedResult = findPacketEntryListRecursive(fieldValue, visited);
                        if (nestedResult != null) {
                            return nestedResult;
                        }
                    }
                } catch (Exception ignored) {}
            }
            inspectionClass = inspectionClass.getSuperclass();
        }
        return null;
    }

    public Map<Integer, Class<?>> findIdToClassMapping(Object protocolSource) {
        List<Object> visited = new ArrayList<>();
        return findMappingRecursive(protocolSource, visited);
    }

    private Map<Integer, Class<?>> findMappingRecursive(Object container, List<Object> visited) {
        if (visited.contains(container)) {
            return null;
        }
        visited.add(container);

        Class<?> inspectionClass = container.getClass();
        while (inspectionClass != null) {
            for (Field field : inspectionClass.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(container);
                    if (fieldValue == null) {
                        continue;
                    }

                    String fieldName = field.getName();
                    if (fieldName.equals("toId") && fieldValue instanceof Map<?, ?> mappingMap) {
                        Map<Integer, Class<?>> validMappings = extractValidMappings(mappingMap);
                        if (!validMappings.isEmpty()) {
                            return validMappings;
                        }
                    }

                    if (isContainerFieldName(fieldName)) {
                        Map<Integer, Class<?>> nestedResult = findMappingRecursive(fieldValue, visited);
                        if (nestedResult != null) {
                            return nestedResult;
                        }
                    }
                } catch (Exception ignored) {}
            }
            inspectionClass = inspectionClass.getSuperclass();
        }
        return null;
    }

    private Map<Integer, Class<?>> extractValidMappings(Map<?, ?> rawMapping) {
        Map<Integer, Class<?>> integerToClassMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> mappingEntry : rawMapping.entrySet()) {
            Object mapKey = mappingEntry.getKey();
            Object mapValue = mappingEntry.getValue();

            if (mapKey instanceof Class<?> keyClass && mapValue instanceof Integer valueId) {
                integerToClassMap.put(valueId, keyClass);
                continue;
            }

            if (mapValue instanceof Integer valueId) {
                Class<?> resolvedClass = discoverClassFromObject(mapKey);
                if (resolvedClass != null) {
                    integerToClassMap.put(valueId, resolvedClass);
                }
            } else if (mapKey instanceof Integer keyId) {
                Class<?> resolvedClass = discoverClassFromObject(mapValue);
                if (resolvedClass != null) {
                    integerToClassMap.put(keyId, resolvedClass);
                }
            }
        }
        return integerToClassMap;
    }

    private Class<?> discoverClassFromObject(Object sourceObject) {
        if (sourceObject == null) {
            return null;
        }
        if (sourceObject instanceof Class<?> targetClass) {
            return isPotentialPacketClass(targetClass) ? targetClass : null;
        }

        Object typeValue = ReflectionAccess.getHiddenFieldValue(sourceObject, "type");
        if (typeValue instanceof Class<?> typeClass && isPotentialPacketClass(typeClass)) {
            return typeClass;
        }

        Object classValue = ReflectionAccess.getHiddenFieldValue(sourceObject, "clazz");
        if (classValue instanceof Class<?> targetClass && isPotentialPacketClass(targetClass)) {
            return targetClass;
        }

        return null;
    }

    private boolean isPotentialPacketClass(Class<?> targetClass) {
        if (targetClass.isInterface() || targetClass.isEnum()) {
            return false;
        }
        String simpleName = targetClass.getSimpleName();
        return !simpleName.endsWith("Protocols") && !simpleName.endsWith("Protocol");
    }

    private boolean isContainerFieldName(String fieldName) {
        return fieldName.contains("details") 
            || fieldName.contains("builder") 
            || fieldName.equals("codec") 
            || fieldName.equals("byId");
    }
}
