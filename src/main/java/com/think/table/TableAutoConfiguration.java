package com.think.table;

import com.think.table.converter.StringToMapConverter;
import com.think.table.properties.CsvProperties;
import com.think.table.properties.ExcelProperties;
import com.think.table.properties.TableProperties;
import com.think.table.reader.TableReader;
import com.think.table.reader.TableReaderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * Table auto configuration.
 *
 * @author veione
 */
@Configuration
@EnableConfigurationProperties({TableProperties.class, ExcelProperties.class, CsvProperties.class})
@ConditionalOnProperty(prefix = "com.think.table", name = "enabled", havingValue = "true")
public class TableAutoConfiguration {
    private final TableProperties tableProperties;

    private final ConversionService conversionService;

    public TableAutoConfiguration(TableProperties tableProperties, ConversionService conversionService) {
        this.tableProperties = tableProperties;
        this.conversionService = conversionService;
        if (conversionService instanceof GenericConversionService cs) {
            cs.addConverter(new StringToMapConverter(conversionService));
        }
    }

    @Bean
    public DefaultTableManager tableManager(TableReaderFactory tableReaderFactory) {
        TableReader tableReader = tableReaderFactory.createTableReader();
        return new DefaultTableManager(tableProperties, tableReader);
    }

    @Bean
    public TableReaderFactory tableReaderFactory() {
        return new TableReaderFactory(tableProperties, conversionService);
    }
}
