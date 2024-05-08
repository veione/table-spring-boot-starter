package com.think.table.reader.csv.bean.exceptionhandler;

import com.think.table.reader.csv.exceptions.CsvException;

/**
 * An exception handler that always queues exceptions raised.
 *
 * @author Andrew Rucker Jones
 * @since 5.2
 */
final public class ExceptionHandlerQueue implements CsvExceptionHandler {

    /**
     * Default Constructor.
     */
    public ExceptionHandlerQueue() {
    }

    @Override
    public CsvException handleException(CsvException e) {
        return e;
    }
}
