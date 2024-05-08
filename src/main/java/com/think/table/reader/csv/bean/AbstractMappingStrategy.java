/*
 * Copyright 2018 Andrew Rucker Jones.
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
import com.think.table.reader.csv.exceptions.CsvBadConverterException;
import com.think.table.reader.csv.exceptions.CsvBeanIntrospectionException;
import com.think.table.reader.csv.exceptions.CsvChainedException;
import com.think.table.reader.csv.exceptions.CsvConstraintViolationException;
import com.think.table.reader.csv.exceptions.CsvDataTypeMismatchException;
import com.think.table.reader.csv.exceptions.CsvFieldAssignmentException;
import com.think.table.reader.csv.exceptions.CsvRequiredFieldEmptyException;
import com.think.table.reader.csv.exceptions.CsvValidationException;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class collects as many generally useful parts of the implementation
 * of a mapping strategy as possible.
 * <p>This mapping strategy knows of the existence of binding annotations, but
 * assumes through {@link #getBindingAnnotations()} they are not in use.</p>
 * <p>Anyone is welcome to use it as a base class for their own mapping
 * strategies.</p>
 *
 * @param <T> Type of object that is being processed.
 * @param <C> The type of the internal many-to-one mapping
 * @param <I> The initializer type used to build the internal many-to-one mapping
 * @param <K> The type of the key used for internal indexing
 * @author Andrew Rucker Jones
 * @since 4.2
 */
public abstract class AbstractMappingStrategy<I, K extends Comparable<K>, C extends ComplexFieldMapEntry<I, K, T>, T> implements MappingStrategy<T> {
    protected final ConversionService conversionService;
    /**
     * This is the class of the bean to be manipulated.
     */
    protected Class<? extends T> type;

    /**
     * The type class is record class.
     */
    protected boolean isRecord;
    /**
     * The type class is fully argument constructor
     */
    protected boolean isFullyArgumentConstructor;

    /**
     * Maintains a bi-directional mapping between column position(s) and header
     * name.
     */
    protected final HeaderIndex headerIndex = new HeaderIndex();

    public AbstractMappingStrategy(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * For {@link BeanField#indexAndSplitMultivaluedField(Object, Object)}
     * it is necessary to determine which index to pass in.
     *
     * @param index The current column position while transmuting a bean to CSV
     *              output
     * @return The index to be used for this mapping strategy for
     * {@link BeanField#indexAndSplitMultivaluedField(Object, Object) }
     */
    protected abstract K chooseMultivaluedFieldIndexFromHeaderIndex(int index);

    /**
     * Returns the {@link FieldMap} associated with this mapping strategy.
     *
     * @return The {@link FieldMap} used by this strategy
     */
    protected abstract FieldMap<I, K, ? extends C, T> getFieldMap();

    /**
     * Creates a map of fields in the bean to be processed that have no
     * annotations.
     * This method is called by {@link #loadFieldMap()} when absolutely no
     * annotations that are relevant for this mapping strategy are found in the
     * type of bean being processed.
     *
     * @param fields A list of all non-synthetic fields in the bean to be
     *               processed
     * @since 5.0
     */
    protected abstract void loadFieldMap(ListValuedMap<Class<?>, Field> fields);

    /**
     * Creates an empty binding-type-specific field map that can be filled in
     * later steps.
     * <p>This method may be called multiple times and must erase any state
     * information from previous calls.</p>
     *
     * @since 5.0
     */
    protected abstract void initializeFieldMap();

    /**
     * Gets the field for a given column position.
     *
     * @param col The column to find the field for
     * @return BeanField containing the field for a given column position, or
     * null if one could not be found
     * @throws CsvBadConverterException If a custom converter for a field cannot
     *                                  be initialized
     */
    protected abstract BeanField<T, K> findField(int col);

    /**
     * Must be called once the length of input for a line/record is known to
     * verify that the line was complete.
     * Complete in this context means, no required fields are missing. The issue
     * here is, as long as a column is present but empty, we can check whether
     * the field is required and throw an exception if it is not, but if the data
     * end prematurely, we never have this chance without indication that no more
     * data are on the way.
     * Another validation is that the number of fields must match the number of
     * headers to prevent a data mismatch situation.
     *
     * @param numberOfFields The number of fields present in the line of input
     * @throws CsvRequiredFieldEmptyException If a required column is missing
     * @since 4.0
     */
    protected abstract void verifyLineLength(int numberOfFields) throws CsvRequiredFieldEmptyException;

    /**
     * Implementation will return a bean of the type of object being mapped.
     *
     * @return A new instance of the class being mapped.
     * @throws CsvBeanIntrospectionException Thrown on error creating object.
     * @throws IllegalStateException         If the type of the bean has not been
     *                                       initialized through {@link #setType(Class)}
     */
    protected Constructor<T> createBeanConstructor() throws CsvBeanIntrospectionException, IllegalStateException {
        if (type == null) {
            throw new IllegalStateException("The type has not been set in the MappingStrategy.");
        }

        return (Constructor<T>) BeanUtils.getResolvableConstructor(type);
    }

    /**
     * Implementation will return a bean of the type of object being mapped.
     *
     * @return A new instance of the class being mapped.
     * @throws CsvBeanIntrospectionException Thrown on error creating object.
     * @throws IllegalStateException         If the type of the bean has not been
     *                                       initialized through {@link #setType(Class)}
     */
    protected T createBean()
            throws CsvBeanIntrospectionException, IllegalStateException {
        if (type == null) {
            throw new IllegalStateException("The type has not been set in the MappingStrategy.");
        }

        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            CsvBeanIntrospectionException csve = new CsvBeanIntrospectionException(
                    "Basic instantiation of the given bean type (and subordinate beans created through recursion, if applicable) was determined to be impossible.");
            csve.initCause(e);
            throw csve;
        }
    }

    /**
     * Creates an index of necessary types according to the mapping strategy
     * and existing instances of (subordinate) beans.
     *
     * @param bean The root bean to be indexed
     * @return The index from type to instance
     * @throws IllegalAccessException    If there are problems accessing a
     *                                   subordinate bean
     * @throws InvocationTargetException If there are problems accessing a
     *                                   subordinate bean
     * @since 5.0
     */
    protected Map<Class<?>, Object> indexBean(T bean)
            throws IllegalAccessException, InvocationTargetException {
        Map<Class<?>, Object> instanceMap = new HashMap<>();
        instanceMap.put(type, bean);
        return instanceMap;
    }

    /**
     * Gets the name (or position number) of the header for the given column
     * number.
     * The column numbers are zero-based.
     *
     * @param col The column number for which the header is sought
     * @return The name of the header
     */
    public abstract String findHeader(int col);

    /**
     * This method generates a header that can be used for writing beans of the
     * type provided back to a file.
     * <p>The ordering of the headers is determined by the
     * {@link com.think.csv.bean.FieldMap} in use.</p>
     * <p>This method should be called first by all overriding classes to make
     * certain {@link #headerIndex} is properly initialized.</p>
     */
    // The rest of the Javadoc is inherited
    @Override
    public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
        if (type == null) {
            throw new IllegalStateException(
                    "You must call MappingStrategy.setType() before calling MappingStrategy.generateHeader().");
        }

        // Always take what's been given or previously determined first.
        if (headerIndex.isEmpty()) {
            String[] header = getFieldMap().generateHeader(bean);
            headerIndex.initializeHeaderIndex(header);
            return header;
        }

        // Otherwise, put headers in the right places.
        return headerIndex.getHeaderIndex();
    }

    /**
     * Get the column name for a given column position.
     *
     * @param col Column position.
     * @return The column name or null if the position is larger than the
     * header array or there are no headers defined.
     */
    protected String getColumnName(int col) {
        // headerIndex is never null because it's final
        return headerIndex.getByPosition(col);
    }

    /**
     * Get the class type that the strategy is mapping.
     *
     * @return Class of the object that this {@link MappingStrategy} will create.
     */
    public Class<? extends T> getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T populateNewBean(String[] line)
            throws CsvBeanIntrospectionException, CsvFieldAssignmentException {
        verifyLineLength(line.length);
        T bean = null;

        try {
            if (isRecord || isFullyArgumentConstructor) {
                Object[] values = new Object[line.length];
                Constructor<T> constructor = createBeanConstructor();
                for (int col = 0; col < line.length; col++) {
                    values[col] = getFieldValue(line[col], col);
                }
                bean = constructor.newInstance(values);
            } else {
                bean = createBean();
                for (int col = 0; col < line.length; col++) {
                    setFieldValue(bean, line[col], col);
                }
            }
        } catch (Exception e) {
            throw new CsvBeanIntrospectionException(e.getMessage());
        }

        return bean;
    }

    /**
     * Populates the field corresponding to the column position indicated of the
     * bean passed in according to the rules of the mapping strategy.
     * This method performs conversion on the input string and assigns the
     * result to the proper field in the provided bean.
     *
     * @param value  String containing the value to set the field to.
     * @param column The column position from the CSV file under which this
     *               value was found.
     * @since 4.2
     */
    protected Object getFieldValue(String value, int column) throws CsvConstraintViolationException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        BeanFieldSingleValue<T, String> beanField = (BeanFieldSingleValue<T, String>) findField(column);
        if (beanField != null) {
            return beanField.getFieldValue(beanField.getType(), value);
        }
        return null;
    }

    /**
     * Sets the class type that is being mapped.
     * Also initializes the mapping between column names and bean fields
     * and attempts to create one example bean to be certain there are no
     * fundamental problems with creation.
     */
    // The rest of the Javadoc is inherited.
    @Override
    public void setType(Class<? extends T> type) throws CsvBadConverterException {
        this.type = type;
        this.isRecord = type.isRecord();
        this.isFullyArgumentConstructor = OpencsvUtils.isFullyArgumentConstructor(type);
        loadFieldMap();
    }

    /**
     * Builds a map of columns from the input to fields of the bean type.
     *
     * @throws CsvBadConverterException If there is a problem instantiating the
     *                                  custom converter for an annotated field
     */
    protected void loadFieldMap() throws CsvBadConverterException {

        // Setup
        initializeFieldMap();

        // Populate the field map
        ListValuedMap<Class<?>, Field> partitionedFields = partitionFields();
        loadFieldMap(partitionedFields);
    }

    /**
     * Creates a non-tree (fairly flat) representation of all of the fields
     * bound from all types.
     * This method is used recursively.
     *
     * @param clazz             The root of the type tree at this level of recursion
     * @param encounteredFields A collection of all fields thus far included
     *                          in the new representation. This collection will
     *                          be added to and is the result of this method.
     */
    private void assembleCompleteFieldList(Class<?> clazz, final ListValuedMap<Class<?>, Field> encounteredFields) {
        encounteredFields.putAll(clazz, filterIgnoredFields(clazz, FieldUtils.getAllFields(clazz)));
    }

    /**
     * Filters all fields that opencsv has been instructed to ignore and
     * returns a list of the rest.
     *
     * @param type   The class from which {@code fields} come. This must be the
     *               class as opencsv would seek to instantiate it, which in the
     *               case of inheritance is not necessarily the declaring class.
     * @param fields The fields to be filtered
     * @return A list of fields that exist for opencsv
     */
    protected List<Field> filterIgnoredFields(final Class<?> type, Field[] fields) {
        final List<Field> filteredFields = new LinkedList<>();
        Collections.addAll(filteredFields, fields);
        return filteredFields;
    }


    /**
     * Partitions all non-synthetic fields of the bean type being processed
     * into annotated and non-annotated fields according to
     * {@link #getBindingAnnotations()}.
     *
     * @return A multi-valued map (class to multiple fields in that class) in
     * which all annotated fields are mapped under {@link Boolean#TRUE}, and
     * all non-annotated fields are mapped under {@link Boolean#FALSE}.
     * @since 5.0
     */
    protected ListValuedMap<Class<?>, Field> partitionFields() {
        // Get a flat list of all fields
        ListValuedMap<Class<?>, Field> allFields = new ArrayListValuedHashMap<>();
        assembleCompleteFieldList(type, allFields);

        ListValuedMap<Class<?>, Field> returnValue = new ArrayListValuedHashMap<>();
        allFields.entries().stream()
                .filter(entry -> !entry.getValue().isSynthetic())
                .forEach(entry -> returnValue.put(entry.getKey(), entry.getValue()));
        return returnValue;
    }

    /**
     * Populates the field corresponding to the column position indicated of the
     * bean passed in according to the rules of the mapping strategy.
     * This method performs conversion on the input string and assigns the
     * result to the proper field in the provided bean.
     *
     * @param bean   Object containing the field to be set.
     * @param value  String containing the value to set the field to.
     * @param column The column position from the CSV file under which this
     *               value was found.
     * @throws CsvDataTypeMismatchException    When the result of data conversion returns
     *                                         an object that cannot be assigned to the selected field
     * @throws CsvRequiredFieldEmptyException  When a field is mandatory, but there is no
     *                                         input datum in the CSV file
     * @throws CsvConstraintViolationException When the internal structure of
     *                                         data would be violated by the data in the CSV file
     * @since 4.2
     */
    protected void setFieldValue(T bean, String value, int column)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException,
            CsvConstraintViolationException {
        BeanField<T, K> beanField = findField(column);
        if (beanField != null) {
            beanField.setFieldValue(bean, value, findHeader(column));
        }
    }

    @Override
    public String[] transmuteBean(T bean) throws CsvFieldAssignmentException, CsvChainedException {
        int numColumns = headerIndex.findMaxIndex() + 1;
        BeanField<T, K> firstBeanField, subsequentBeanField;
        K firstIndex, subsequentIndex;
        List<String> contents = new ArrayList<>(Math.max(numColumns, 0));

        // Create a map of types to instances of subordinate beans
        Map<Class<?>, Object> instanceMap;
        try {
            instanceMap = indexBean(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // Our testing indicates these exceptions probably can't be thrown,
            // but they're declared, so we have to deal with them. It's an
            // alibi catch block.
            CsvBeanIntrospectionException csve = new CsvBeanIntrospectionException(
                    "There was an error while manipulating the bean to be written.");
            csve.initCause(e);
            throw csve;
        }

        CsvChainedException chainedException = null;
        for (int i = 0; i < numColumns; ) {

            // Determine the first value
            firstBeanField = findField(i);
            firstIndex = chooseMultivaluedFieldIndexFromHeaderIndex(i);
            String[] fields = ArrayUtils.EMPTY_STRING_ARRAY;
            if (firstBeanField != null) {
                try {
                    fields = firstBeanField.write(instanceMap.get(firstBeanField.getType()), firstIndex);
                } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
                    if (chainedException != null) {
                        chainedException.add(e);
                    } else {
                        chainedException = new CsvChainedException(e);
                    }
                }
            }

            if (fields.length == 0) {

                // Write the only value
                contents.add(StringUtils.EMPTY);
                i++; // Advance the index
            } else {

                // Multiple values. Write the first.
                contents.add(StringUtils.defaultString(fields[0]));

                // Now write the rest.
                // We must make certain that we don't write more fields
                // than we have columns of the correct type to cover them.
                int j = 1;
                int displacedIndex = i + j;
                subsequentBeanField = findField(displacedIndex);
                subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(displacedIndex);
                while (j < fields.length
                        && displacedIndex < numColumns
                        && Objects.equals(firstBeanField, subsequentBeanField)
                        && Objects.equals(firstIndex, subsequentIndex)) {
                    // This field still has a header, so add it
                    contents.add(StringUtils.defaultString(fields[j]));

                    // Prepare for the next loop through
                    displacedIndex = i + (++j);
                    subsequentBeanField = findField(displacedIndex);
                    subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(displacedIndex);
                }

                i = displacedIndex; // Advance the index

                // And here's where we fill in any fields that are missing to
                // cover the number of columns of the same type
                if (i < numColumns) {
                    subsequentBeanField = findField(i);
                    subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(i);
                    while (Objects.equals(firstBeanField, subsequentBeanField)
                            && Objects.equals(firstIndex, subsequentIndex)
                            && i < numColumns) {
                        contents.add(StringUtils.EMPTY);
                        subsequentBeanField = findField(++i);
                        subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(i);
                    }
                }
            }
        }

        // If there were exceptions, throw them
        if (chainedException != null) {
            if (chainedException.hasOnlyOneException()) {
                throw chainedException.getFirstException();
            }
            throw chainedException;
        }

        return contents.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }
}
