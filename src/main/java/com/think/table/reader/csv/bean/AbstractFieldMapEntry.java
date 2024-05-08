package com.think.table.reader.csv.bean;

import java.util.Locale;

/**
 * Collects common aspects of a {@link ComplexFieldMapEntry}.
 *
 * @param <I> The initializer type used to build the many-to-one mapping
 * @param <K> The type of the key used for indexing
 * @param <T> The type of the bean being converted
 *
 * @author Andrew Rucker Jones
 * @since 4.2
 */
abstract public class AbstractFieldMapEntry<I, K extends Comparable<K>, T> implements ComplexFieldMapEntry<I, K, T> {

    /** The {@link BeanField} that is the target of this mapping. */
    protected final BeanField<T, K> field;

    /** The locale to be used for error messages. */
    protected Locale errorLocale;

    /**
     * The only constructor, and it must be called by all derived classes.
     *
     * @param field The BeanField being mapped to
     */
    protected AbstractFieldMapEntry(final BeanField<T, K> field) {
        this.field = field;
    }

    @Override
    public BeanField<T, K> getBeanField() {
        return field;
    }
}
