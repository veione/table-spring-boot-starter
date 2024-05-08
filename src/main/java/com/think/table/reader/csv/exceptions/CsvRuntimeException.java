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
package com.think.table.reader.csv.exceptions;

/**
 * The base class for all unchecked exceptions in opencsv.
 * @author Andrew Rucker Jones
 */
public class CsvRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Nullary constructor. Does nothing. */
    public CsvRuntimeException() {}

    /**
     * Constructor with a message.
     * @param message A human-readable error message
     */
    public CsvRuntimeException(String message) {super(message);}

    /**
     * Constructor with a message and throwable
     *
     * @param message A human-readable error message
     * @param e       - Throwable that caused the issue.
     */
    public CsvRuntimeException(String message, Throwable e) {
        super(message, e);
    }
}
