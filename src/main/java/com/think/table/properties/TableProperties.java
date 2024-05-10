package com.think.table.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.think.table.properties.TableProperties.PREFIX;

/**
 * Table properties
 *
 * @author veione
 */
@Data
@ConfigurationProperties(PREFIX)
public class TableProperties {
    /**
     * Table properties prefix
     */
    public static final String PREFIX = "com.think.table";
    /**
     * enabled table auto configuration
     */
    private boolean enabled;
    /**
     * Excel path
     */
    private String path;
    /**
     * 指定扫描包
     */
    private String scanPackage;
    /**
     * 格式：json、excel、csv
     */
    private TableType type;
    /**
     * Excel properties
     */
    @NestedConfigurationProperty
    private ExcelProperties excel = new ExcelProperties();
    /**
     * Csv properties
     */
    @NestedConfigurationProperty
    private CsvProperties csv = new CsvProperties();

    public enum TableType {
        CSV,
        EXCEL,
        JSON;
    }
}
