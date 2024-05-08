package com.think.table.reader.csv.bean;

/*
 Copyright 2007 Kyle Miller.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import com.think.table.reader.csv.CSVReader;
import com.think.table.reader.csv.exceptions.CsvBadConverterException;
import com.think.table.reader.csv.exceptions.CsvBeanIntrospectionException;
import com.think.table.reader.csv.exceptions.CsvChainedException;
import com.think.table.reader.csv.exceptions.CsvFieldAssignmentException;
import com.think.table.reader.csv.exceptions.CsvRequiredFieldEmptyException;

import java.io.IOException;
import java.util.Locale;

/**
 * The interface for the classes that handle translating between the columns in
 * the CSV file to an actual object.
 * <p>Any implementing class <em>must</em> be thread-safe. Specifically, the
 * following methods must be thread-safe:</p>
 * <ul><li>{@link #populateNewBean(String[])}</li>
 * <li>{@link #transmuteBean(Object)}</li></ul>
 *
 * @param <T> Type of object you are converting the data to.
 */
public interface MappingStrategy<T> {

    /**
     * Implementation of this method can grab the header line before parsing
     * begins to use to map columns to bean properties.
     *
     * @param reader The CSVReader to use for header parsing
     * @throws IOException If parsing fails
     * @throws CsvRequiredFieldEmptyException If a field is required, but the
     *   header or column position for the field is not present in the input
     */
    void captureHeader(CSVReader reader) throws IOException, CsvRequiredFieldEmptyException;

    /**
     * Implementations of this method must return an array of column headers
     * based on the contents of the mapping strategy.
     * If no header can or should be generated, an array of zero length must
     * be returned, and not {@code null}.
     * @param bean One fully populated bean from which the header can be derived.
     *   This is important in the face of joining and splitting. If we have a
     *   MultiValuedMap as a field that is the target for a join on reading, that
     *   same field must be split into multiple columns on writing. Since the
     *   joining is done via regular expressions, it is impossible for opencsv
     *   to know what the column names are supposed to be on writing unless this
     *   bean includes a fully populated map.
     * @return An array of column names for a header. This may be an empty array
     *   if no header should be written, but it must not be {@code null}.
     * @throws CsvRequiredFieldEmptyException If a required header is missing
     *   while attempting to write. Since every other header is hard-wired
     *   through the bean fields and their associated annotations, this can only
     *   happen with multi-valued fields.
     * @since 3.9
     */
    String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException;

    /**
     * Takes a line of input from a CSV file and creates a bean out of it.
     *
     * @param line A line of input returned from {@link com.think.csv.CSVReader}
     * @return A bean containing the converted information from the input
     * @throws CsvBeanIntrospectionException Generally, if some part of the bean cannot
     *   be accessed and used as needed
     * @throws CsvFieldAssignmentException A more specific subclass of this
     *   exception is thrown for any problem decoding and assigning a field
     *   of the input to a bean field
     * @throws CsvChainedException If multiple exceptions are thrown for the
     * same input line
     * @since 4.2
     */
    T populateNewBean(String[] line)
            throws CsvBeanIntrospectionException, CsvFieldAssignmentException,
            CsvChainedException;

    /**
     * Sets the class type that is being mapped.
     * May perform additional initialization tasks. If instantiating a
     * mapping strategy, this method should be called after any other
     * initialization, for example a call to
     * {@link #setErrorLocale(Locale)} or {@link #setProfile(String)}.
     *
     * @param type Class type.
     * @throws CsvBadConverterException If a field in the bean is annotated
     *   with a custom converter that cannot be initialized. If you are not
     *   using custom converters that you have written yourself, it should be
     *   safe to catch this exception and ignore it.
     */
    void setType(Class<? extends T> type) throws CsvBadConverterException;

    /**
     * Transmutes a bean instance into an array of {@link String}s to be written
     * to a CSV file.
     *
     * @param bean The bean to be transmuted
     * @return The converted values of the bean fields in the correct order,
     *   ready to be passed to a {@link com.opencsv.CSVWriter}
     * @throws CsvFieldAssignmentException A more specific subclass of this
     *   exception is thrown for any problem decoding and assigning a field
     *   of the input to a bean field
     * @throws CsvChainedException If multiple exceptions are thrown for the
     * same input line
     */
    String[] transmuteBean(T bean) throws CsvFieldAssignmentException, CsvChainedException;
}