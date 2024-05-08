package com.think.table.reader.csv.bean;

import com.think.table.reader.csv.CSVParserBuilder;
import com.think.table.reader.csv.CSVReader;
import com.think.table.reader.csv.ICSVParser;
import com.think.table.reader.csv.enums.CSVReaderNullFieldIndicator;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Reader;

/**
 * Base class for the builders of various incarnations of CSVReaders.
 * @param <T> The type pf the CSVReader class to return
 *
 * @author Andrew Rucker Jones
 * @since 5.5.2
 */
abstract public class CSVReaderBaseBuilder<T> {
    protected final Reader reader;
    private final CSVParserBuilder parserBuilder = new CSVParserBuilder();
    protected int skipLines = CSVReader.DEFAULT_SKIP_LINES;
    protected ICSVParser icsvParser = null;
    protected boolean keepCR;
    protected CSVReaderNullFieldIndicator nullFieldIndicator = CSVReaderNullFieldIndicator.NEITHER;
    protected int multilineLimit = CSVReader.DEFAULT_MULTILINE_LIMIT;

    /**
     * Base Constructor
     *
     * @param reader The reader to an underlying CSV source.
     */
    protected CSVReaderBaseBuilder(final Reader reader) {
        this.reader = reader;
    }

    /**
     * Used by unit tests.
     *
     * @return The reader.
     */
    protected Reader getReader() {
        return reader;
    }

    /**
     * Used by unit tests.
     *
     * @return The set number of lines to skip
     */
    protected int getSkipLines() {
        return skipLines;
    }

    /**
     * Used by unit tests.
     *
     * @return The CSVParser used by the builder.
     */
    protected ICSVParser getCsvParser() {
        return icsvParser;
    }

    /**
     * Used by unit tests.
     *
     * @return The upper limit on lines in multiline records.
     */
    protected int getMultilineLimit() {
        return multilineLimit;
    }

    /**
     * Returns if the reader built will keep or discard carriage returns.
     *
     * @return {@code true} if the reader built will keep carriage returns,
     * {@code false} otherwise
     */
    protected boolean keepCarriageReturn() {
        return this.keepCR;
    }

    /**
     * Creates a new {@link ICSVParser} if the class doesn't already hold one.
     *
     * @return The injected {@link ICSVParser} or a default parser.
     */
    protected ICSVParser getOrCreateCsvParser() {
        return ObjectUtils.defaultIfNull(icsvParser,
                parserBuilder
                        .withFieldAsNull(nullFieldIndicator)
                        .build());
    }
    /**
     * Must create the CSVReader type requested.
     * @return A new instance of {@link CSVReader} or derived class
     */
    public abstract T build();
}
