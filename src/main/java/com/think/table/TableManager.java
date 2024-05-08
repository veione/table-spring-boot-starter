package com.think.table;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Table manager interface.
 *
 * @author veione
 */
public interface TableManager {

    <T> T findById(Class<T> clazz, Serializable id);

    <T> Optional<T> findById(Class<T> clazz, Predicate<T> predicate);

    <T> List<T> findAll(Class<T> clazz, Predicate<T> predicate);

    <T> List<T> findAll(Class<T> clazz);

    <T> long count(Class<T> clazz, Predicate<T> predicate);

    <T> boolean exists(Class<T> clazz, Serializable id);

    <T> boolean exists(Class<T> clazz, Predicate<T> predicate);
}
