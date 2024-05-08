package com.think.table.reader.csv.bean.exceptionhandler;

import com.think.table.reader.csv.bean.CsvToBean;
import com.think.table.reader.csv.exceptions.CsvException;

/**
 * This interface defines a generic way of dealing with exceptions thrown
 * during the creation of beans or their conversion to CSV output.
 * @author Andrew Rucker Jones
 * @since 5.2
 */
@FunctionalInterface
public interface CsvExceptionHandler {

    /**
     * <p>Determines how opencsv will handle exceptions that crop up during
     * bean creation or writing.
     * There are three ways of dealing with an exception:</p>
     * <ol>
     *     <li>Ignore the exception. In this case, return {@code null}.</li>
     *     <li>Queue the exception. In this case, return either the original
     *     exception thrown, or a new exception that meets your needs better.</li>
     *     <li>Halt processing by throwing the exception. In this case, throw
     *     the exception or a new exception that meets your needs better.</li>
     * </ol>
     * <p>Please be cautioned: Due to the multi-threaded nature of opencsv's
     * conversion routines, there is no guarantee that the exception thrown,
     * if your chosen error handler throws exceptions, is the error in the
     * input that would have caused the exception to be thrown if the input
     * were processed in strict sequential order. That is, if there are four
     * errors in the input and your error handler queues the first three but
     * throws the fourth, the exception thrown might relate to <em>any</em>
     * of the four errors in the input.</p>
     * <p>If your exception handler queues a certain number of exceptions
     * before throwing one, opencsv makes a best-faith effort to make sure all
     * of the exceptions marked for queueing are actually available via
     * {@link CsvToBean#getCapturedExceptions()} or
     * but there are no absolute guarantees.</p>
     * <p>This method must be thread-safe.</p>
     * @param e The exception that was thrown
     * @return The exception to be queued, or {@code null} if the exception
     *   should be ignored
     * @throws CsvException If a halt to all processing is desired
     */
    CsvException handleException(CsvException e) throws CsvException;
}
