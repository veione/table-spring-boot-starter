package com.think.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Table scanner for bean definition register.
 *
 * @author veione
 */
public class TableScanImportBeanDefinitionRegister implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware {
    private static final Logger logger = LoggerFactory.getLogger(TableScanImportBeanDefinitionRegister.class);
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (!AutoConfigurationPackages.has(this.beanFactory)) {
            logger.debug("Could not determine auto-configuration package, automatic excel table scanning disabled.");
            return;
        }

        logger.debug("Searching for repository annotated with @TableRepository");

        List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
        if (logger.isDebugEnabled()) {
            packages.forEach(pkg -> logger.debug("Using auto-configuration base package '{}'", pkg));
        }

        // 使用自定义扫描器扫描
        TableClassPathBeanDefinitionScanner scanner = new TableClassPathBeanDefinitionScanner(registry, false);
        scanner.doScan(StringUtils.toStringArray(packages));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
