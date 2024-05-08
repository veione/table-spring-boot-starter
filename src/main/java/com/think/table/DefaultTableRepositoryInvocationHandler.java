package com.think.table;

import com.think.table.annotation.CfgTable;
import com.think.table.properties.TableProperties;
import com.think.table.reader.TableReader;
import com.think.table.repository.CfgRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Default table repository proxy handler.
 *
 * @param <T>
 * @author veione
 */
public class DefaultTableRepositoryInvocationHandler<T> implements CfgRepository<T, Serializable>, InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTableRepositoryInvocationHandler.class);
    private final DefaultTableManager manager;
    private final Class<T> clazz;
    private final Class<T> tableClazz;
    private final Map<Serializable, T> items = new HashMap<>(64);

    public DefaultTableRepositoryInvocationHandler(ApplicationContext applicationContext, Class<T> clazz) {
        this.manager = applicationContext.getBean(DefaultTableManager.class);
        this.clazz = clazz;
        this.tableClazz = getCfgBeanType(clazz);
        this.manager.register(tableClazz, this);
        this.init();
    }

    private void init() {
        TableProperties properties = manager.getProperties();
        TableReader reader = manager.getReader();
        CfgTable cfgTableAnno = tableClazz.getAnnotation(CfgTable.class);
        String tableFileName = String.format("%s%s%s.%s", properties.getPath(), File.separator, cfgTableAnno.value(), reader.getSuffix());
        ClassPathResource resource = new ClassPathResource(tableFileName);

        try (InputStream inputStream = resource.getInputStream()) {
            // 将JSON字符串数组转换为List<CfgItem>
            List<T> itemList = reader.read(inputStream, tableClazz);
            try {
                // 打印转换后的Record对象列表
                for (T item : itemList) {
                    String idName = cfgTableAnno.id();

                    Field idField;
                    if (idName != null && !idName.isEmpty()) {
                        idField = item.getClass().getDeclaredField(idName);
                    } else {
                        idField = item.getClass().getDeclaredField("id");
                    }
                    idField.setAccessible(true);

                    Serializable id = idField.getInt(item);
                    items.put(id, item);
                }
            } catch (Exception e) {
                logger.error("配置表读取失败 {} :(", tableFileName, e);
            }
        } catch (Exception e) {
            logger.error("配置表读取失败 {} :(", tableFileName, e);
        }
    }

    private <T> Class<T> getCfgBeanType(Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericInterfaces()[0]; // Assuming the first interface is the one we want

        if (!(genericSuperclass instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Supertype is not parameterized");
        }

        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

        if (actualTypeArguments.length == 0) {
            throw new IllegalStateException("No generic types found");
        }

        Type beanType = actualTypeArguments[0];

        if (!(beanType instanceof Class)) {
            throw new IllegalArgumentException("First generic type is not a class");
        }

        return (Class<T>) beanType;
    }

    @Override
    public T findById(Serializable id) {
        return items.get(id);
    }

    @Override
    public Optional<T> findById(Predicate<T> predicate) {
        return items.values().stream().filter(predicate).findFirst();
    }

    @Override
    public List<T> findAll(Predicate<T> predicate) {
        return items.values().stream().filter(predicate).toList();
    }

    @Override
    public List<T> findAll() {
        return items.values().stream().toList();
    }

    @Override
    public long count(Predicate<T> predicate) {
        return items.values().stream().filter(predicate).count();
    }

    @Override
    public boolean exists(Serializable id) {
        return items.containsKey(id);
    }

    @Override
    public boolean exists(Predicate<T> predicate) {
        return items.values().stream().anyMatch(predicate);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object 方法，走原生方法,比如hashCode()
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        // 其它走本地代理
        return method.invoke(this, args);
    }

    @Override
    public String toString() {
        return "DefaultCfgRepositoryInvocationHandler{" +
                "manager=" + manager +
                ", repositoryClazz=" + clazz +
                ", tableClazz=" + tableClazz +
                '}';
    }
}
