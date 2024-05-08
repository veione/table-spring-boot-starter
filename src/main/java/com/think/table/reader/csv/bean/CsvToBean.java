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
import com.think.table.reader.csv.bean.concurrent.CompleteFileReader;
import com.think.table.reader.csv.bean.concurrent.LineExecutor;
import com.think.table.reader.csv.bean.concurrent.ProcessCsvLine;
import com.think.table.reader.csv.bean.concurrent.SingleLineReader;
import com.think.table.reader.csv.bean.exceptionhandler.CsvExceptionHandler;
import com.think.table.reader.csv.bean.exceptionhandler.ExceptionHandlerQueue;
import com.think.table.reader.csv.bean.exceptionhandler.ExceptionHandlerThrow;
import com.think.table.reader.csv.bean.util.OrderedObject;
import com.think.table.reader.csv.exceptions.CsvException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Converts CSV data to objects.
 * Mixing the {@link #parse()} method with the {@link #iterator() Iterator} is
 * not supported and will lead to unpredictable results. Additionally, reusing
 * an instance of this class after all beans have been read is not supported
 * and will certainly break something.
 *
 * @param <T> Class to convert the objects to.
 */
public class CsvToBean<T> implements Iterable<T> {

    /**
     * A list of all exceptions during parsing and mapping of the input.
     */
    private final List<CsvException> capturedExceptions = new LinkedList<>();

    /**
     * The mapping strategy to be used by this CsvToBean.
     */
    private MappingStrategy<? extends T> mappingStrategy;

    /**
     * The reader this class will use to access the data to be read.
     */
    private CSVReader csvReader;

    /**
     * Determines how exceptions thrown during processing will be handled.
     */
    private CsvExceptionHandler exceptionHandler = new ExceptionHandlerThrow();

    /**
     * The {@link java.util.concurrent.ExecutorService} for parallel processing
     * of beans.
     */
    private LineExecutor<T> executor;

    /**
     * When an empty line is encountered (not part of the data) then it is
     * ignored.
     * By default this is {@code false}, which means an exception is thrown if
     * there are required fields or the number of fields do not match the
     * number of headers.
     */
    private boolean ignoreEmptyLines = false;

    /**
     * Default constructor.
     */
    public CsvToBean() {
    }

    /**
     * Parses the input based on parameters already set through other methods.
     *
     * @return A list of populated beans based on the input
     * @throws IllegalStateException If either MappingStrategy or CSVReader is
     *                               not specified
     * @see #stream()
     * @see #iterator()
     */
    public List<T> parse() throws IllegalStateException {
        return stream().collect(Collectors.toList());
    }

    /**
     * Parses the input based on parameters already set through other methods.
     * This method saves a marginal amount of time and storage compared to
     * {@link #parse()} because it avoids the intermediate storage of the
     * results in a {@link List}. If you plan on further processing
     * the results as a {@link Stream}, use this method.
     *
     * @return A stream of populated beans based on the input
     * @throws IllegalStateException If either MappingStrategy or CSVReader is
     *                               not specified
     * @see #parse()
     * @see #iterator()
     */
    public Stream<T> stream() throws IllegalStateException {
        prepareToReadInput();
        CompleteFileReader<T> completeFileReader = new CompleteFileReader<>(
                csvReader, ignoreEmptyLines,
                mappingStrategy, exceptionHandler);
        executor = new LineExecutor<T>(completeFileReader);
        executor.prepare();
        return StreamSupport.stream(executor, false);
    }

    /**
     * Returns the list of all exceptions that would have been thrown during the
     * import, but were queued by the exception handler.
     * <p>The results returned by this method are not consistent until parsing
     * is concluded.</p>
     *
     * @return The list of exceptions captured while processing the input file
     * @see #setExceptionHandler(CsvExceptionHandler)
     * @see #setThrowExceptions(boolean)
     */
    public List<CsvException> getCapturedExceptions() {
        // The exceptions are stored in different places, dependent on
        // whether or not the iterator is used.
        return executor != null ? executor.getCapturedExceptions() : capturedExceptions;
    }

    /**
     * Sets the mapping strategy to be used by this bean.
     *
     * @param mappingStrategy Mapping strategy to convert CSV input to a bean
     */
    public void setMappingStrategy(MappingStrategy<? extends T> mappingStrategy) {
        this.mappingStrategy = mappingStrategy;
    }

    /**
     * Sets the reader to be used to read in the information from the CSV input.
     *
     * @param csvReader Reader for input
     */
    public void setCsvReader(CSVReader csvReader) {
        this.csvReader = csvReader;
    }

    /**
     * Determines whether errors during import should be thrown or kept in a
     * list for later retrieval via {@link #getCapturedExceptions()}.
     * <p>This is a convenience function and is maintained for backwards
     * compatibility. Passing in {@code true} is equivalent to
     * {@code setExceptionHandler(new ExceptionHandlerThrow())}
     * and {@code false} is equivalent to
     * {@code setExceptionHandler(new ExceptionHandlerQueue())}</p>
     * <p>Please note that if both this method and
     * {@link #setExceptionHandler(CsvExceptionHandler)} are called,
     * the last call wins.</p>
     *
     * @param throwExceptions Whether or not to throw exceptions during
     *                        processing
     * @see #setExceptionHandler(CsvExceptionHandler)
     */
    public void setThrowExceptions(boolean throwExceptions) {
        if (throwExceptions) {
            exceptionHandler = new ExceptionHandlerThrow();
        } else {
            exceptionHandler = new ExceptionHandlerQueue();
        }
    }

    /**
     * Sets the handler for recoverable exceptions raised during processing of
     * records.
     * <p>If neither this method nor {@link #setThrowExceptions(boolean)} is
     * called, the default exception handler is
     * {@link ExceptionHandlerThrow}.</p>
     * <p>Please note that if both this method and
     * {@link #setThrowExceptions(boolean)} are called, the last call wins.</p>
     *
     * @param handler The exception handler to be used. If {@code null},
     *                this method does nothing.
     * @since 5.2
     */
    public void setExceptionHandler(CsvExceptionHandler handler) {
        if (handler != null) {
            exceptionHandler = handler;
        }
    }

    /**
     * Package scope method currently used by the CsvToBeanBuilderTest
     *
     * @return CsvExceptionHandler used by the CsvToBean object.
     */
    CsvExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    private void prepareToReadInput() throws IllegalStateException {
        // First verify that the user hasn't failed to give us the information
        // we need to do his or her work for him or her.
        if (mappingStrategy == null || csvReader == null) {
            throw new IllegalStateException("Both mapping strategy and CSVReader/Reader must be specified!");
        }

        // Get the header information
        try {
            mappingStrategy.captureHeader(csvReader);
        } catch (Exception e) {
            throw new RuntimeException("Error capturing CSV header!", e);
        }
    }

    /**
     * The iterator returned by this method takes one line of input at a time
     * and returns one bean at a time.
     * <p>The advantage to this method is saving memory. The cost is the loss of
     * parallel processing, reducing throughput.</p>
     * <p>The iterator respects all aspects of {@link CsvToBean}, including
     * filters and capturing exceptions.</p>
     *
     * @return An iterator over the beans created from the input
     * @see #parse()
     * @see #stream()
     */
    @Override
    public Iterator<T> iterator() {
        prepareToReadInput();
        return new CsvToBeanIterator();
    }

    /**
     * Ignores any blank lines in the data that are not part of a field.
     *
     * @param ignoreEmptyLines {@code true} to ignore empty lines, {@code false} otherwise
     */
    public void setIgnoreEmptyLines(boolean ignoreEmptyLines) {
        this.ignoreEmptyLines = ignoreEmptyLines;
    }

    /**
     * A private inner class for implementing an iterator for the input data.
     */
    private class CsvToBeanIterator implements Iterator<T> {
        private final BlockingQueue<OrderedObject<T>> resultantBeansQueue;
        private final BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue;
        private final SingleLineReader lineReader = new SingleLineReader(csvReader, ignoreEmptyLines);
        private String[] line = null;
        private long lineProcessed = 0;
        private T bean;

        CsvToBeanIterator() {
            resultantBeansQueue = new ArrayBlockingQueue<>(1);
            thrownExceptionsQueue = new LinkedBlockingQueue<>();
            readSingleLine();
        }

        private void processException() {
            // At least one exception was thrown
            OrderedObject<CsvException> o = thrownExceptionsQueue.poll();
            while (o != null && o.getElement() != null) {
                capturedExceptions.add(o.getElement());
                o = thrownExceptionsQueue.poll();
            }
        }

        private void readLineWithPossibleError() throws IOException {
            // Read a line
            bean = null;
            while (bean == null && null != (line = lineReader.readNextLine())) {
                lineProcessed = lineReader.getLinesRead();

                // Create a bean
                ProcessCsvLine<T> proc = new ProcessCsvLine<>(
                        lineProcessed, mappingStrategy,
                        line, resultantBeansQueue, thrownExceptionsQueue,
                        new TreeSet<>(), exceptionHandler);
                proc.run();

                if (!thrownExceptionsQueue.isEmpty()) {
                    processException();
                } else {
                    // No exception, so there really must always be a bean
                    // . . . unless it was filtered
                    OrderedObject<T> o = resultantBeansQueue.poll();
                    bean = o == null ? null : o.getElement();
                }
            }
            if (line == null) {
                // There isn't any more
                bean = null;
            }
        }

        private void readSingleLine() {
            try {
                readLineWithPossibleError();
            } catch (IOException e) {
                line = null;
                throw new RuntimeException(String.format("Error parsing CSV line: %1$d. [%2$s]",
                        lineProcessed, Arrays.toString(line)), e);
            }
        }

        @Override
        public boolean hasNext() {
            return bean != null;
        }

        @Override
        public T next() {
            if (bean == null) {
                throw new NoSuchElementException();
            }
            T intermediateBean = bean;
            readSingleLine();
            return intermediateBean;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This is a read-only iterator.");
        }
    }
}
