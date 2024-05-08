package com.think.table.annotation;

import com.think.table.TableScanImportBeanDefinitionRegister;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// 使用import的方式导入
@Import(TableScanImportBeanDefinitionRegister.class)
public @interface TableScan {
    @AliasFor("value")
    String[] basePackage() default {};

    @AliasFor("basePackage")
    String[] value() default {};
}
