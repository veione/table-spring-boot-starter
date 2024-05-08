package com.think.table.reader.csv.bean.exceptionhandler;

import com.think.table.reader.csv.exceptions.CsvException;

/**
 * An exception handler that always throws exceptions raised.
 *
 * @author Andrew Rucker Jones
 * @since 5.2
 */
final public class ExceptionHandlerThrow implements CsvExceptionHandler {

    /**
     * Default Constructor.
     */
    public ExceptionHandlerThrow() {
    }

    @Override
    public CsvException handleException(CsvException e) throws CsvException {
        throw e;
    }
}
