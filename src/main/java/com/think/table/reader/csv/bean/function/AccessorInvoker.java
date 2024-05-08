package com.think.table.reader.csv.bean.function;

import java.lang.reflect.InvocationTargetException;

/**
 * Functional interface for accessing a value from a field.
 * <p>The field must be part of the code created for this functional interface,
 * as there is no way to pass the field in during invocation.</p>
 * <p>This interface is almost identical to {@link java.util.function.Function}
 * save the exceptions thrown, which is the reason for its existence.</p>
 *
 * @param <T> The type of the object from which the value should be retrieved
 * @param <R> The type of the return value
 * @author Andrew Rucker Jones
 * @since 5.0
 */
@FunctionalInterface
public interface AccessorInvoker<T, R> {

    /**
     * Invoke the code to retrieve a value from a field.
     *
     * @param object The object from which the value of the field should be
     *               retrieved
     * @return The value of the field from the given object
     * @throws IllegalAccessException If retrieval causes a problem
     * @throws InvocationTargetException If retrieval causes a problem
     */
    R invoke(T object) throws IllegalAccessException, InvocationTargetException;
}
