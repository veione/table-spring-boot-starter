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

import com.think.table.reader.csv.exceptions.CsvBadConverterException;
import com.think.table.reader.csv.exceptions.CsvBeanIntrospectionException;
import com.think.table.reader.csv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.convert.ConversionService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * This class is used for combining multiple columns of the input, possibly
 * with multiple identically named columns, into one field.
 *
 * @param <T> The type of the bean being populated
 * @param <I> The index of the {@link MultiValuedMap} in use
 * @author Andrew Rucker Jones
 * @since 4.2
 */
abstract public class BeanFieldJoin<T, I> extends BeanFieldSingleValue<T, I> {

    /**
     * The type of the {@link MultiValuedMap}
     * that should be instantiated for the bean field being populated.
     */
    private final Class<? extends MultiValuedMap> mapType;

    /**
     * Creates a new instance.
     *
     * @param type The type of the class in which this field is found. This is
     *             the type as instantiated by opencsv, and not necessarily the
     *             type in which the field is declared in the case of
     *             inheritance.
     * @param field The bean field this object represents
     * @param required Whether or not a value is always required for this field
     * @param mapType The type of the
     *   {@link MultiValuedMap} that should be
     *   instantiated for the bean field being populated
     */
    public BeanFieldJoin(
            Class<?> type, Field field, boolean required, ConversionService conversionService, Class<? extends MultiValuedMap> mapType) {

        // Simple assignments
        super(type, field, required, conversionService);

        // Check that we really have a collection
        if(!MultiValuedMap.class.isAssignableFrom(field.getType())) {
            throw new CsvBadConverterException(
                    BeanFieldJoin.class,
                    String.format("The specified type for the map is either unknown or does not implement org.apache.commons.collections4.MultiValuedMap: %s",
                            field.getType().toString()));
        }

        // Determine the MultiValuedMap implementation that should be
        // instantiated for every bean.
        Class<?> fieldType = field.getType();
        if(!fieldType.isInterface()) {
            this.mapType = (Class<MultiValuedMap>)field.getType();
        }
        else if(!mapType.isInterface()) {
            this.mapType = mapType;
        }
        else {
            if(MultiValuedMap.class.equals(fieldType) || ListValuedMap.class.equals(fieldType)) {
                this.mapType = ArrayListValuedHashMap.class;
            }
            else if(SetValuedMap.class.equals(fieldType)) {
                this.mapType = HashSetValuedHashMap.class;
            }
            else {
                this.mapType = null;
                throw new CsvBadConverterException(
                        BeanFieldJoin.class,
                        String.format("The specified type for the map is either unknown or does not implement org.apache.commons.collections4.MultiValuedMap: %s",
                                mapType.toString()));
            }
        }

        // Now that we know what type we want to assign, run one last check
        // that assignment is truly possible
        if(!field.getType().isAssignableFrom(this.mapType)) {
            throw new CsvBadConverterException(
                    BeanFieldJoin.class,
                    String.format("The MultiValuedMap implementation specified (%s) cannot be assigned to the type of the bean field (%s).",
                            mapType.getName(), field.getType().getName()));
        }
    }

    /**
     * Puts the value given in {@code newValue} into {@code map} using
     * {@code index}.
     * This allows derived classes to do something special before assigning the
     * value, such as converting the index to a different type.
     *
     * @param map The map to which to assign the new value. Never null.
     * @param index The index under which the new value should be placed in the
     *   map. Never null.
     * @param newValue The new value to be put in the map
     * @return The previous value under this index, or null if there was no
     *   previous value
     */
    abstract protected Object putNewValue(MultiValuedMap<I, Object> map, String index, Object newValue);

    /**
     * Assigns the value given to the proper field of the bean given.
     * In the case of this kind of bean field, the new value will be added to
     * an existing map, and a new map will be created if one does not already
     * exist.
     */
    // The rest of the Javadoc is inherited
    @Override
    protected void assignValueToField(Object bean, Object obj, String header)
            throws CsvDataTypeMismatchException {

        // Find and use getter and setter methods if available
        // obj == null means that the source field was empty. Then we simply
        // make certain that a(n empty) map exists.
        @SuppressWarnings("unchecked")
        MultiValuedMap<I,Object> currentValue = (MultiValuedMap<I,Object>) getFieldValue(bean);
        try {
            if(currentValue == null) {
                Constructor<? extends MultiValuedMap> c = mapType.getConstructor();
                currentValue = c.newInstance();
            }
            putNewValue(currentValue, header, obj);
            super.assignValueToField(bean, currentValue, header);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            CsvBeanIntrospectionException csve =
                    new CsvBeanIntrospectionException(bean, field,
                            e.getLocalizedMessage());
            csve.initCause(e);
            throw csve;
        } catch(InstantiationException | NoSuchMethodException e) {
            CsvBadConverterException csve = new CsvBadConverterException(
                    BeanFieldJoin.class,
                    String.format("A multi-valued map of type [%s] cannot be instantiated with a nullary constructor.",
                            mapType.getName()));
            csve.initCause(e);
            throw csve;
        }
    }

    /**
     * @return An array of all objects in the
     *   {@link MultiValuedMap} addressed by
     *   this bean field answering to the key given in {@code index}
     */
    // The rest of the Javadoc is inherited
    @Override
    public Object[] indexAndSplitMultivaluedField(Object value, I index)
            throws CsvDataTypeMismatchException {
        Object[] splitObjects = ArrayUtils.EMPTY_OBJECT_ARRAY;
        if(value != null) {
            if(MultiValuedMap.class.isAssignableFrom(value.getClass())) {
                @SuppressWarnings("unchecked")
                MultiValuedMap<Object,Object> map = (MultiValuedMap<Object,Object>) value;
                Collection<Object> splitCollection = map.get(index);
                splitObjects = splitCollection.toArray(ArrayUtils.EMPTY_OBJECT_ARRAY);
            }
            else {
                // Note about code coverage: I sincerely doubt this code is
                // reachable. It is meant as one more safeguard.
                throw new CsvDataTypeMismatchException(value, String.class,
                        "The bean field must implement org.apache.commons.collections4.MultiValuedMap.");
            }
        }
        return splitObjects;
    }

    /**
     * Checks that {@code value} is not null and not empty.
     */
    // The rest of the Javadoc is inherited
    @Override
    @SuppressWarnings("unchecked")
    protected boolean isFieldEmptyForWrite(Object value) {
        return super.isFieldEmptyForWrite(value) || ((MultiValuedMap<Object, Object>)value).isEmpty();
    }
}
