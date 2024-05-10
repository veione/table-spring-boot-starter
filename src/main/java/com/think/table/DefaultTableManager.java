package com.think.table;

import com.think.table.annotation.CfgTable;
import com.think.table.properties.TableProperties;
import com.think.table.reader.TableReader;
import com.think.table.repository.CfgRepository;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Default table manager implementation.
 *
 * @author veione
 */
public class DefaultTableManager implements TableManager, AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(DefaultTableManager.class);
    private final Map<Class<?>, CfgRepository<?, ?>> tableMap = new HashMap<>(64);
    private final Map<String, Class<?>> tableNameMap = new HashMap<>(64);
    private final TableReader reader;
    private final TableProperties properties;
    private FileAlterationObserver fileAlterationObserver;
    private FileAlterationMonitor fileAlterationMonitor;

    public DefaultTableManager(TableProperties properties, TableReader reader) {
        this.properties = properties;
        this.reader = reader;
        this.startMonitor();
    }

    /**
     * 开启文件监听
     */
    private void startMonitor() {
        // 轮询间隔 5 秒
        long interval = TimeUnit.SECONDS.toMillis(properties.getInterval());

        try {
            String path = new ClassPathResource(properties.getPath()).getFile().getAbsolutePath();
            // 创建一个文件观察器用于处理文件的格式
            fileAlterationObserver = new FileAlterationObserver(path,
                    FileFilterUtils.and(FileFilterUtils.fileFileFilter())
                            .or(FileFilterUtils.suffixFileFilter("csv"))
                            .or(FileFilterUtils.suffixFileFilter("json"))
                            .or(FileFilterUtils.suffixFileFilter("xlsx")));
            //设置文件变化监听器
            fileAlterationObserver.addListener(new FileAlterationListenerAdaptor() {
                @Override
                public void onFileChange(File file) {
                    String fileName = file.getName();
                    logger.info("Table resource {} changed.", fileName);
                    String fileBaseName = FilenameUtils.getBaseName(fileName);
                    Class<?> clazz = tableNameMap.get(fileBaseName);
                    if (clazz == null) {
                        logger.warn("Table file reload fail, table info not exist -> {}", fileBaseName);
                        return;
                    }
                    CfgRepository repository = tableMap.get(clazz);
                    if (repository == null) {
                        logger.warn("Table file reload fail, Repository not exist -> {}", fileBaseName);
                        return;
                    }
                    Reloadable reloadable = (Reloadable) repository;
                    reloadable.reload();
                }
            });
            fileAlterationMonitor = new FileAlterationMonitor(interval, fileAlterationObserver);
            fileAlterationMonitor.start();
            logger.info("Table hotswap monitor stared :) interval(s): {}, path: {}", interval, path);
        } catch (Exception e) {
            logger.error("Start table hotswap monitor failed", e);
        }
    }

    @Override
    public void close() {
        tableMap.clear();
        tableNameMap.clear();
        fileAlterationMonitor.removeObserver(fileAlterationObserver);
        logger.info("配置表容器关闭,资源清理完毕 :)");
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
     * @param repository
     * @param <T>
     */
    protected <T> void register(Class<T> clazz, CfgRepository<T, Serializable> repository) {
        this.tableMap.put(clazz, repository);
        CfgTable anno = clazz.getAnnotation(CfgTable.class);
        this.tableNameMap.put(anno.value(), clazz);
    }
}
