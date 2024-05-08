package com.think.table;

import com.think.table.annotation.TableScan;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Table scanner for bean definition register.
 *
 * @author veione
 */
public class TableScanImportBeanDefinitionRegister implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        // 获取TableScan注解属性信息
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(TableScan.class.getName()));
        // 获取注解的属性值,拿到定义的扫描路径
        String[] basePackages = annotationAttributes.getStringArray("basePackage");

        if (ArrayUtils.isEmpty(basePackages)) {
            String className = metadata.getClassName();
            basePackages = new String[]{className.substring(0, className.lastIndexOf('.'))};
        }

        // 使用自定义扫描器扫描
        TableClassPathBeanDefinitionScanner scanner = new TableClassPathBeanDefinitionScanner(registry, false);
        scanner.doScan(basePackages);
    }
}
