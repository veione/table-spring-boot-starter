package com.think.table.reader.excel;

import com.think.table.reader.FieldType;
import org.springframework.core.convert.TypeDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CfgBeanField extends FieldType {
    private final String name;
    private final Class<?> fieldType;
    private final boolean isPrimitive;

    public CfgBeanField(Field field) {
        super(field);
        this.name = field.getName();
        this.fieldType = field.getType();
        this.isPrimitive = fieldType.isPrimitive();
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return fieldType;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }
}
