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

import com.think.table.reader.csv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class maintains a mapping from header names out of a CSV file to bean
 * fields.
 * Simple entries are matched using string equality. Complex entries are matched
 * using regular expressions.
 *
 * @param <T> Type of the bean being converted
 * @author Andrew Rucker Jones
 * @since 4.2
 */
public class FieldMapByName<T> extends AbstractFieldMap<String, String, RegexToBeanField<T>, T> {

    /** Holds a {@link Comparator} to sort columns on writing. */
    private Comparator<String> writeOrder = null;

    /**
     * @param key A regular expression matching header names
     */
    // The rest of the Javadoc is inherited
    @Override
    public void putComplex(final String key, final BeanField<T, String> value) {
        complexMapList.add(new RegexToBeanField<>(key, value));
    }

    /**
     * Returns a list of required headers that were not present in the input.
     *
     * @param headersPresent An array of all headers present from the input
     * @return A list of name + field for all of the required headers that were
     *   not found
     */
    public List<FieldMapByNameEntry<T>> determineMissingRequiredHeaders(final String[] headersPresent) {

        // Start with collections of all required headers
        final List<String> requiredStringList = simpleMap.entrySet().stream()
                .filter(e -> e.getValue().isRequired())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedList::new));
        final List<ComplexFieldMapEntry<String, String, T>> requiredRegexList = complexMapList.stream()
                .filter(r -> r.getBeanField().isRequired())
                .collect(Collectors.toList());

        // Now remove the ones we found
        for(String h : headersPresent) {
            if(!requiredStringList.remove(h.toUpperCase())) {
                final ListIterator<ComplexFieldMapEntry<String, String, T>> requiredRegexListIterator = requiredRegexList.listIterator();
                boolean found = false;
                while(!found && requiredRegexListIterator.hasNext()) {
                    final ComplexFieldMapEntry<String, String, T> r = requiredRegexListIterator.next();
                    if(r.contains(h)) {
                        found = true;
                        requiredRegexListIterator.remove();
                    }
                }
            }
        }

        // Repackage what remains
        List<FieldMapByNameEntry<T>> missingRequiredHeaders = new LinkedList<>();
        for(String s : requiredStringList) {
            missingRequiredHeaders.add(new FieldMapByNameEntry<T>(s, simpleMap.get(s), false));
        }
        for(ComplexFieldMapEntry<String, String, T> r : requiredRegexList) {
            missingRequiredHeaders.add(new FieldMapByNameEntry<T>(r.getInitializer(), r.getBeanField(), true));
        }

        return missingRequiredHeaders;
    }

    /**
     * This method generates a header that can be used for writing beans of the
     * type provided back to a file.
     * <p>The ordering of the headers is determined by the
     * {@link Comparator} passed in to
     * {@link #setColumnOrderOnWrite(Comparator)}, should that method be called,
     * otherwise the natural ordering is used (alphabetically ascending).</p>
     * <p>This implementation will not write headers discovered in multi-valued
     * bean fields if the headers would not be matched by the bean field on
     * reading. There are two reasons for this:</p>
     * <ol><li>opencsv always tries to create data that are round-trip
     * equivalent, and that would not be the case if it generated data on
     * writing that it would discard on reading.</li>
     * <li>As the code is currently written, the header name is used on writing
     * each bean field to determine the appropriate {@link BeanField} for
     * information concerning conversions, locales, necessity (whether or not
     * the field is required). Without this information, conversion is
     * impossible, and every value written under the unmatched header is blank,
     * regardless of the contents of the bean.</li></ol>
     */
    // The rest of the Javadoc is inherited.
    @Override
    public String[] generateHeader(final T bean) throws CsvRequiredFieldEmptyException {
        final List<Field> missingRequiredHeaders = new LinkedList<>();
        final List<String> headerList = new ArrayList<>(simpleMap.keySet());
        for(ComplexFieldMapEntry<String, String, T> r : complexMapList) {
            @SuppressWarnings("unchecked")
            final MultiValuedMap<String,T> m = (MultiValuedMap<String,T>) r.getBeanField().getFieldValue(bean);
            if(m != null && !m.isEmpty()) {
                headerList.addAll(m.entries().stream()
                        .map(Map.Entry::getKey)
                        .filter(r::contains)
                        .collect(Collectors.toList()));
            }
            else {
                if(r.getBeanField().isRequired()) {
                    missingRequiredHeaders.add(r.getBeanField().getField());
                }
            }
        }

        // Report headers that should have been present
        if(!missingRequiredHeaders.isEmpty()) {
            String errorMessage = String.format(
                    "Header is missing required fields [%s]. The list of headers encountered is [%s].",
                    missingRequiredHeaders.stream()
                            .map(Field::getName)
                            .collect(Collectors.joining(" ")),
                    String.join(" ", headerList));
            throw new CsvRequiredFieldEmptyException(bean.getClass(), missingRequiredHeaders, errorMessage);
        }

        // Sort and return
        headerList.sort(writeOrder);
        return headerList.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Sets the {@link Comparator} to be used to sort columns when
     * writing beans to a CSV file.
     *
     * @param writeOrder The {@link Comparator} to use. May be
     *   {@code null}, in which case the natural ordering is used.
     * @since 4.3
     */
    public void setColumnOrderOnWrite(Comparator<String> writeOrder) {
        this.writeOrder = writeOrder;
    }
}
