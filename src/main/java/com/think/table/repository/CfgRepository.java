package com.think.table.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Table repository interface, for subclass table repository proxy.
 *
 * @param <T>
 * @param <Serializable>
 * @author veione
 */
public interface CfgRepository<T, Serializable> {

    T findById(Serializable id);

    Optional<T> findById(Predicate<T> predicate);

    List<T> findAll(Predicate<T> predicate);

    List<T> findAll();

    long count(Predicate<T> predicate);

    boolean exists(Serializable id);

    boolean exists(Predicate<T> predicate);
}
