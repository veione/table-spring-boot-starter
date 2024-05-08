package com.think.table.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Csv properties
 *
 * @author veione
 */
@Data
@ConfigurationProperties("com.think.table.csv")
public class CsvProperties {
    /**
     * 分隔符
     */
    private String lineSeparator = "|";
    /**
     * 跳过行
     */
    private int skipLines = 0;
}
