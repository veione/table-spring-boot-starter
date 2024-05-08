/*
 * Copyright 2016 Andrew Rucker Jones.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.think.table.reader.csv.bean.util;

import com.think.table.reader.csv.bean.exceptionhandler.CsvExceptionHandler;
import com.think.table.reader.csv.exceptions.CsvBadConverterException;
import com.think.table.reader.csv.exceptions.CsvChainedException;
import com.think.table.reader.csv.exceptions.CsvException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.TypeDescriptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class is meant to be a collection of general purpose static methods
 * useful in internal processing for opencsv.
 *
 * @author Andrew Rucker Jones
 * @since 3.9
 */
public final class OpencsvUtils {

    /**
     * This class can't be instantiated.
     */
    private OpencsvUtils() {
    }

    /**
     * I find it annoying that when I want to queue something in a blocking
     * queue, the thread might be interrupted and I have to try again; this
     * method fixes that.
     *
     * @param <E>    The type of the object to be queued
     * @param queue  The queue the object should be added to
     * @param object The object to be queued
     * @since 4.0
     */
    public static <E> void queueRefuseToAcceptDefeat(BlockingQueue<E> queue, E object) {
        boolean interrupted = true;
        while (interrupted) {
            try {
                queue.put(object);
                interrupted = false;
            } catch (InterruptedException ie) {/* Do nothing. */}
        }
    }

    /**
     * A function to consolidate code common to handling exceptions thrown
     * during reading or writing of CSV files.
     * The proper line number is set for the exception, the exception handler
     * is run, and the exception is queued or thrown as necessary.
     *
     * @param e                The exception originally thrown
     * @param lineNumber       The line or record number that caused the exception
     * @param exceptionHandler The exception handler
     * @param queue            The queue for captured exceptions
     * @since 5.2
     */
    public static synchronized void handleException(
            CsvException e, long lineNumber,
            CsvExceptionHandler exceptionHandler, BlockingQueue<OrderedObject<CsvException>> queue) {
        e.setLineNumber(lineNumber);
        CsvException capturedException = null;
        List<CsvException> exceptionList = e instanceof CsvChainedException ?
                Collections.<CsvException>unmodifiableList(((CsvChainedException) e).getExceptionChain()) :
                Collections.singletonList(e);
        for (CsvException iteratedException : exceptionList) {
            try {
                capturedException = exceptionHandler.handleException(iteratedException);
            } catch (CsvException csve) {
                capturedException = csve;
                throw new RuntimeException(csve);
            } finally {
                if (capturedException != null) {
                    queueRefuseToAcceptDefeat(queue,
                            new OrderedObject<>(lineNumber, capturedException));
                }
            }
        }
    }

    /**
     * Compiles a regular expression into a {@link Pattern},
     * throwing an exception that is proper in the context of opencsv if the
     * regular expression is not valid.
     * This method may be used by custom converters if they are required to
     * compile regular expressions that are unknown at compile time.
     *
     * @param regex        The regular expression to be compiled. May be {@code null}
     *                     or an empty string, in which case {@code null} is returned.
     * @param regexFlags   Flags for compiling the regular expression, as in
     *                     {@link Pattern#compile(String, int)}.
     * @param callingClass The class from which this method is being called.
     *                     Used for generating helpful exceptions.
     * @return A compiled pattern, or {@code null} if the input was null or
     * empty
     * @throws CsvBadConverterException If the regular expression is not empty
     *                                  but invalid
     * @since 4.3
     */
    public static Pattern compilePattern(String regex, int regexFlags, Class<?> callingClass)
            throws CsvBadConverterException {
        Pattern tempPattern = null;

        // Set up the regular expression for extraction of the value to be
        // converted
        if (StringUtils.isNotEmpty(regex)) {
            try {
                tempPattern = Pattern.compile(regex, regexFlags);
            } catch (PatternSyntaxException e) {
                CsvBadConverterException csve = new CsvBadConverterException(
                        callingClass,
                        String.format("The specified regular expression is invalid: %s", regex));
                csve.initCause(e);
                throw csve;
            }
        }
        return tempPattern;
    }

    public static TypeDescriptor getTypeDescriptor(Field field) {
        Class<?> fieldType = field.getType();
        TypeDescriptor typeDescriptor = null;

        if (Map.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                Type keyType = typeArguments[0];
                Type valueType = typeArguments[1];
                typeDescriptor = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf((Class<?>) keyType), TypeDescriptor.valueOf((Class<?>) valueType));
            }
        } else if (List.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                typeDescriptor = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf((Class<?>) typeArguments[0]));
            }
        } else if (Set.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                typeDescriptor = TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf((Class<?>) typeArguments[0]));
            }
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                typeDescriptor = TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf((Class<?>) typeArguments[0]));
            }
        } else {
            typeDescriptor = TypeDescriptor.valueOf(fieldType);
        }
        return typeDescriptor;
    }

    public static <T> boolean isFullyArgumentConstructor(Class<? extends T> type) {
        Constructor<? extends T> constructor = BeanUtils.getResolvableConstructor(type);
        return constructor.getParameterCount() > 0;
    }
}
