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

import java.io.IOException;

/**
 * Exception that is thrown when the {@link com.opencsv.CSVReader} cannot process a line.
 */
public class CsvMalformedLineException extends IOException {
    private static final long serialVersionUID = 1L;
    private long lineNumber;
    private String context;

    /**
     * @return The row number set during construction of the exception
     */
    public long getLineNumber() {
        return lineNumber;
    }

    /**
     * @return Context set during construction of the exception
     */
    public String getContext() {
        return context;
    }

    /**
     * Nullary constructor. Does nothing.
     */
    public CsvMalformedLineException() {
        super();
    }

    /**
     * Constructor with a message.
     *
     * @param message    A human-readable error message
     * @param lineNumber Line number where error occurred
     * @param context    Line (or part of the line) that caused the error
     */
    public CsvMalformedLineException(String message, long lineNumber, String context) {
        super(message);
        this.lineNumber = lineNumber;
        this.context = context;
    }
}
