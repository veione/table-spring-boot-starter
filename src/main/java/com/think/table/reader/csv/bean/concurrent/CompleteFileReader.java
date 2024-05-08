package com.think.table.reader.csv.bean.concurrent;

import com.think.table.reader.csv.CSVReader;
import com.think.table.reader.csv.bean.MappingStrategy;
import com.think.table.reader.csv.bean.exceptionhandler.CsvExceptionHandler;

/**
 * Implements a separate thread for reading input and siphoning it to a
 * {@link LineExecutor}.
 *
 * @param <T> The type of bean being created
 * @author Andrew Rucker Jones
 * @since 5.2
 */
public class CompleteFileReader<T> extends SingleLineReader {

    /**
     * The mapping strategy in use.
     */
    private final MappingStrategy<? extends T> mappingStrategy;

    /**
     * Whether exceptions in processing should be thrown or collected.
     */
    private final CsvExceptionHandler exceptionHandler;

    /**
     * Counts how many records have been read from the input.
     */
    private long lineProcessed;

    /**
     * The exception that brought execution to a grinding halt.
     */
    private Throwable terminalException;

    /**
     * The executor that takes lines of input and converts them to beans.
     */
    private LineExecutor<T> executor;

    /**
     * @param csvReader        The {@link CSVReader} from which input is read
     * @param ignoreEmptyLines Whether empty lines of input should be ignored
     * @param mappingStrategy  The mapping strategy in use
     * @param exceptionHandler Determines the exception handling behavior
     */
    public CompleteFileReader(CSVReader csvReader,
                              boolean ignoreEmptyLines,
                              MappingStrategy<? extends T> mappingStrategy,
                              CsvExceptionHandler exceptionHandler) {
        super(csvReader, ignoreEmptyLines);
        this.mappingStrategy = mappingStrategy;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * @return The exception that brought execution to a halt
     */
    public Throwable getTerminalException() {
        return terminalException;
    }

    /**
     * @return How many lines have been processed thus far
     */
    public long getLineProcessed() {
        return lineProcessed;
    }

    /**
     * Sets the executor that will convert text input to bean output.
     *
     * @param executor The executor to use
     */
    public void setExecutor(LineExecutor<T> executor) {
        if (this.executor == null) {
            this.executor = executor;
        }
    }

    /**
     * Runs a nice, tight loop to simply read input and submit for conversion.
     */
    public void startRead() {
        // Parse through each line of the file
        try {
            while (null != readNextLine()) {
                lineProcessed = csvReader.getLinesRead();
                executor.submitLine(lineProcessed, mappingStrategy, line, exceptionHandler);
            }
        } catch (Exception e) {
            terminalException = e;
        }
    }
}
