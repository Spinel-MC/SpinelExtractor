package net.vaprant.spinelextractor.protocol.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.vaprant.spinelextractor.protocol.definition.FieldDefinition;
import java.lang.reflect.Field;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.*;

public final class CodecDeconstructor {

    private static final Logger LOG = LoggerFactory.getLogger(CodecDeconstructor.class);
    private static final int MAX_COMPOSITE_FIELDS = 16;
    
    private final WireTypeFormatter formatter;

    public CodecDeconstructor(WireTypeFormatter formatter) {
        this.formatter = formatter;
    }

    public List<FieldDefinition> deconstruct(Object codec) {
        Set<Object> processedCodecs = new HashSet<>();
        return deconstruct(codec, processedCodecs);
    }

    private List<FieldDefinition> deconstruct(Object codec, Set<Object> processedCodecs) {
        if (codec == null) {
            return new ArrayList<>();
        }

        boolean isAlreadyProcessed = !processedCodecs.add(codec);
        if (isAlreadyProcessed) {
            return new ArrayList<>();
        }

        String[] wrapperFieldNames = {"codec", "wrapped", "delegate", "streamCodec"};
        for (String fieldName : wrapperFieldNames) {
            Object wrappedCodec = ReflectionAccess.getHiddenFieldValue(codec, fieldName);
            boolean isDistinctWrapper = wrappedCodec != null && wrappedCodec != codec;
            if (isDistinctWrapper) {
                return deconstruct(wrappedCodec, processedCodecs);
            }
        }

        if (isDispatch(codec)) {
            List<Map<String, Object>> polymorphicVariants = deconstructDispatch(codec);
            FieldDefinition dispatchField = new FieldDefinition("dispatch", "Polymorphic", null, polymorphicVariants);
            return Collections.singletonList(dispatchField);
        }

        if (isComposite(codec)) {
            return deconstructComposite(codec, processedCodecs);
        }

        return new ArrayList<>();
    }

    private boolean isComposite(Object codec) {
        Class<?> inspectionClass = codec.getClass();

        while (inspectionClass != null && inspectionClass != Object.class) {
            String className = inspectionClass.getName();
            boolean isCompositeByStructure = className.contains("CompositeStreamCodec") 
                || className.contains("MemberComponent");

            if (isCompositeByStructure) {
                return true;
            }

            for (Field field : inspectionClass.getDeclaredFields()) {
                String fieldName = field.getName();
                boolean hasCodecIndicators = fieldName.equals("codecs") || fieldName.startsWith("codec");
                if (hasCodecIndicators) {
                    return true;
                }
            }
            inspectionClass = inspectionClass.getSuperclass();
        }

        return false;
    }

    private boolean isDispatch(Object codec) {
        Class<?> inspectionClass = codec.getClass();

        while (inspectionClass != null && inspectionClass != Object.class) {
            for (Field field : inspectionClass.getDeclaredFields()) {
                String fieldName = field.getName();
                boolean isExplicitDispatch = fieldName.equals("typeCodec") 
                    || fieldName.equals("codecGetter") 
                    || fieldName.equals("keyReader");

                if (isExplicitDispatch) {
                    return true;
                }
                
                Class<?> fieldType = field.getType();
                String typeName = fieldType.getName();
                boolean isFunctionLambda = fieldName.equals("val$codec") && typeName.contains("Function");
                if (isFunctionLambda) {
                    return true;
                }
                
                boolean isTypeLambda = fieldName.equals("val$type") && typeName.contains("Function");
                if (isTypeLambda) {
                    return true;
                }
            }
            inspectionClass = inspectionClass.getSuperclass();
        }

        return false;
    }

    private List<FieldDefinition> deconstructComposite(Object codec, Set<Object> processedCodecs) {
        List<FieldDefinition> fields = new ArrayList<>();
        try {
            Object[] childCodecs = (Object[]) ReflectionAccess.getHiddenFieldValue(codec, "codecs");
            Object[] fieldGetters = (Object[]) ReflectionAccess.getHiddenFieldValue(codec, "getters");

            if (childCodecs == null) {
                childCodecs = tryFindIndexedCodecs(codec);
                fieldGetters = tryFindIndexedGetters(codec);
            }

            if (childCodecs != null) {
                for (int index = 0; index < childCodecs.length; index++) {
                    Object childCodec = childCodecs[index];
                    String fieldName = "field_" + index;
                    if (fieldGetters != null && index < fieldGetters.length) {
                        fieldName = tryRecoverName(fieldGetters[index], fieldName);
                    }
                    
                    String typeName = formatCodecType(childCodec);
                    List<FieldDefinition> subFields = deconstruct(childCodec, processedCodecs);
                    List<Map<String, Object>> variants = null;
                    
                    if (isDispatch(childCodec)) {
                        variants = deconstructDispatch(childCodec);
                        typeName = "Polymorphic";
                    }
                    
                    List<FieldDefinition> nestedFields = subFields.isEmpty() ? null : subFields;
                    boolean isUnknownLeaf = "unknown".equals(typeName) && nestedFields == null && variants == null;
                    if (isUnknownLeaf) {
                        continue;
                    }
                    FieldDefinition compositeField = new FieldDefinition(fieldName, typeName, nestedFields, variants);
                    fields.add(compositeField);
                }
            }
        } catch (Exception exception) {
            String errorMessage = exception.getMessage();
            LOG.debug("Failed to deconstruct composite codec: {}", errorMessage);
        }
        return fields;
    }

    private Object[] tryFindIndexedCodecs(Object codec) {
        List<Object> foundCodecs = new ArrayList<>();
        for (int index = 1; index <= MAX_COMPOSITE_FIELDS; index++) {
            Object indexedCodec = ReflectionAccess.getHiddenFieldValue(codec, "codec" + index);
            if (indexedCodec == null) {
                indexedCodec = ReflectionAccess.getHiddenFieldValue(codec, "val$codec" + index);
            }
            
            if (indexedCodec == null) {
                break;
            }
            foundCodecs.add(indexedCodec);
        }
        return foundCodecs.isEmpty() ? null : foundCodecs.toArray();
    }

    private Object[] tryFindIndexedGetters(Object codec) {
        List<Object> foundGetters = new ArrayList<>();
        for (int index = 1; index <= MAX_COMPOSITE_FIELDS; index++) {
            Object getter = ReflectionAccess.getHiddenFieldValue(codec, "getter" + index);
            if (getter == null) {
                getter = ReflectionAccess.getHiddenFieldValue(codec, "val$getter" + index);
            }
            
            if (getter == null) {
                break;
            }
            foundGetters.add(getter);
        }
        return foundGetters.isEmpty() ? null : foundGetters.toArray();
    }

    private List<Map<String, Object>> deconstructDispatch(Object codec) {
        List<Map<String, Object>> variants = new ArrayList<>();
        try {
            Object typeCodec = ReflectionAccess.getHiddenFieldValue(codec, "typeCodec");
            Object codecGetter = ReflectionAccess.getHiddenFieldValue(codec, "codecGetter");

            if (typeCodec == null) {
                typeCodec = ReflectionAccess.getHiddenFieldValue(codec, "keyReader");
            }
            
            if (codecGetter == null) {
                codecGetter = ReflectionAccess.getHiddenFieldValue(codec, "val$codec");
            }

            if (typeCodec == null) {
                typeCodec = findCodecByType(codec, "StreamCodec");
            }

            if (codecGetter == null) {
                codecGetter = findCodecByType(codec, "Function");
            }
            
            if (typeCodec != null && codecGetter != null) {
                Class<?> enumClass = findValueClass(typeCodec);
                populateVariants(variants, enumClass, codecGetter);
            }
        } catch (Exception exception) {
            String errorMessage = exception.getMessage();
            LOG.debug("Failed to deconstruct dispatch codec: {}", errorMessage);
        }
        return variants;
    }

    private void populateVariants(List<Map<String, Object>> variants, Class<?> enumClass, Object codecGetter) throws Exception {
        if (enumClass == null || !enumClass.isEnum()) {
            return;
        }

        Method applyMethod = null;
        for (Method method : codecGetter.getClass().getDeclaredMethods()) {
            boolean isEligible = !method.isSynthetic() && (method.getName().equals("apply") || method.getName().equals("getCodec") || method.getParameterCount() == 1);
            if (isEligible) {
                applyMethod = method;
                break;
            }
        }
        
        if (applyMethod == null) {
            return;
        }

        applyMethod.setAccessible(true);
        for (Object enumConstant : enumClass.getEnumConstants()) {
            Object subCodec = applyMethod.invoke(codecGetter, enumConstant);
            if (subCodec != null) {
                Map<String, Object> variant = new LinkedHashMap<>();
                String constantId = enumConstant.toString();
                variant.put("id", constantId);
                variant.put("fields", deconstruct(subCodec));
                variants.add(variant);
            }
        }
    }

    private Object findCodecByType(Object container, String typeFragment) {
        Class<?> containerClass = container.getClass();
        for (Field field : containerClass.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            String typeName = fieldType.getName();
            if (typeName.contains(typeFragment)) {
                String fieldName = field.getName();
                return ReflectionAccess.getHiddenFieldValue(container, fieldName);
            }
        }
        return null;
    }

    private Class<?> findValueClass(Object codec) {
        Class<?> inspectionClass = codec.getClass();
        while (inspectionClass != null && inspectionClass != Object.class) {
            for (java.lang.reflect.Type genericInterface : inspectionClass.getGenericInterfaces()) {
                if (genericInterface instanceof java.lang.reflect.ParameterizedType parameterizedType) {
                    for (java.lang.reflect.Type typeArgument : parameterizedType.getActualTypeArguments()) {
                        if (typeArgument instanceof Class<?> argumentClass) {
                            boolean isBufferType = argumentClass == byte[].class || argumentClass.getName().contains("ByteBuf");
                            if (!isBufferType) {
                                return argumentClass;
                            }
                        }
                    }
                }
            }
            inspectionClass = inspectionClass.getSuperclass();
        }
        return null;
    }

    private String tryRecoverName(Object getter, String fallback) {
        try {
            Class<?> getterClass = getter.getClass();
            Method writeReplaceMethod = getterClass.getDeclaredMethod("writeReplace");
            writeReplaceMethod.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplaceMethod.invoke(getter);
            String implementationMethodName = lambda.getImplMethodName();
            
            if (implementationMethodName.startsWith("get")) {
                String rawName = implementationMethodName.substring(3);
                char firstChar = Character.toLowerCase(rawName.charAt(0));
                return firstChar + rawName.substring(1);
            }
            return implementationMethodName;
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String formatCodecType(Object codec) {
        if (codec == null) {
            return "unknown";
        }
        Class<?> codecClass = codec.getClass();
        String qualifiedClassName = codecClass.getName();
        
        if (qualifiedClassName.contains("ByteBufCodecs$")) {
            String typeDescription = codecClass.getSimpleName();
            if (typeDescription.contains("VAR_INT")) {
                return "VarInt";
            }
            if (typeDescription.contains("VAR_LONG")) {
                return "VarLong";
            }
            if (typeDescription.contains("STRING")) {
                return "String";
            }
            if (typeDescription.contains("BOOL")) {
                return "Boolean";
            }
            if (typeDescription.contains("FLOAT")) {
                return "Float";
            }
            if (typeDescription.contains("DOUBLE")) {
                return "Double";
            }
            if (typeDescription.contains("LONG")) {
                return "Long";
            }
            if (typeDescription.contains("SHORT")) {
                return "Short";
            }
            if (typeDescription.contains("BYTE")) {
                return "Byte";
            }
            if (typeDescription.contains("UUID")) {
                return "UUID";
            }
            if (typeDescription.contains("NBT") || typeDescription.contains("TAG")) {
                return "NBT";
            }
            return "unknown";
        }

        if (isDispatch(codec)) {
            return "Polymorphic";
        }

        Class<?> domainValueClass = findValueClass(codec);
        if (domainValueClass != null) {
            return formatter.formatType(domainValueClass);
        }

        return "unknown";
    }
}
