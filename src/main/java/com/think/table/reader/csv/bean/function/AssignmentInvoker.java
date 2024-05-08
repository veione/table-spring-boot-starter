package com.think.table.reader.csv.bean.function;

import java.lang.reflect.InvocationTargetException;

/**
 * Functional interface for assigning a value to a field.
 * <p>The field must be part of the code created for this functional interface,
 * as there is no way to pass the field in during invocation.</p>
 * <p>This interface is almost identical to {@link java.util.function.BiConsumer}
 * save the exceptions thrown, which is the reason for its existence.</p>
 *
 * @param <T> The type of the object upon which the assignment code is to be
 *            invoked
 * @param <U> The type of the value that is to be assigned
 * @author Andrew Rucker Jones
 * @since 5.0
 */
@FunctionalInterface
public interface AssignmentInvoker<T, U> {

    /**
     * Invoke the code to assign a value to a field.
     *
     * @param object The object upon which the assignment method should be
     *               invoked
     * @param value  The value to assign to a member variable of the given
     *               object
     * @throws IllegalAccessException    If assignment causes a problem
     * @throws InvocationTargetException If assignment causes a problem
     */
    void invoke(T object, U value) throws IllegalAccessException, InvocationTargetException;
}