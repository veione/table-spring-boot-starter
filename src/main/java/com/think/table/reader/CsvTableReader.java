package com.think.table.reader;

import com.think.table.exception.TableReadException;
import com.think.table.reader.csv.bean.CsvToBean;
import com.think.table.reader.csv.bean.CsvToBeanBuilder;
import com.think.table.reader.csv.bean.HeaderColumnNameMappingStrategy;
import org.springframework.core.convert.ConversionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV reader implementation.
 *
 * @author veione
 */
public class CsvTableReader implements TableReader {
    private final char lineSeparator;
    private final int skipLines;
    private final ConversionService conversionService;

    public CsvTableReader(char lineSeparator, int skipLines, ConversionService conversionService) {
        this.lineSeparator = lineSeparator;
        this.skipLines = skipLines;
        this.conversionService = conversionService;
    }

    @Override
    public <T> List<T> read(InputStream inputStream, Class<T> clazz) throws TableReadException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>(conversionService);
            strategy.setType(clazz);

            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withSeparator(lineSeparator)
                    .withIgnoreEmptyLine(true)
                    .withMappingStrategy(strategy)
                    .withSkipLines(skipLines)
                    .withConversionService(conversionService)
                    .build();

            return csvToBean.parse();
        } catch (IOException e) {
            throw new TableReadException(e);
        }
    }

    @Override
    public String getSuffix() {
        return "csv";
    }
}
