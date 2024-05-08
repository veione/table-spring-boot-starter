package com.think.table.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Excel properties
 *
 * @author veione
 */
@Data
@ConfigurationProperties("com.think.table.excel")
public class ExcelProperties {
    /**
     * header row count.
     */
    private int headerRow = 3;
}
