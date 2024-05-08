package com.think.table.reader;

import com.think.table.exception.TableReadException;

import java.io.InputStream;
import java.util.List;

/**
 * Table reader interface.
 *
 * @author veione
 */
public interface TableReader {
    /**
     * Converter to object
     *
     * @param inputStream
     * @param clazz
     * @param <T>
     * @return
     * @throws TableReadException
     */
    <T> List<T> read(InputStream inputStream, Class<T> clazz) throws TableReadException;

    /**
     * Table file suffix.
     *
     * @return
     */
    String getSuffix();
}
