package com.think.table.reader.util;

import java.util.HashMap;
import java.util.Map;

public final class TypeUtils {
    private static final Map<Class<?>, Object> PRIMITIVE_TYPE_DEFAULT_VALUES = new HashMap<>(8);

    static {
        PRIMITIVE_TYPE_DEFAULT_VALUES.put(int.class, 0);
        PRIMITIVE_TYPE_DEFAULT_VALUES.put(double.class, 0.0);
        PRIMITIVE_TYPE_DEFAULT_VALUES.put(float.class, 0F);
        PRIMITIVE_TYPE_DEFAULT_VALUES.put(byte.class, 0);
        PRIMITIVE_TYPE_DEFAULT_VALUES.put(boolean.class, false);
        PRIMITIVE_TYPE_DEFAULT_VALUES.put(char.class, '\u0000');
    }

    public static Object getPrimitiveValue(Class<?> clazz) {
        return PRIMITIVE_TYPE_DEFAULT_VALUES.get(clazz);
    }
}
