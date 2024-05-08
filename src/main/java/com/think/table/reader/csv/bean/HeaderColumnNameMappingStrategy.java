package com.think.table.reader.csv.bean;

/*
 * Copyright 2007 Kyle Miller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.springframework.core.convert.ConversionService;

/**
 * Maps data to objects using the column names in the first row of the CSV file
 * as reference. This way the column order does not matter.
 *
 * @param <T> Type of the bean to be returned
 */
public class HeaderColumnNameMappingStrategy<T> extends HeaderNameBaseMappingStrategy<T> {

    /**
     * Default constructor. Considered stable.
     */
    public HeaderColumnNameMappingStrategy(ConversionService conversionService) {
        super(conversionService);
    }

    /**
     * Constructor to allow setting options for header name mapping.
     * Not considered stable. As new options are introduced for the mapping
     * strategy, they will be introduced here. You are encouraged to use
     * {@link HeaderColumnNameMappingStrategyBuilder}.
     *
     * @param forceCorrectRecordLength If set, every record will be shortened
     *                                 or lengthened to match the number of
     *                                 headers
     * @see HeaderColumnNameMappingStrategyBuilder
     */
    public HeaderColumnNameMappingStrategy(boolean forceCorrectRecordLength, ConversionService conversionService) {
        super(forceCorrectRecordLength, conversionService);
    }
}
