package com.think.table.reader.excel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class CfgBeanDefinition {
    private final Class<?> clazz;
    private final Map<String, CfgBeanField> fieldMap;
    private final Map<Integer, String> indexNameMap = new HashMap<>();
    private final Class<?>[] constructorParameterTypes;

    public CfgBeanDefinition(Class<?> clazz) {
        this.clazz = clazz;

        // 如果是record类则需要使用构造函数, 如果是普通类则可以采用构造函数
        // 根据名称注入属性
        Field[] fields = clazz.getDeclaredFields();
        fieldMap = new HashMap<>(fields.length);
        this.constructorParameterTypes = new Class[fields.length];

        int index = 0;
        for (Field field : fields) {
            CfgBeanField beanField = new CfgBeanField(field);
            fieldMap.put(field.getName(), beanField);
            indexNameMap.put(index, beanField.getName());
            constructorParameterTypes[index] = field.getType();
            index++;
        }
    }

    public Class<?> getBeanClass() {
        return clazz;
    }

    public CfgBeanField getField(String name) {
        return fieldMap.get(name);
    }

    public Class<?> getFieldType(String name) {
        return fieldMap.get(name).getType();
    }

    public String getName(int index) {
        return indexNameMap.get(index);
    }

    public int getFieldSize() {
        return fieldMap.size();
    }

    public Class<?>[] getConstructorParameterTypes() {
        return constructorParameterTypes;
    }
}
