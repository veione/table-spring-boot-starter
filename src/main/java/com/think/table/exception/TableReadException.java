package com.think.table.exception;

/**
 * Table parse exception
 *
 * @author veione
 */
public class TableReadException extends TableException {
    public TableReadException() {
    }

    public TableReadException(Throwable e) {
        super(e);
    }

    public TableReadException(String msg, Throwable e) {
        super(msg, e);
    }
}
