/*
 Copyright 2005 Bytecode Pty Ltd.

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
package com.think.table.reader.csv;

import com.think.table.reader.csv.bean.CSVReaderBaseBuilder;
import com.think.table.reader.csv.enums.CSVReaderNullFieldIndicator;

import java.io.Reader;

/**
 * Builder for creating a {@link CSVReader}.
 * <p>This should be the preferred method of creating a Reader as there are so many
 * possible values to be set it is impossible to have constructors for all of
 * them and keep backwards compatibility with previous constructors.<br>
 *
 * <code>
 * final CSVParser parser =<br>
 * new CSVParserBuilder()<br>
 * .withSeparator('\t')<br>
 * .withIgnoreQuotations(true)<br>
 * .build();<br>
 * final CSVReader reader =<br>
 * new CSVReaderBuilder(new StringReader(csv))<br>
 * .withSkipLines(1)<br>
 * .withCSVParser(parser)<br>
 * .build();<br>
 * </code></p>
 *
 * @see com.think.table.reader.csv.CSVReader
 */
public class CSVReaderBuilder extends CSVReaderBaseBuilder<CSVReader> {

    /**
     * Sets the reader to an underlying CSV source.
     *
     * @param reader The reader to an underlying CSV source.
     */
    public CSVReaderBuilder(
            final Reader reader) {
        super(reader);
        if (reader == null) {
            throw new IllegalArgumentException(("The Reader must always be non-null."));
        }
    }

    /**
     * Sets the number of lines to skip before reading.
     *
     * @param skipLines The number of lines to skip before reading.
     * @return {@code this}
     */
    public CSVReaderBuilder withSkipLines(
            final int skipLines) {
        this.skipLines = Math.max(skipLines, 0);
        return this;
    }

    /**
     * Sets the parser to use to parse the input.
     *
     * @param icsvParser The parser to use to parse the input.
     * @return {@code this}
     */
    public CSVReaderBuilder withCSVParser(
            final ICSVParser icsvParser) {
        this.icsvParser = icsvParser;
        return this;
    }

    /**
     * Creates the {@link CSVReader}.
     * @return The {@link CSVReader} based on the set criteria.
     */
    @Override
    public CSVReader build() {
        final ICSVParser parser = getOrCreateCsvParser();
        return new CSVReader(reader, skipLines, parser, keepCR, multilineLimit);
    }

    /**
     * Sets if the reader will keep or discard carriage returns.
     *
     * @param keepCR True to keep carriage returns, false to discard.
     * @return {@code this}
     */
    public CSVReaderBuilder withKeepCarriageReturn(boolean keepCR) {
        this.keepCR = keepCR;
        return this;
    }

    /**
     * Checks to see if it should treat a field with two separators, two quotes, or both as a null field.
     *
     * @param indicator {@link CSVReaderNullFieldIndicator} set to what should be considered a null field.
     * @return {@code this}
     */
    public CSVReaderBuilder withFieldAsNull(CSVReaderNullFieldIndicator indicator) {
        this.nullFieldIndicator = indicator;
        return this;
    }

    /**
     * Sets the maximum number of lines allowed in a multiline record.
     * More than this number in one record results in an IOException.
     *
     * @param multilineLimit No more than this number of lines is allowed in a
     *                       single input record. The default is {@link CSVReader#DEFAULT_MULTILINE_LIMIT}.
     * @return {@code this}
     */
    public CSVReaderBuilder withMultilineLimit(int multilineLimit) {
        this.multilineLimit = multilineLimit;
        return this;
    }
}
