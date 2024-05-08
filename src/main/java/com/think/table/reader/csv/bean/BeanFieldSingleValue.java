/*
 * Copyright 2017 Andrew Rucker Jones.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.think.table.reader.csv.bean;

import com.think.table.reader.util.TypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import java.lang.reflect.Field;

/**
 * This class concerns itself with handling single-valued bean fields.
 *
 * @param <T> The type of the bean being populated
 * @param <I> Type of the index into a multivalued field
 * @author Andrew Rucker Jones
 * @since 4.2
 */
public class BeanFieldSingleValue<T, I> extends AbstractBeanField<T, I> {
    /**
     * Simply calls the same constructor in the base class.
     *
     * @param type     The type of the class in which this field is found. This is
     *                 the type as instantiated by opencsv, and not necessarily the
     *                 type in which the field is declared in the case of
     *                 inheritance.
     * @param field    A {@link Field} object.
     * @param required Whether or not this field is required in input
     * @see AbstractBeanField#AbstractBeanField(Class, Field, boolean, ConversionService)
     */
    public BeanFieldSingleValue(Class<?> type, Field field, boolean required, ConversionService conversionService) {
        super(type, field, required, conversionService);
    }

    /**
     * Passes the string to be converted to the converter.
     *
     */
    // The rest of the Javadoc is inherited
    @Override
    protected Object convert(String value) {
        if (StringUtils.isEmpty(value) && isPrimitive) {
            // 如果是原始类型的则不能为null,需要使用默认值进行填充
            return TypeUtils.getPrimitiveValue(field.getType());
        }
        return conversionService.convert(value, TypeDescriptor.valueOf(String.class), typeDescriptor);
    }

    /**
     * Passes the object to be converted to the converter.
     *
     */
    // The rest of the Javadoc is inherited
    @Override
    protected String convertToWrite(Object value) {
        return (String) conversionService.convert(value, typeDescriptor, TypeDescriptor.valueOf(String.class));
    }
}
