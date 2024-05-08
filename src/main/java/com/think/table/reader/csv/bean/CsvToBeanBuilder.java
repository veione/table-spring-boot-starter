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

import com.think.table.reader.csv.CSVParser;
import com.think.table.reader.csv.CSVParserBuilder;
import com.think.table.reader.csv.CSVReader;
import com.think.table.reader.csv.CSVReaderBuilder;
import com.think.table.reader.csv.bean.exceptionhandler.CsvExceptionHandler;
import com.think.table.reader.csv.bean.exceptionhandler.ExceptionHandlerThrow;
import com.think.table.reader.csv.enums.CSVReaderNullFieldIndicator;
import org.springframework.core.convert.ConversionService;

import java.io.Reader;
import java.util.Locale;

/**
 * This class makes it possible to bypass all the intermediate steps and classes
 * in setting up to read from a CSV source to a list of beans.
 * <p>If you want nothing but defaults for the entire import, your code can look
 * as simple as this, where {@code myreader} is any valid {@link Reader Reader}:<br>
 * {@code List<MyBean> result = new CsvToBeanBuilder(myreader).withType(MyBean.class).build().parse();}</p>
 * <p>This builder is intelligent enough to guess the mapping strategy according to the
 * following strategy:</p><ol>
 * <li>If a mapping strategy is explicitly set, it is always used.</li>
 * </ol>
 *
 * @param <T> Type of the bean to be populated
 * @author Andrew Rucker Jones
 * @since 3.9
 */
public class CsvToBeanBuilder<T> {

    private MappingStrategy<? extends T> mappingStrategy = null;

    /**
     * A CSVReader will be built out of this {@link Reader}.
     *
     * @see CsvToBean#csvReader
     */
    private final Reader reader;

    /**
     * Allow the user to pass in a prebuilt/custom {@link CSVReader}.
     */
    private final CSVReader csvReader;

    /**
     * @see CsvToBean#throwExceptions
     */
    private CsvExceptionHandler exceptionHandler = null;

    /**
     * @see com.think.table.reader.csv.CSVParser#nullFieldIndicator
     */
    private CSVReaderNullFieldIndicator nullFieldIndicator = null;

    /**
     * @see CSVReader#keepCR
     */
    private boolean keepCR;

    /**
     * @see CSVReader#skipLines
     */
    private Integer skipLines = null;

    /**
     * @see com.think.table.reader.csv.CSVParser#separator
     */
    private Character separator = null;

    /**
     * @see com.think.table.reader.csv.CSVParser#quotechar
     */
    private Character quoteChar = null;

    /**
     * @see com.think.table.reader.csv.CSVParser#escape
     */
    private Character escapeChar = null;

    /**
     * @see com.think.table.reader.csv.CSVParser#strictQuotes
     */
    private Boolean strictQuotes = null;

    /**
     * @see com.think.table.reader.csv.CSVParser#ignoreLeadingWhiteSpace
     */
    private Boolean ignoreLeadingWhiteSpace = null;

    /**
     * @see com.think.table.reader.csv.CSVParser#ignoreQuotations
     */
    private Boolean ignoreQuotations = null;

    /**
     * @see com.think.table.reader.csv.bean.CsvToBean#setThrowExceptions(boolean)
     */
    private Boolean throwsExceptions = true;

    /**
     * @see HeaderColumnNameMappingStrategy#type
     */
    private Class<? extends T> type = null;

    /**
     * @see CSVReader#multilineLimit
     */
    private Integer multilineLimit = null;

    /**
     * @see com.think.table.reader.csv.bean.CsvToBean#orderedResults
     */
    private boolean orderedResults = true;

    /**
     * @see com.think.csv.bean.CsvToBean#ignoreEmptyLines
     */
    private boolean ignoreEmptyLines = false;

    private ConversionService conversionService;

    /**
     * Constructor with the one parameter that is most definitely mandatory, and
     * always will be.
     *
     * @param reader The reader that is the source of data for the CSV import
     */
    public CsvToBeanBuilder(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("The Reader must always be non-null.");
        }
        this.reader = reader;
        this.csvReader = null;
    }

    /**
     * Constructor with the one parameter that is most definitely mandatory, and
     * always will be.
     *
     * @param csvReader The CSVReader that is the source of data for the CSV import
     */
    public CsvToBeanBuilder(CSVReader csvReader) {
        if (csvReader == null) {
            throw new IllegalArgumentException("The Reader must always be non-null.");
        }
        this.reader = null;
        this.csvReader = csvReader;
    }

    /**
     * Builds the {@link CsvToBean} out of the provided information.
     *
     * @return A valid {@link CsvToBean}
     * @throws IllegalStateException If a necessary parameter was not specified.
     *                               Currently this means that both the mapping strategy and the bean type
     *                               are not set, so it is impossible to determine a mapping strategy.
     */
    public CsvToBean<T> build() throws IllegalStateException {
        // Check for errors in the configuration first
        if (mappingStrategy == null && type == null) {
            throw new IllegalStateException("Either a mapping strategy or the type of the bean to be populated must be specified.");
        }

        // Build Parser and Reader
        CsvToBean<T> bean = new CsvToBean<>();

        if (csvReader != null) {
            bean.setCsvReader(csvReader);
        } else {
            CSVParser parser = buildParser();
            bean.setCsvReader(buildReader(parser));
        }

        // Set variables in CsvToBean itself

        if (exceptionHandler != null) {
            bean.setExceptionHandler(exceptionHandler);
        } else {
            bean.setThrowExceptions(throwsExceptions);
        }

        // Now find the mapping strategy and ignore irrelevant fields.
        // It's possible the mapping strategy has already been primed, so only
        // pass on our data if the user actually gave us something.
        if (mappingStrategy == null) {
            if (conversionService == null) {
                throw new IllegalArgumentException("ConversionService must be specified");
            }
            mappingStrategy = new HeaderColumnNameMappingStrategy<>(conversionService);
        }
        bean.setMappingStrategy(mappingStrategy);
        bean.setIgnoreEmptyLines(ignoreEmptyLines);

        return bean;
    }

    /**
     * Builds a {@link CSVParser} from the information provided to this builder.
     * This is an intermediate step in building the {@link CsvToBean}.
     *
     * @return An appropriate {@link CSVParser}
     */
    private CSVParser buildParser() {
        CSVParserBuilder csvpb = new CSVParserBuilder();
        if (nullFieldIndicator != null) {
            csvpb.withFieldAsNull(nullFieldIndicator);
        }
        if (separator != null) {
            csvpb.withSeparator(separator);
        }
        if (quoteChar != null) {
            csvpb.withQuoteChar(quoteChar);
        }
        if (escapeChar != null) {
            csvpb.withEscapeChar(escapeChar);
        }
        if (strictQuotes != null) {
            csvpb.withStrictQuotes(strictQuotes);
        }
        if (ignoreLeadingWhiteSpace != null) {
            csvpb.withIgnoreLeadingWhiteSpace(ignoreLeadingWhiteSpace);
        }
        if (ignoreQuotations != null) {
            csvpb.withIgnoreQuotations(ignoreQuotations);
        }

        return csvpb.build();
    }

    /**
     * Builds a {@link CSVReader} from the information provided to this builder.
     * This is an intermediate step in building the {@link CsvToBean}.
     *
     * @param parser The {@link CSVParser} necessary for this reader
     * @return An appropriate {@link CSVReader}
     */
    private CSVReader buildReader(CSVParser parser) {
        CSVReaderBuilder csvrb = new CSVReaderBuilder(reader);
        csvrb.withCSVParser(parser);
        csvrb.withKeepCarriageReturn(keepCR);
        if (skipLines != null) {
            csvrb.withSkipLines(skipLines);
        }
        if (multilineLimit != null) {
            csvrb.withMultilineLimit(multilineLimit);
        }
        return csvrb.build();
    }

    /**
     * @param mappingStrategy Please see the "See Also" section
     * @return {@code this}
     * @see CsvToBean#setMappingStrategy(com.opencsv.bean.MappingStrategy)
     */
    public CsvToBeanBuilder<T> withMappingStrategy(MappingStrategy<? extends T> mappingStrategy) {
        this.mappingStrategy = mappingStrategy;
        return this;
    }

    /**
     * Sets how the CsvToBean will act when an exception occurs.   If both withThrowsExcpetion and
     * {@link #withExceptionHandler(CsvExceptionHandler)} are used then the withExceptionHandler takes
     * precedence and is used.
     *
     * @param throwExceptions Please see the "See Also" section
     * @return {@code this}
     * @see CsvToBean#setThrowExceptions(boolean)
     * @see #withExceptionHandler(CsvExceptionHandler)
     */
    public CsvToBeanBuilder<T> withThrowExceptions(boolean throwExceptions) {
        this.throwsExceptions = throwExceptions;
        return this;
    }

    /**
     * Sets the handler for recoverable exceptions raised during processing of
     * records. If both {@link #withThrowExceptions(boolean)} and withExceptionHandler are used then the
     * withExceptionHandler takes precedence and is used.
     * <p>If neither this method nor {@link #withThrowExceptions(boolean)} is
     * called, the default exception handler is
     * {@link ExceptionHandlerThrow}.</p>
     * <p>Please note that if both this method and
     * {@link #withThrowExceptions(boolean)} are called, the last call wins.</p>
     *
     * @param exceptionHandler The exception handler to be used. If {@code null},
     *                         this method does nothing.
     * @return {@code this}
     * @since 5.2
     */
    public CsvToBeanBuilder<T> withExceptionHandler(CsvExceptionHandler exceptionHandler) {
        if (exceptionHandler != null) {
            this.exceptionHandler = exceptionHandler;
        }
        return this;
    }

    /**
     * @param indicator Which field content will be returned as null: EMPTY_SEPARATORS, EMPTY_QUOTES,
     *                  BOTH, NEITHER (default)
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withFieldAsNull(CSVReaderNullFieldIndicator indicator) {
        this.nullFieldIndicator = indicator;
        return this;
    }

    /**
     * @param keepCR True to keep carriage returns in data read, false otherwise
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withKeepCarriageReturn(boolean keepCR) {
        this.keepCR = keepCR;
        return this;
    }

    public CsvToBeanBuilder<T> withConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
        return this;
    }

    /**
     * @param skipLines Please see the "See Also" section
     * @return {@code this}
     * @see CSVReaderBuilder#withSkipLines(int)
     */
    public CsvToBeanBuilder<T> withSkipLines(
            final int skipLines) {
        this.skipLines = skipLines;
        return this;
    }

    /**
     * @param separator Please see the "See Also" section
     * @return {@code this}
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     */
    public CsvToBeanBuilder<T> withSeparator(char separator) {
        this.separator = separator;
        return this;
    }

    /**
     * @param quoteChar Please see the "See Also" section
     * @return {@code this}
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     */
    public CsvToBeanBuilder<T> withQuoteChar(char quoteChar) {
        this.quoteChar = quoteChar;
        return this;
    }

    /**
     * @param escapeChar Please see the "See Also" section
     * @return {@code this}
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     */
    public CsvToBeanBuilder<T> withEscapeChar(char escapeChar) {
        this.escapeChar = escapeChar;
        return this;
    }

    /**
     * @param strictQuotes Please see the "See Also" section
     * @return {@code this}
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     */
    public CsvToBeanBuilder<T> withStrictQuotes(boolean strictQuotes) {
        this.strictQuotes = strictQuotes;
        return this;
    }

    /**
     * @param ignoreLeadingWhiteSpace Please see the "See Also" section
     * @return {@code this}
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     */
    public CsvToBeanBuilder<T> withIgnoreLeadingWhiteSpace(boolean ignoreLeadingWhiteSpace) {
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
        return this;
    }

    /**
     * @param ignoreQuotations Please see the "See Also" section
     * @return {@code this}
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     */
    public CsvToBeanBuilder<T> withIgnoreQuotations(boolean ignoreQuotations) {
        this.ignoreQuotations = ignoreQuotations;
        return this;
    }

    /**
     * Sets the type of the bean to be populated.
     * Ignored if {@link #withMappingStrategy(com.opencsv.bean.MappingStrategy)}
     * is called.
     *
     * @param type Class of the destination bean
     * @return {@code this}
     * @see HeaderColumnNameMappingStrategy#setType(Class)
     * @see ColumnPositionMappingStrategy#setType(Class)
     */
    public CsvToBeanBuilder<T> withType(Class<? extends T> type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the maximum number of lines allowed in a multiline record.
     * More than this number in one record results in an IOException.
     *
     * @param multilineLimit No more than this number of lines is allowed in a
     *                       single input record. The default is {@link CSVReader#DEFAULT_MULTILINE_LIMIT}.
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withMultilineLimit(int multilineLimit) {
        this.multilineLimit = multilineLimit;
        return this;
    }

    /**
     * Sets whether the resulting beans must be ordered as in the input.
     *
     * @param orderedResults Whether to order the results or not
     * @return {@code this}
     * @see CsvToBean#setOrderedResults(boolean)
     * @since 4.0
     */
    public CsvToBeanBuilder<T> withOrderedResults(boolean orderedResults) {
        this.orderedResults = orderedResults;
        return this;
    }

    /**
     * @param ignore Please see the "See Also" section
     * @return {@code this}
     * @see CsvToBean#ignoreEmptyLines
     */
    public CsvToBeanBuilder<T> withIgnoreEmptyLine(boolean ignore) {
        this.ignoreEmptyLines = ignore;
        return this;
    }
}
