package com.think.table.reader.csv.bean;

import com.think.table.reader.csv.CSVReader;
import com.think.table.reader.csv.exceptions.CsvBadConverterException;
import com.think.table.reader.csv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class serves as a location to collect code common to a mapping strategy
 * that maps header names to member variables.
 *
 * @param <T> The type of bean being created or written
 * @author Andrew Rucker Jones
 * @since 5.0
 */
abstract public class HeaderNameBaseMappingStrategy<T> extends AbstractMappingStrategy<String, String, ComplexFieldMapEntry<String, String, T>, T> {
    /**
     * Given a header name, this map allows one to find the corresponding
     * {@link BeanField}.
     */
    protected FieldMapByName<T> fieldMap = null;

    /**
     * Holds a {@link Comparator} to sort columns on writing.
     */
    protected Comparator<String> writeOrder = null;

    /**
     * If set, every record will be shortened or lengthened to match the number of headers.
     */
    protected final boolean forceCorrectRecordLength;

    /**
     * Nullary constructor for compatibility.
     */
    public HeaderNameBaseMappingStrategy(ConversionService conversionService) {
        super(conversionService);
        this.forceCorrectRecordLength = false;
    }

    /**
     * Constructor to allow setting options for header name mapping.
     *
     * @param forceCorrectRecordLength If set, every record will be shortened
     *                                 or lengthened to match the number of
     *                                 headers
     */
    public HeaderNameBaseMappingStrategy(boolean forceCorrectRecordLength, ConversionService conversionService) {
        super(conversionService);
        this.forceCorrectRecordLength = forceCorrectRecordLength;
    }

    @Override
    public void captureHeader(CSVReader reader) throws IOException, CsvRequiredFieldEmptyException {
        // Validation
        if (type == null) {
            throw new IllegalStateException("The type has not been set in the MappingStrategy.");
        }

        // Read the header
        String[] header = ArrayUtils.nullToEmpty(reader.readNextSilently());
        for (int i = 0; i < header.length; i++) {
            // For the case that a header is empty and someone configured
            // empty fields to be null
            if (header[i] == null) {
                header[i] = StringUtils.EMPTY;
            }
        }
        headerIndex.initializeHeaderIndex(header);

        // Throw an exception if any required headers are missing
        List<FieldMapByNameEntry<T>> missingRequiredHeaders = fieldMap.determineMissingRequiredHeaders(header);
        if (!missingRequiredHeaders.isEmpty()) {
            String[] requiredHeaderNames = new String[missingRequiredHeaders.size()];
            List<Field> requiredFields = new ArrayList<>(missingRequiredHeaders.size());
            for (int i = 0; i < missingRequiredHeaders.size(); i++) {
                FieldMapByNameEntry<T> fme = missingRequiredHeaders.get(i);
                if (fme.isRegexPattern()) {
                    requiredHeaderNames[i] = String.format("Matches [%s]", fme.getName());
                } else {
                    requiredHeaderNames[i] = fme.getName();
                }
                requiredFields.add(fme.getField().getField());
            }
            String missingRequiredFields = String.join(", ", requiredHeaderNames);
            String allHeaders = String.join(",", header);
            CsvRequiredFieldEmptyException e = new CsvRequiredFieldEmptyException(type, requiredFields,
                    String.format("Header is missing required fields [%s]. The list of headers encountered is [%s].",
                            missingRequiredFields, allHeaders));
            e.setLine(header);
            throw e;
        }
    }

    @Override
    protected String chooseMultivaluedFieldIndexFromHeaderIndex(int index) {
        String[] s = headerIndex.getHeaderIndex();
        return index >= s.length ? null : s[index];
    }

    @Override
    public void verifyLineLength(int numberOfFields) throws CsvRequiredFieldEmptyException {
        if (!headerIndex.isEmpty()) {
            if (numberOfFields != headerIndex.getHeaderIndexLength() && !forceCorrectRecordLength) {
                throw new CsvRequiredFieldEmptyException(type, "Number of data fields does not match number of headers.");
            }
        }
    }

    @Override
    protected BeanField<T, String> findField(int col) throws CsvBadConverterException {
        BeanField<T, String> beanField = null;
        String columnName = getColumnName(col);
        if (columnName == null) {
            return null;
        }
        columnName = columnName.trim();
        if (!columnName.isEmpty()) {
            beanField = fieldMap.get(columnName.toUpperCase());
        }
        return beanField;
    }

    /**
     * Creates a map of fields in the bean to be processed that have no binding
     * annotations.
     * <p>This method is called by {@link #loadFieldMap()} when absolutely no
     * binding annotations that are relevant for this mapping strategy are
     * found in the type of bean being processed. It is then assumed that every
     * field is to be included, and that the name of the member variable must
     * exactly match the header name of the input.</p>
     * <p>Two exceptions are made to the rule that everything is written:<ol>
     * <li>Any field named "serialVersionUID" will be ignored if the
     * enclosing class implements {@link Serializable}.</li>
     * </ol></p>
     */
    @Override
    protected void loadFieldMap(ListValuedMap<Class<?>, Field> fields) {
        fields.entries().stream()
                .filter(entry -> !(Serializable.class.isAssignableFrom(entry.getKey()) && "serialVersionUID".equals(entry.getValue().getName())))
                .forEach(entry -> {
                    Field field = entry.getValue();
                    BeanFieldSingleValue<T, String> beanField = new BeanFieldSingleValue<>(entry.getKey(), field, false, conversionService);
                    fieldMap.put(field.getName().toUpperCase(), beanField);
                });
    }

    @Override
    protected void initializeFieldMap() {
        fieldMap = new FieldMapByName<>();
        fieldMap.setColumnOrderOnWrite(writeOrder);
    }

    @Override
    protected FieldMap<String, String, ? extends ComplexFieldMapEntry<String, String, T>, T> getFieldMap() {
        return fieldMap;
    }

    @Override
    public String findHeader(int col) {
        return headerIndex.getByPosition(col);
    }

    /**
     * Sets the {@link Comparator} to be used to sort columns when
     * writing beans to a CSV file.
     * Behavior of this method when used on a mapping strategy intended for
     * reading data from a CSV source is not defined.
     *
     * @param writeOrder The {@link Comparator} to use. May be
     *                   {@code null}, in which case the natural ordering is used.
     * @since 4.3
     */
    public void setColumnOrderOnWrite(Comparator<String> writeOrder) {
        this.writeOrder = writeOrder;
        if (fieldMap != null) {
            fieldMap.setColumnOrderOnWrite(this.writeOrder);
        }
    }
}
