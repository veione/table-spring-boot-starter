package com.think.table.converter;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * String to any map converter
 *
 * @author veione
 */
public class StringToMapConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public StringToMapConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Map.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return canConvertKey(sourceType, targetType) && canConvertValue(sourceType, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }

        Map<Object, Object> sourceMap = new HashMap<>();
        String[] srcArray = StringUtils.commaDelimitedListToStringArray(source.toString());
        for (String s : srcArray) {
            String[] strings = StringUtils.delimitedListToStringArray(s, ":");
            Object key = strings[0];
            Object value = strings[1];
            if (sourceMap.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate key: " + key);
            }
            sourceMap.put(key, value);
        }

        // Shortcut if possible...
        boolean copyRequired = !targetType.getType().isInstance(source);
        if (!copyRequired && sourceMap.isEmpty()) {
            return sourceMap;
        }
        TypeDescriptor keyDesc = targetType.getMapKeyTypeDescriptor();
        TypeDescriptor valueDesc = targetType.getMapValueTypeDescriptor();

        List<MapEntry> targetEntries = new ArrayList<>(sourceMap.size());
        for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
            Object sourceKey = entry.getKey();
            Object sourceValue = entry.getValue();
            Object targetKey = convertKey(sourceKey, sourceType, keyDesc);
            Object targetValue = convertValue(sourceValue, sourceType, valueDesc);
            targetEntries.add(new MapEntry(targetKey, targetValue));
            if (sourceKey != targetKey || sourceValue != targetValue) {
                copyRequired = true;
            }
        }
        if (!copyRequired) {
            return sourceMap;
        }

        Map<Object, Object> targetMap = CollectionFactory.createMap(targetType.getType(),
                (keyDesc != null ? keyDesc.getType() : null), sourceMap.size());

        for (MapEntry entry : targetEntries) {
            entry.addToMap(targetMap);
        }
        return Collections.unmodifiableMap(targetMap);
    }

    // internal helpers
    private boolean canConvertKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return canConvertElements(sourceType, targetType.getMapKeyTypeDescriptor(), this.conversionService);
    }

    private boolean canConvertValue(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return canConvertElements(sourceType, targetType.getMapValueTypeDescriptor(), this.conversionService);
    }


    private Object convertKey(Object sourceKey, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType == null) {
            return sourceKey;
        }
        return this.conversionService.convert(sourceKey, sourceType, targetType);
    }


    private Object convertValue(Object sourceValue, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType == null) {
            return sourceValue;
        }
        return this.conversionService.convert(sourceValue, sourceType, targetType);
    }

    private boolean canConvertElements(TypeDescriptor sourceElementType, TypeDescriptor targetElementType, ConversionService conversionService) {
        if (targetElementType == null) {
            // yes
            return true;
        }
        if (sourceElementType == null) {
            // maybe
            return true;
        }
        if (conversionService.canConvert(sourceElementType, targetElementType)) {
            // yes
            return true;
        }
        if (ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType())) {
            // maybe
            return true;
        }
        // no
        return false;
    }

    private record MapEntry(Object key, Object value) {

        public void addToMap(Map<Object, Object> map) {
            map.put(this.key, this.value);
        }
    }
}
