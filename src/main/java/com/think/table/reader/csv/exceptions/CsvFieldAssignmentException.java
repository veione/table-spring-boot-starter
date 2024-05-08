package com.think.table.reader.csv.exceptions;

/**
 * Superclass for checked exceptions that can be thrown while trying to decode
 * and assign a single field.
 *
 * @author Andrew Rucker Jones
 * @since 5.3
 */
public abstract class CsvFieldAssignmentException extends CsvException {

    /** Nullary constructor. */
    public CsvFieldAssignmentException() {}

    /**
     * Constructor for initializing an error message.
     * @param message Human-readable error message
     */
    public CsvFieldAssignmentException(String message) {
        super(message);
    }
}
