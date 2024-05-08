package com.think.table.exception;

/**
 * Table base exception
 *
 * @author veione
 */
public class TableException extends Exception {
    public TableException() {
    }

    public TableException(Throwable e) {
        super(e);
    }

    public TableException(String msg, Throwable e) {
        super(msg, e);
    }
}
