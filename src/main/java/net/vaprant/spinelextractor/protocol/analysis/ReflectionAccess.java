package net.vaprant.spinelextractor.protocol.analysis;

import java.lang.reflect.Field;

public final class ReflectionAccess {

    private ReflectionAccess() {}

    public static Object getHiddenFieldValue(Object instance, String fieldName) {
        Class<?> currentClass = instance.getClass();

        while (currentClass != null) {
            try {
                Field targetField = currentClass.getDeclaredField(fieldName);
                targetField.setAccessible(true);
                return targetField.get(instance);
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException accessException) {
                throw new ReflectionExtractionException("Could not access field: " + fieldName, accessException);
            }
        }

        return null;
    }

    public static boolean hasFieldWithName(Object instance, String fieldName) {
        Object fieldValue = getHiddenFieldValue(instance, fieldName);
        return fieldValue != null;
    }
}
