package com.think.table.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.think.table.exception.TableReadException;

import java.io.InputStream;
import java.util.List;

/**
 * JSON table reader implementation.
 *
 * @author veione
 */
public class JsonTableReader implements TableReader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        MAPPER.registerModule(new ParameterNamesModule());
    }

    @Override
    public <T> List<T> read(InputStream inputStream, Class<T> clazz) throws TableReadException {
        try {
            return MAPPER.readValue(inputStream, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new TableReadException(e);
        }
    }

    @Override
    public String getSuffix() {
        return "json";
    }
}
