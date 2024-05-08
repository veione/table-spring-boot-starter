package com.think.table.reader.csv.bean.concurrent;

import com.think.table.reader.csv.CSVReader;
import com.think.table.reader.csv.bean.CsvToBean;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * This class exists to isolate the logic for reading a single line of input
 * that is common to {@link CsvToBean#iterator()} and {@link CompleteFileReader}.
 * It is not meant for end user consumption.
 *
 * @author Andrew Rucker Jones
 * @since 5.2
 */
public class SingleLineReader {
    protected final CSVReader csvReader;
    protected final boolean ignoreEmptyLines;

    /**
     * Stores the result of parsing a line of input.
     */
    protected String[] line;

    /**
     * The only constructor.
     *
     * @param csvReader        The {@link CSVReader} for reading the input
     * @param ignoreEmptyLines Whether blank lines of input should be ignored
     */
    public SingleLineReader(CSVReader csvReader, boolean ignoreEmptyLines) {
        this.csvReader = csvReader;
        this.ignoreEmptyLines = ignoreEmptyLines;
    }

    private boolean isCurrentLineEmpty() {
        return line.length == 0 || (line.length == 1 && StringUtils.isEmpty(line[0]));
    }

    /**
     * Reads from the {@link CSVReader} provided on instantiation until a
     * usable line of input is found.
     *
     * @return The next line of significant input, or {@code null} if none
     * remain
     * @throws IOException If bad things happen during the read
     */
    public String[] readNextLine() throws IOException {
        do {
            line = csvReader.readNext();
        } while (line != null && isCurrentLineEmpty() && ignoreEmptyLines);
        return getLine();
    }

    /**
     * @return The number of lines read from the input this far
     */
    public long getLinesRead() {
        return csvReader.getLinesRead();
    }

    /**
     * Returns a copy of the last line read by {@link #readNextLine()}.
     *
     * @return A new array with the last line read
     */
    public String[] getLine() {
        String[] lineCopy = line;
        if (line != null) {
            lineCopy = new String[line.length];
            System.arraycopy(line, 0, lineCopy, 0, line.length);
        }
        return lineCopy;
    }
}
