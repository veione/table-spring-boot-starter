package com.think.table;

import com.think.table.properties.TableProperties;
import com.think.table.reader.TableReader;
import com.think.table.reader.TableReaderFactory;
import com.think.table.repository.CfgRepository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Default table manager implementation.
 *
 * @author veione
 */
public class DefaultTableManager implements TableManager, AutoCloseable {
    private final Map<Class<?>, CfgRepository<?, ?>> tableMap = new HashMap<>(64);
    private final TableReader reader;
    private final TableProperties properties;

    public DefaultTableManager(TableProperties properties, TableReader reader) {
        this.properties = properties;
        this.reader = reader;
    }

    public void init() {
        System.out.println("初始化加载资源数据");
    }

    @Override
    public void close() {
        System.out.println("容器关闭销毁数据");
    }

    protected TableProperties getProperties() {
        return properties;
    }

    @Override
    public <T> Optional<T> findById(Class<T> clazz, Predicate<T> predicate) {
        CfgRepository<T, ?> cfgOperationRepository = (CfgRepository<T, ?>) tableMap.get(clazz);
        if (cfgOperationRepository == null) {
            return Optional.empty();
        }

        return cfgOperationRepository.findById(predicate);
    }

    @Override
    public <T> T findById(Class<T> clazz, Serializable id) {
        CfgRepository<T, Serializable> cfgOperationRepository = (CfgRepository<T, Serializable>) tableMap.get(clazz);
        if (cfgOperationRepository == null) {
            return null;
        }

        return cfgOperationRepository.findById(id);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, Predicate<T> predicate) {
        CfgRepository<T, Serializable> cfgOperationRepository = (CfgRepository<T, Serializable>) tableMap.get(clazz);
        if (cfgOperationRepository == null) {
            return null;
        }

        return cfgOperationRepository.findAll(predicate);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        CfgRepository<T, Serializable> cfgOperationRepository = (CfgRepository<T, Serializable>) tableMap.get(clazz);
        if (cfgOperationRepository == null) {
            return null;
        }

        return cfgOperationRepository.findAll();
    }

    @Override
    public <T> long count(Class<T> clazz, Predicate<T> predicate) {
        CfgRepository<T, Serializable> cfgOperationRepository = (CfgRepository<T, Serializable>) tableMap.get(clazz);
        if (cfgOperationRepository == null) {
            return 0L;
        }

        return cfgOperationRepository.count(predicate);
    }

    @Override
    public <T> boolean exists(Class<T> clazz, Serializable id) {
        CfgRepository<T, Serializable> cfgOperationRepository = (CfgRepository<T, Serializable>) tableMap.get(clazz);
        if (cfgOperationRepository == null) {
            return false;
        }

        return cfgOperationRepository.exists(id);
    }

    @Override
    public <T> boolean exists(Class<T> clazz, Predicate<T> predicate) {
        CfgRepository<T, Serializable> cfgOperationRepository = (CfgRepository<T, Serializable>) tableMap.get(clazz);
        if (cfgOperationRepository == null) {
            return false;
        }

        return cfgOperationRepository.exists(predicate);
    }

    protected TableReader getReader() {
        return reader;
    }

    /**
     * 注册配置表仓库
     *
     * @param clazz
     * @param cfgOperationRepository
     * @param <T>
     */
    protected <T> void register(Class<T> clazz, CfgRepository<T, Serializable> cfgOperationRepository) {
        this.tableMap.put(clazz, cfgOperationRepository);
    }
}
