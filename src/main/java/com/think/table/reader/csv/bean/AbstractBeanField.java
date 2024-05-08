/*
 * Copyright 2016 Andrew Rucker Jones.
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

import com.think.table.reader.csv.bean.util.OpencsvUtils;
import com.think.table.reader.csv.exceptions.CsvBeanIntrospectionException;
import com.think.table.reader.csv.exceptions.CsvConstraintViolationException;
import com.think.table.reader.csv.exceptions.CsvDataTypeMismatchException;
import com.think.table.reader.csv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * This base bean takes over the responsibility of converting the supplied
 * string to the proper type for the destination field and setting the
 * destination field.
 * <p>All custom converters must be descended from this class.</p>
 * <p>Internally, opencsv uses another set of classes for the actual conversion,
 * leaving this class mostly to deal with assigment to bean fields.</p>
 *
 * @param <T> Type of the bean being populated
 * @param <I> Type of the index into a multivalued field
 * @author Andrew Rucker Jones
 * @since 3.8
 */
abstract public class AbstractBeanField<T, I> implements BeanField<T, I> {

    /**
     * The type the field is located in.
     * This is not necessarily the declaring class in the case of inheritance,
     * but rather the type that opencsv expects to instantiate.
     */
    protected Class<?> type;

    /**
     * The field this class represents.
     */
    protected Field field;

    /**
     * The field is primitive?
     */
    protected boolean isPrimitive;

    /**
     * The field type descriptor
     */
    protected TypeDescriptor typeDescriptor;

    protected ConversionService conversionService;

    /**
     * Whether or not this field is required.
     */
    protected boolean required;

    /**
     * An encapsulated way of accessing the member variable associated with this
     * field.
     */
    protected FieldAccess<Object> fieldAccess;

    /**
     * Default nullary constructor, so derived classes aren't forced to create
     * a constructor identical to this one.
     */
    public AbstractBeanField() {
        required = false;
    }

    /**
     * @param type The type of the class in which this field is found. This is
     *             the type as instantiated by opencsv, and not necessarily the
     *             type in which the field is declared in the case of
     *             inheritance.
     * @param field       A {@link Field} object.
     * @param required    Whether or not this field is required in input
     * @since 4.2
     */
    public AbstractBeanField(Class<?> type, Field field, boolean required, ConversionService conversionService) {
        this.type = type;
        this.field = field;
        this.required = required;
        this.isPrimitive = field.getType().isPrimitive();
        this.conversionService = conversionService;
        fieldAccess = new FieldAccess<>(this.field);
        typeDescriptor = OpencsvUtils.getTypeDescriptor(field);
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public void setType(Class<?> type) { this.type = type; }

    @Override
    public void setField(Field field) {
        this.field = field;
        fieldAccess = new FieldAccess<>(this.field);
    }

    @Override
    public Field getField() {
        return this.field;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public final void setFieldValue(Object bean, String value, String header)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException,
            CsvConstraintViolationException {
        if (required && StringUtils.isBlank(value)) {
            throw new CsvRequiredFieldEmptyException(
                    bean.getClass(), field,
                    String.format("Field '%s' is mandatory but no value was provided.",
                            field.getName()));
        }

        assignValueToField(bean, convert(value), header);
    }

    @Override
    public Object getFieldValue(Object bean) {
        Object o = null;
        try {
            o = fieldAccess.getField(bean);
        }
        catch(IllegalAccessException | InvocationTargetException e) {
            // Our testing indicates these exceptions probably can't be thrown,
            // but they're declared, so we have to deal with them. It's an
            // alibi catch block.
            CsvBeanIntrospectionException csve = new CsvBeanIntrospectionException(
                    bean, field,
                    String.format("An introspection error was thrown while attempting to manipulate property %1$s of bean %2$s.",
                            field.getName(), bean.getClass().toString()));
            csve.initCause(e);
            throw csve;
        }
        return o;
    }

    public final Object getFieldValue(Class<?> type, String value) throws CsvRequiredFieldEmptyException, CsvConstraintViolationException, CsvDataTypeMismatchException {
        if (required && StringUtils.isBlank(value)) {
            throw new CsvRequiredFieldEmptyException(
                    type, field,
                    String.format("Field '%s' is mandatory but no value was provided.",
                            field.getName()));
        }

        return convert(value);
    }

    /**
     * @return {@code value} wrapped in an array, since we assume most values
     * will not be multi-valued
     * @since 4.2
     */
    // The rest of the Javadoc is inherited
    @Override
    public Object[] indexAndSplitMultivaluedField(Object value, I index)
            throws CsvDataTypeMismatchException {
        return new Object[]{value};
    }

    /**
     * Whether or not this implementation of {@link BeanField} considers the
     * value passed in as empty for the purposes of determining whether or not
     * a required field is empty.
     * <p>This allows any overriding class to define "empty" while writing
     * values to a CSV file in a way that is meaningful for its own data. A
     * simple example is a {@link java.util.Collection} that is not null, but
     * empty.</p>
     * <p>The default implementation simply checks for {@code null}.</p>
     *
     * @param value The value of a field out of a bean that is being written to
     *              a CSV file. Can be {@code null}.
     * @return Whether or not this implementation considers {@code value} to be
     * empty for the purposes of its conversion
     * @since 4.2
     */
    protected boolean isFieldEmptyForWrite(Object value) {
        return value == null;
    }

    /**
     * Assigns the given object to this field of the destination bean.
     * <p>Uses the setter method if available.</p>
     * <p>Derived classes can override this method if they have special needs
     * for setting the value of a field, such as adding to an existing
     * collection.</p>
     *
     * @param bean   The bean in which the field is located
     * @param obj    The data to be assigned to this field of the destination bean
     * @param header The header from the CSV file under which this value was found.
     * @throws CsvDataTypeMismatchException If the data to be assigned cannot
     *                                      be converted to the type of the destination field
     */
    protected void assignValueToField(Object bean, Object obj, String header)
            throws CsvDataTypeMismatchException {

        // obj == null means that the source field was empty. Then we simply
        // leave the field as it was initialized by the VM. For primitives,
        // that will be values like 0, and for objects it will be null.
        if (obj != null) {
            try {
                fieldAccess.setField(bean, obj);
            } catch (InvocationTargetException | IllegalAccessException e) {
                CsvBeanIntrospectionException csve = new CsvBeanIntrospectionException(bean, field, e.getLocalizedMessage());
                csve.initCause(e);
                throw csve;
            } catch (IllegalArgumentException e2) {
                CsvDataTypeMismatchException csve = new CsvDataTypeMismatchException(obj, field.getType());
                csve.initCause(e2);
                throw csve;
            }
        }
    }

    /**
     * Method for converting from a string to the proper datatype of the
     * destination field.
     * This method must be specified in all non-abstract derived classes.
     *
     * @param value The string from the selected field of the CSV file. If the
     *              field is marked as required in the annotation, this value is guaranteed
     *              not to be null, empty or blank according to
     *              {@link StringUtils#isBlank(CharSequence)}
     * @return An {@link Object} representing the input data converted
     * into the proper type
     * @throws CsvDataTypeMismatchException    If the input string cannot be converted into
     *                                         the proper type
     * @throws CsvConstraintViolationException When the internal structure of
     *                                         data would be violated by the data in the CSV file
     */
    protected abstract Object convert(String value)
            throws CsvDataTypeMismatchException, CsvConstraintViolationException;

    /**
     * This method takes the current value of the field in question in the bean
     * passed in and converts it to a string.
     * It is actually a stub that calls {@link #convertToWrite(Object)}
     * for the actual conversion, and itself performs validation and handles
     * exceptions thrown by {@link #convertToWrite(Object)}. The
     * validation consists of verifying that both {@code bean} and {@link #field}
     * are not null before calling {@link #convertToWrite(Object)}.
     */
    // The rest of the Javadoc is automatically inherited
    @Override
    public final String[] write(Object bean, I index) throws CsvDataTypeMismatchException,
            CsvRequiredFieldEmptyException {

        // If the input is empty, check if the field is required
        Object value = bean != null ? getFieldValue(bean): null;
        if(required && (bean == null || isFieldEmptyForWrite(value))) {
            throw new CsvRequiredFieldEmptyException(type, field,
                    String.format("Field '%s' is mandatory but no value was provided.",
                            field.getName()));
        }

        String[] result;
        Object[] multivalues = indexAndSplitMultivaluedField(value, index);
        String[] intermediateResult = new String[multivalues.length];
        try {
            for (int i = 0; i < multivalues.length; i++) {
                intermediateResult[i] = convertToWrite(multivalues[i]);
            }
            result = intermediateResult;
        } catch (CsvDataTypeMismatchException e) {
            CsvDataTypeMismatchException csve = new CsvDataTypeMismatchException(
                    bean, field.getType(), e.getMessage());
            csve.initCause(e.getCause());
            throw csve;
        } catch (CsvRequiredFieldEmptyException e) {
            // Our code no longer throws this exception from here, but
            // rather from write() using isFieldEmptyForWrite() to determine
            // when to throw the exception. But user code is still allowed
            // to override convertToWrite() and throw this exception
            Class<?> beanClass = bean == null ? null : bean.getClass();
            CsvRequiredFieldEmptyException csve = new CsvRequiredFieldEmptyException(
                    beanClass, field, e.getMessage());
            csve.initCause(e.getCause());
            throw csve;
        }
        return result;
    }

    /**
     * This is the method that actually performs the conversion from field to
     * string for {@link #write(Object, Object) } and should
     * be overridden in derived classes.
     * <p>The default implementation simply calls {@code toString()} on the
     * object in question. Derived classes will, in most cases, want to override
     * this method. Alternatively, for complex types, overriding the
     * {@code toString()} method in the type of the field in question would also
     * work fine.</p>
     *
     * @param value The contents of the field currently being processed from the
     *              bean to be written. Can be null if the field is not marked as required.
     * @return A string representation of the value of the field in question in
     * the bean passed in, or an empty string if {@code value} is null
     * @throws CsvDataTypeMismatchException   This implementation does not throw
     *                                        this exception
     * @throws CsvRequiredFieldEmptyException If the input is empty but the
     *                                        field is required. The case of the field being null is checked before
     *                                        this method is called, but other implementations may have other cases
     *                                        that are semantically equivalent to being empty, such as an empty
     *                                        collection. The preferred way to perform this check is in
     *                                        {@link #isFieldEmptyForWrite(Object) }. This exception may
     *                                        be removed from this method signature sometime in the future.
     * @see #write(Object, Object)
     * @since 3.9
     */
    protected String convertToWrite(Object value)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        // Since we have no concept of which field is required at this level,
        // we can't check for null and throw an exception.
        return Objects.toString(value, StringUtils.EMPTY);
    }

    public TypeDescriptor getTypeDescriptor() {
        return typeDescriptor;
    }
}
