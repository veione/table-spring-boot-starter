package com.think.table.reader.csv.bean.concurrent;

import com.think.table.reader.csv.bean.MappingStrategy;
import com.think.table.reader.csv.bean.exceptionhandler.CsvExceptionHandler;
import com.think.table.reader.csv.bean.util.OrderedObject;
import com.think.table.reader.csv.exceptions.CsvException;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 * @param <T> The type of the bean being converted to
 * @author Andrew Rucker Jones
 * @since 5.0
 */
public class LineExecutor<T> implements Spliterator<T> {
    /**
     * A sorted, concurrent map for the beans created.
     */
    private ConcurrentNavigableMap<Long, T> resultantBeansMap = null;
    /**
     * A queue of the beans created.
     */
    protected final BlockingQueue<OrderedObject<T>> resultQueue = new LinkedBlockingQueue<>();

    /**
     * A list of the ordinals of data records still to be expected by the accumulator.
     */
    protected final SortedSet<Long> expectedRecords = new ConcurrentSkipListSet<>();

    /**
     * A multi-valued map for any exceptions captured.
     * <p>The multi-valued part is important because the same line can throw more
     * than one exception.</p>
     * <p><em>All access to this variable must be synchronized.</em></p>
     */
    private ListValuedMap<Long, CsvException> thrownExceptionsMap = null;
    /**
     * A queue of exceptions thrown by threads during processing.
     */
    protected final BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue = new LinkedBlockingQueue<>();

    private final CompleteFileReader<T> completeFileReader;

    /**
     * The only constructor available for this class.
     *
     * @param completeFileReader The thread that reads lines of input and feeds the
     *                           results to this Executor
     */
    public LineExecutor(CompleteFileReader<T> completeFileReader) {
        this.completeFileReader = completeFileReader;
    }

    public void prepare() {
        completeFileReader.setExecutor(this);

        resultantBeansMap = new ConcurrentSkipListMap<>();
        thrownExceptionsMap = new ArrayListValuedHashMap<>();

        completeFileReader.startRead();

        collectResult();
    }

    private void collectResult() {
        while (!resultQueue.isEmpty()) {
            OrderedObject<T> orderedObject = null;

            // Move the output objects from the unsorted queue to the
            // navigable map. Only the next expected objects are moved;
            // if a gap in numbering occurs, the thread waits until those
            // results have been filled in before continuing.
            if (!expectedRecords.isEmpty()) {
                orderedObject = resultQueue.stream()
                        .filter(e -> expectedRecords.first().equals(e.getOrdinal()))
                        .findAny().orElse(null);
            }
            while (orderedObject != null) {
                resultQueue.remove(orderedObject);
                expectedRecords.remove(expectedRecords.first());
                resultantBeansMap.put(orderedObject.getOrdinal(), orderedObject.getElement());
                if (!expectedRecords.isEmpty()) {
                    orderedObject = resultQueue.stream()
                            .filter(e -> expectedRecords.first().equals(e.getOrdinal()))
                            .findAny().orElse(null);
                } else {
                    orderedObject = null;
                }
            }
        }
    }

    /**
     * Submit one record for conversion to a bean.
     *
     * @param lineNumber       Which record in the input file is being processed
     * @param mapper           The mapping strategy to be used
     * @param line             The line of input to be transformed into a bean
     * @param exceptionHandler The handler for exceptions thrown during record
     *                         processing
     */
    public void submitLine(long lineNumber, MappingStrategy<? extends T> mapper, String[] line, CsvExceptionHandler exceptionHandler) {
        expectedRecords.add(lineNumber);
        try {
            ProcessCsvLine<T> process = new ProcessCsvLine<>(
                    lineNumber, mapper, line,
                    resultQueue, thrownExceptionsQueue,
                    expectedRecords, exceptionHandler);
            process.run();
        } catch (Exception e) {
            expectedRecords.remove(lineNumber);
            throw e;
        }
    }

    /**
     * Determines whether more conversion results can be expected.
     * Since {@link Spliterator}s have no way of indicating that they don't
     * have a result at the moment, but might in the future, we must ensure
     * that every call to {@link #tryAdvance(Consumer)} or {@link #trySplit()}
     * only returns {@code null} if the entire conversion apparatus has shut
     * down and all result queues are cleared. Thus, this method waits until
     * either that is true, or there is truly at least one result that can be
     * returned to users of the {@link Spliterator} interface.
     *
     * @return {@code false} if conversion is complete and no more results
     * can ever be expected out of this {@link Spliterator}, {@code true}
     * otherwise. If {@code true} is returned, it is guaranteed that at
     * least one result is available immediately to the caller.
     */
    private boolean areMoreResultsAvailable() {
        return !resultantBeansMap.isEmpty();
    }

    /**
     * Returns exceptions captured during the conversion process if
     * the conversion process was set not to propagate these errors
     * up the call stack.
     * The call is nondestructive.
     *
     * @return All exceptions captured
     */
    public List<CsvException> getCapturedExceptions() {
        List<CsvException> returnList = null;
        if (thrownExceptionsMap == null) {
            returnList = thrownExceptionsQueue.stream()
                    .filter(Objects::nonNull)
                    .map(OrderedObject::getElement)
                    .collect(Collectors.toList());
        } else {
            returnList = new LinkedList<>();
            synchronized (thrownExceptionsMap) {
                final List<CsvException> finalReturnList = returnList;
                thrownExceptionsMap.keySet().stream()
                        .sorted()
                        .forEach(l -> finalReturnList.addAll(thrownExceptionsMap.get(l)));
            }
        }
        return returnList;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        T bean = null;

        if (areMoreResultsAvailable()) {
            Map.Entry<Long, T> mapEntry = resultantBeansMap.pollFirstEntry();
            if (mapEntry != null) {
                bean = mapEntry.getValue();
            }
            if (bean != null) {
                action.accept(bean);
            }
        }

        return bean != null;
    }

    // WARNING! This code is untested because I have no way of telling the JDK
    // streaming code how to do its job.
    @Override
    public Spliterator<T> trySplit() {
        Spliterator<T> s = null;
        int size = resultantBeansMap.size();
        ArrayList<T> c = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Map.Entry<Long, T> mapEntry = resultantBeansMap.pollFirstEntry();
            if (mapEntry != null) {
                c.add(mapEntry.getValue());
            }
        }
        s = c.spliterator();

        return s;
    }

    @Override
    public long estimateSize() {
        return resultantBeansMap.size();
    }

    @Override
    public int characteristics() {
        return Spliterator.CONCURRENT | Spliterator.NONNULL | Spliterator.ORDERED;
    }
}

