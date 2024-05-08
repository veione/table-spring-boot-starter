package com.think.table.reader.csv.bean;

import com.think.table.reader.csv.bean.util.OpencsvUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps any header name matching a regular expression to a {@link BeanField}.
 *
 * @param <T> The type of the bean being converted
 * @author Andrew Rucker Jones
 * @since 4.2
 */
public class RegexToBeanField<T> extends AbstractFieldMapEntry<String, String, T> {

    /** The compiled regular expression used to match header names. */
    private final Pattern regex;

    /**
     * Initializes this mapping with the regular expression used to map header
     * names and the {@link BeanField} they should be mapped to.
     *
     * @param pattern A valid regular expression against which potential header
     *   names are matched
     * @param field The {@link BeanField} this mapping maps to
     */
    public RegexToBeanField(final String pattern, final BeanField<T, String> field) {
        super(field);
        regex = OpencsvUtils.compilePattern(pattern, Pattern.CASE_INSENSITIVE, BeanFieldJoin.class);
    }

    @Override
    public boolean contains(String key) {
        final Matcher m = regex.matcher(key);
        return m.matches();
    }

    @Override
    public String getInitializer() {
        return regex.pattern();
    }
}