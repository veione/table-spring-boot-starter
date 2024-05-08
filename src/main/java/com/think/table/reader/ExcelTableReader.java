package com.think.table.reader;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.think.table.reader.excel.CfgBeanDefinition;
import com.think.table.reader.excel.CfgBeanField;
import com.think.table.reader.excel.ExcelHeader;
import com.think.table.reader.util.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel table reader implementation.
 *
 * @author veioen
 */
public class ExcelTableReader implements TableReader {
    private final int headRowNumber;
    private final ConversionService conversionService;
    private static final Logger log = LoggerFactory.getLogger(ExcelTableReader.class);

    public ExcelTableReader(int headRowNumber, ConversionService conversionService) {
        this.headRowNumber = headRowNumber;
        this.conversionService = conversionService;
    }

    @Override
    public <T> List<T> read(InputStream inputStream, Class<T> clazz) {
        List<T> dataList = new ArrayList<>(32);
        EasyExcel.read(inputStream, new CfgExcelTableParseListener(headRowNumber, clazz, conversionService, dataList))
                .sheet()
                .autoTrim(true)
                .headRowNumber(headRowNumber)
                .doRead();
        return dataList;
    }

    @Override
    public String getSuffix() {
        return "xlsx";
    }

    private static class CfgExcelTableParseListener<T> extends AnalysisEventListener<Map<Integer, String>> {
        private final Map<Integer, ExcelHeader> headerMap = new HashMap<>();
        private final int headRowNumber;
        private int parseRowCount = 0;
        private final List<Map<Integer, String>> headRows;
        private final ConversionService conversionService;
        private final Class<?> clazz;
        private final CfgBeanDefinition beanDefinition;
        private final List<T> dataList;

        public CfgExcelTableParseListener(int headRowNumber, Class<?> clazz, ConversionService conversionService, List<T> dataList) {
            this.headRowNumber = headRowNumber;
            this.clazz = clazz;
            this.dataList = dataList;
            this.conversionService = conversionService;
            this.headRows = new ArrayList<>(headRowNumber);
            this.beanDefinition = new CfgBeanDefinition(clazz);
        }

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            if (parseRowCount < headRowNumber) {
                parseRowCount++;
                headRows.add(headMap);
            }

            if (parseRowCount == headRowNumber) {
                onHeadRowAPost();
            }
        }

        /**
         * 当头部数据解析完成
         */
        private void onHeadRowAPost() {
            // 字段描述
            Map<Integer, String> descMap = headRows.get(0);
            // 字段类型
            Map<Integer, String> typeMap = headRows.get(1);
            // 字段名称
            Map<Integer, String> nameMap = headRows.get(2);

            int colSize = nameMap.size();

            for (int col = 0; col < colSize; col++) {
                ExcelHeader header = new ExcelHeader();
                header.setCol(col);
                header.setName(nameMap.get(col));
                header.setType(typeMap.get(col));
                header.setDescription(descMap.get(col));

                headerMap.put(col, header);
            }
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            Map<String, Object> fieldValueMap = new HashMap<>(data.size());

            // 需要把这一行的数据转换为 Bean 对象
            for (Map.Entry<Integer, String> entry : data.entrySet()) {
                ExcelHeader header = headerMap.get(entry.getKey());
                if (header != null) {
                    // 根据name找到字段对象
                    CfgBeanField field = beanDefinition.getField(header.getName());
                    String value = entry.getValue();
                    if (value != null && !value.isEmpty()) {
                        Object result = conversionService.convert(value, field.getTypeDescriptor());
                        fieldValueMap.put(field.getName(), result);
                    } else if (value == null && field.isPrimitive()) {
                        // 如果是原始类型的则不能为null,需要使用默认值进行填充
                        fieldValueMap.put(field.getName(), TypeUtils.getPrimitiveValue(field.getType()));
                    }
                }
            }

            try {
                Class<?>[] parameters = beanDefinition.getConstructorParameterTypes();
                Constructor<?> constructor = clazz.getConstructor(parameters);

                int parameterCount = constructor.getParameterCount();
                Object[] params = new Object[parameterCount];

                for (int i = 0; i < parameterCount; i++) {
                    String name = beanDefinition.getName(i);
                    Object value = fieldValueMap.get(name);
                    params[i] = value;
                }

                Object instance = constructor.newInstance(params);
                dataList.add((T) instance);
            } catch (NoSuchMethodException e) {
                log.error("Please ensure that the constructor contains all parameters or recommend using the record class for definition table class", e);
            } catch (Exception e) {
                log.error("转换为对象报错啦", e);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        }
    }
}
