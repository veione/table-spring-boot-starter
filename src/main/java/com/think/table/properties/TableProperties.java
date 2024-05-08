package com.think.table.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Table properties
 *
 * @author veione
 */
@Data
@ConfigurationProperties("com.think.table")
public class TableProperties {
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
