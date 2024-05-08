package com.think.table.reader;

import com.think.table.properties.CsvProperties;
import com.think.table.properties.TableProperties;
import org.springframework.core.convert.ConversionService;

/**
 * Table reader factory.
 *
 * @author veione
 */
public class TableReaderFactory {
    private final TableProperties properties;
    private final ConversionService conversionService;

    public TableReaderFactory(TableProperties properties, ConversionService conversionService) {
        this.properties = properties;
        this.conversionService = conversionService;
    }

    public TableReader createTableReader() {
        TableProperties.TableType type = properties.getType();
        switch (type) {
            case CSV -> {
                CsvProperties csvProperties = properties.getCsv();
                return new CsvTableReader(csvProperties.getLineSeparator().charAt(0), csvProperties.getSkipLines(), conversionService);
            }
            case JSON -> {
                return new JsonTableReader();
            }
            case EXCEL -> {
                return new ExcelTableReader(properties.getExcel().getHeaderRow(), conversionService);
            }
            default -> throw new IllegalArgumentException("Invalid table reader " + type);
        }
    }
}
