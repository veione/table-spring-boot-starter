package com.think.table.reader.csv;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides an Iterator over the data found in opencsv.
 * <p><em>Fair warning!</em> This mechanism of getting at the data opencsv
 * delivers has limitations when used with the opencsv annotations. Locales and
 * custom converters are not supported. Further features may or may not work.</p>
 */
public class CSVIterator implements Iterator<String[]> {
    private final CSVReader reader;
    private String[] nextLine;

    /**
     * @param reader Reader for the CSV data.
     * @throws IOException If unable to read data from the reader.
     */
    public CSVIterator(CSVReader reader) throws IOException {
        this.reader = reader;
        nextLine = reader.readNext();
    }

    /**
     * Returns true if the iteration has more elements.
     * In other words, returns true if {@link #next()} would return an element
     * rather than throwing an exception.
     *
     * @return True if the CSVIterator has more elements.
     */
    @Override
    public boolean hasNext() {
        return nextLine != null;
    }

    /**
     * Returns the next element in the iterator.
     *
     * @return The next element of the iterator.
     */
    @Override
    public String[] next() {
        String[] temp = nextLine;
        try {
            nextLine = reader.readNext();
        } catch (IOException e) {
            NoSuchElementException nse = new NoSuchElementException(e.getLocalizedMessage());
            nse.initCause(e);
            throw nse;
        }
        return temp;
    }

    /**
     * This method is not supported by opencsv and will throw an
     * {@link UnsupportedOperationException} if called.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("This is a read-only iterator.");
    }
}
