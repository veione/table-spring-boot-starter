package com.think.table;

import com.think.table.annotation.TableRepository;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * Table class path bean definition scanner.
 *
 * @author veione
 */
public class TableClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public TableClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        super(registry, useDefaultFilters);
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        // 添加过滤器,只扫描@TableRepository注解的类
        addIncludeFilter(new AnnotationTypeFilter(TableRepository.class));
        Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);

        try {
            doTableProxy(beanDefinitionHolders);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        return beanDefinitionHolders;
    }

    private void doTableProxy(Set<BeanDefinitionHolder> beanDefinitionHolders) throws ClassNotFoundException {
        for (BeanDefinitionHolder holder : beanDefinitionHolders) {
            // 设置工厂等操作需要基于GenericBeanDefinition，BeanDefinitionHolder是其子类
            GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();
            // 设置构造函数参数
            ConstructorArgumentValues constructorArgumentValues = definition.getConstructorArgumentValues();
            String beanClassName = definition.getBeanClassName();
            // 将beanClassName中的类名解析为Class对象
            Class<?> beanClass = Class.forName(beanClassName);
            constructorArgumentValues.addGenericArgumentValue(beanClass);
            definition.setLazyInit(true);
            // 设置工厂
            definition.setBeanClass(TableRepositoryFactoryBean.class);
            definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        }
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isInterface() && metadata.isIndependent();
    }
}
