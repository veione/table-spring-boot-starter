package com.think.table.reader;

import org.springframework.core.convert.TypeDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class FieldType {
    protected Field field;
    protected TypeDescriptor typeDescriptor;

    public FieldType(Field field) {
        this.field = field;
        Class<?> fieldType = field.getType();
        if (Map.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                Type keyType = typeArguments[0];
                Type valueType = typeArguments[1];
                this.typeDescriptor = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf((Class<?>) keyType), TypeDescriptor.valueOf((Class<?>) valueType));
            }
        } else if (List.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                this.typeDescriptor = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf((Class<?>) typeArguments[0]));
            }
        } else if (Set.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                this.typeDescriptor = TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf((Class<?>) typeArguments[0]));
            }
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                this.typeDescriptor = TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf((Class<?>) typeArguments[0]));
            }
        } else {
            this.typeDescriptor = TypeDescriptor.valueOf(fieldType);
        }
    }

    public TypeDescriptor getTypeDescriptor() {
        return typeDescriptor;
    }
}
