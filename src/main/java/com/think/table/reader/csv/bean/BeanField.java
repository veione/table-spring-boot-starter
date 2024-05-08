package com.think.table.reader.csv.bean;

import com.think.table.reader.csv.exceptions.CsvConstraintViolationException;
import com.think.table.reader.csv.exceptions.CsvDataTypeMismatchException;
import com.think.table.reader.csv.exceptions.CsvRequiredFieldEmptyException;

import java.lang.reflect.Field;

/**
 * Used to extend the {@link Field} class to include
 * functionality that opencsv requires.
 * <p>This includes a required flag and a
 * {@link #write(Object, Object) } method for writing beans
 * back out to a CSV file. The required flag determines if the field has to be
 * non-empty.</p>
 * <p><b><i>Synchronization:</i></b> All implementations of this interface must
 * be thread-safe.</p>
 *
 * @param <T> Type of the bean being populated
 * @param <I> Type of the index into a multivalued field
 */
public interface BeanField<T, I> {

    /**
     * Gets the type of the bean this field is attached to.
     * This is necessary because the declaring class as given by the field
     * itself may be a superclass of the class that is instantiated during bean
     * population.
     *
     * @return The type of the bean this field is attached to
     * @since 5.0
     */
    Class<?> getType();

    /**
     * Sets the type of the bean this field is attached to.
     *
     * @param type The type that is instantiated when this field is used
     * @since 5.0
     */
    void setType(Class<?> type);

    /**
     * Sets the field to be processed.
     *
     * @param field Which field is being populated
     */
    void setField(Field field);

    /**
     * Gets the field to be processed.
     *
     * @return A field object
     * @see Field
     */
    Field getField();

    /**
     * Answers the query, whether this field is required or not.
     *
     * @return True if the field is required to be set (cannot be null or an
     * empty string), false otherwise
     * @since 3.10
     */
    boolean isRequired();

    /**
     * Determines whether or not a field is required.
     * Implementation note: This method is necessary for custom converters. If
     * we did not have it, every custom converter would be required to implement
     * a constructor with this one boolean parameter, and the instantiation code
     * for the custom converter would look much uglier.
     *
     * @param required Whether or not the field is required
     * @since 3.10
     */
    void setRequired(boolean required);

    /**
     * Populates the selected field of the bean.
     * This method performs conversion on the input string and assigns the
     * result to the proper field in the provided bean.
     *
     * @param bean   Object containing the field to be set.
     * @param value  String containing the value to set the field to.
     * @param header The header from the CSV file under which this value was found.
     * @throws CsvDataTypeMismatchException   When the result of data conversion returns
     *                                        an object that cannot be assigned to the selected field
     * @throws CsvRequiredFieldEmptyException When a field is mandatory, but there is no
     *                                        input datum in the CSV file
     */
    void setFieldValue(Object bean, String value, String header)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException,
            CsvConstraintViolationException;

    /**
     * Gets the contents of the selected field of the given bean.
     * This method performs no conversions of any kind, but simply gets the
     * value of the desired field using an accessor method if one is
     * available and reflection if one is not.
     *
     * @param bean Object containing the field to be read
     * @return The value of the field in the given bean
     * @since 4.2
     */
    Object getFieldValue(Object bean);

    /**
     * Given the value of a bean field and an index into that value, determine
     * what values need to be written.
     * When writing a bean to a CSV file, some single fields from the bean could
     * have values that need to be split into multiple fields when writing them
     * to the CSV file. Given the value of the bean field and an index into the
     * data, this method returns the objects to be converted and written.
     *
     * @param value The value of the bean field that should be written
     * @param index An index into {@code value} that determines which of the
     *              many possible values are currently being written. For header-based
     *              mapping strategies, this will be the header name, and for column
     *              position mapping strategies, it will be the zero-based column position.
     * @return An array of {@link Object}s that should be converted for the
     * output and written
     * @throws CsvDataTypeMismatchException If {@code value} is not of the type
     *                                      expected by the implementing class
     * @since 4.2
     */
    Object[] indexAndSplitMultivaluedField(Object value, I index)
            throws CsvDataTypeMismatchException;

    /**
     * This method takes the current value of the field in question in the bean
     * passed in and converts it to one or more strings.
     * This method is used to write beans back out to a CSV file, and should
     * ideally provide an accurate representation of the field such that it is
     * round trip equivalent. That is to say, this method should write data out
     * just as it would expect to read the data in.
     *
     * @param bean  The bean holding the field to be written
     * @param index The header name or column number of the field currently
     *              being processed. This can be used to find a certain position in a
     *              multivalued field when not all of the values should be written.
     * @return An array of string representations for the values of this field
     * out of the bean passed in. Typically, there will be only one value, but
     * {@link com.think.csv.bean.BeanFieldJoin} may return multiple values.
     * If either the bean or the field are {@code null}, this method returns
     * an empty array to allow the writer to treat {@code null} specially. It
     * is also possible that individual values in the array are {@code null}.
     * The writer may wish to write "(null)" or "\0" or "NULL" or some other
     * key instead of a blank string.
     * @throws CsvDataTypeMismatchException   If expected to convert an
     *                                        unsupported data type
     * @throws CsvRequiredFieldEmptyException If the field is marked as required,
     *                                        but is currently empty
     * @since 3.9
     */
    String[] write(Object bean, I index) throws CsvDataTypeMismatchException,
            CsvRequiredFieldEmptyException;
}
