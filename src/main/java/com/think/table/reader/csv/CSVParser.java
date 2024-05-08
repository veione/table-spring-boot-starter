package com.think.table.reader.csv;

/*
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import com.think.table.reader.csv.enums.CSVReaderNullFieldIndicator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A very simple CSV parser released under a commercial-friendly license.
 * This just implements splitting a single line into fields.</p>
 *
 * <p>The purpose of the CSVParser is to take a single string and parse it into
 * its elements based on the delimiter, quote and escape characters.</p>
 *
 * <p>The CSVParser has grown organically based on user requests and does not truly match
 * any current requirements (though it can be configured to match or come close).  There
 * are no plans to change this as it will break existing requirements.  Consider using
 * the RFC4180Parser for less configurability but closer match to the RFC4180 requirements.</p>
 *
 * @author Glen Smith
 * @author Rainer Pruy
 */
public class CSVParser extends AbstractCSVParser {

    private static final int BEGINNING_OF_LINE = 3;
    /**
     * This is the character that the CSVParser will treat as the escape character.
     */
    private final char escape;

    /**
     * String of escape character - optimization for replaceAll
     */
    private final String escapeAsString;

    /**
     * String escapeAsString+escapeAsString - optimization for replaceAll
     */
    private final String escapeDoubleAsString;

    /**
     * Determines if the field is between quotes (true) or between separators (false).
     */
    private final boolean strictQuotes;
    /**
     * Ignore any leading white space at the start of the field.
     */
    private final boolean ignoreLeadingWhiteSpace;
    /**
     * Skip over quotation characters when parsing.
     */
    private final boolean ignoreQuotations;
    private int tokensOnLastCompleteLine = -1;
    private boolean inField = false;

    /**
     * Constructs CSVParser using default values for everything.
     */
    public CSVParser() {
        this(DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER,
                DEFAULT_ESCAPE_CHARACTER, DEFAULT_STRICT_QUOTES,
                DEFAULT_IGNORE_LEADING_WHITESPACE,
                DEFAULT_IGNORE_QUOTATIONS,
                DEFAULT_NULL_FIELD_INDICATOR);
    }

    /**
     * Constructs CSVParser.
     * <p>This constructor sets all necessary parameters for CSVParser, and
     * intentionally has package access so only the builder can use it.</p>
     *
     * @param separator               The delimiter to use for separating entries
     * @param quotechar               The character to use for quoted elements
     * @param escape                  The character to use for escaping a separator or quote
     * @param strictQuotes            If true, characters outside the quotes are ignored
     * @param ignoreLeadingWhiteSpace If true, white space in front of a quote in a field is ignored
     * @param ignoreQuotations        If true, treat quotations like any other character.
     * @param nullFieldIndicator      Which field content will be returned as null: EMPTY_SEPARATORS, EMPTY_QUOTES,
     *                                BOTH, NEITHER (default)
     */
    CSVParser(char separator, char quotechar, char escape, boolean strictQuotes, boolean ignoreLeadingWhiteSpace,
              boolean ignoreQuotations, CSVReaderNullFieldIndicator nullFieldIndicator) {
        super(separator, quotechar, nullFieldIndicator);
        if (anyCharactersAreTheSame(separator, quotechar, escape)) {
            throw new UnsupportedOperationException("The separator, quote, and escape characters must be different!");
        }
        if (separator == NULL_CHARACTER) {
            throw new UnsupportedOperationException("The separator character must be defined!");
        }
        this.escape = escape;
        this.escapeAsString = Character.toString(escape);
        this.escapeDoubleAsString = escapeAsString + escapeAsString;
        this.strictQuotes = strictQuotes;
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
        this.ignoreQuotations = ignoreQuotations;
    }

    /**
     * @return The default escape character for this parser.
     */
    public char getEscape() {
        return escape;
    }

    /**
     * @return The default strictQuotes setting for this parser.
     */
    public boolean isStrictQuotes() {
        return strictQuotes;
    }

    /**
     * @return The default ignoreLeadingWhiteSpace setting for this parser.
     */
    public boolean isIgnoreLeadingWhiteSpace() {
        return ignoreLeadingWhiteSpace;
    }

    /**
     * @return The default ignoreQuotation setting for this parser.
     */
    public boolean isIgnoreQuotations() {
        return ignoreQuotations;
    }

    /**
     * Checks to see if any two of the three characters are the same.
     * This is because in opencsv the separator, quote, and escape characters
     * must the different.
     *
     * @param separator The defined separator character
     * @param quotechar The defined quotation cahracter
     * @param escape    The defined escape character
     * @return True if any two of the three are the same.
     */
    private boolean anyCharactersAreTheSame(char separator, char quotechar, char escape) {
        return isSameCharacter(separator, quotechar) || isSameCharacter(separator, escape) || isSameCharacter(quotechar, escape);
    }

    /**
     * Checks that the two characters are the same and are not the defined NULL_CHARACTER.
     * @param c1 First character
     * @param c2 Second character
     * @return True if both characters are the same and are not the defined NULL_CHARACTER
     */
    private boolean isSameCharacter(char c1, char c2) {
        return c1 != NULL_CHARACTER && c1 == c2;
    }

    @Override
    protected String convertToCsvValue(String value, boolean applyQuotestoAll) {
        String testValue = (value == null && !nullFieldIndicator.equals(CSVReaderNullFieldIndicator.NEITHER)) ? "" : value;
        StringBuilder builder = new StringBuilder(testValue == null ? MAX_SIZE_FOR_EMPTY_FIELD : (testValue.length() * 2));
        boolean containsQuoteChar = StringUtils.contains(testValue, getQuotechar());
        boolean containsEscapeChar = StringUtils.contains(testValue, getEscape());
        boolean containsSeparatorChar = StringUtils.contains(testValue, getSeparator());
        boolean surroundWithQuotes = applyQuotestoAll || isSurroundWithQuotes(value, containsSeparatorChar);

        String convertedString = !containsQuoteChar ? testValue : quoteMatcherPattern.matcher(testValue).replaceAll(quoteDoubledAsString);
        convertedString = !containsEscapeChar ? convertedString : convertedString.replace(escapeAsString, escapeDoubleAsString);

        if (surroundWithQuotes) {
            builder.append(getQuotechar());
        }

        builder.append(convertedString);

        if (surroundWithQuotes) {
            builder.append(getQuotechar());
        }

        return builder.toString();
    }

    @Override
    protected String[] parseLine(String nextLine, boolean multi) throws IOException {

        if (!multi && pending != null) {
            pending = null;
        }

        if (nextLine == null) {
            if (pending != null) {
                String s = pending;
                pending = null;
                return new String[]{s};
            }
            return null;
        }
        final List<String> tokensOnThisLine = tokensOnLastCompleteLine <= 0 ? new ArrayList<>() : new ArrayList<>((tokensOnLastCompleteLine + 1) * 2);
        final StringFragmentCopier sfc = new StringFragmentCopier(nextLine);
        boolean inQuotes = false;
        boolean fromQuotedField = false;
        if (pending != null) {
            sfc.append(pending);
            pending = null;
            inQuotes = !this.ignoreQuotations;
        }

        while (!sfc.isEmptyInput()) {
            final char c = sfc.takeInput();
            if (c == this.escape) {
                if (!strictQuotes) {
                    inField = true; // For the unusual case of escaping the first character
                }
                handleEscapeCharacter(nextLine, sfc, inQuotes);
            } else if (c == quotechar) {
                if (isNextCharacterEscapedQuote(nextLine, inQuotes(inQuotes), sfc.i - 1)) {
                    sfc.takeInput();
                    sfc.appendPrev();
                } else {

                    inQuotes = !inQuotes;
                    if (sfc.isEmptyOutput()) {
                        fromQuotedField = true;
                    }

                    // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                    handleQuoteCharButNotStrictQuotes(nextLine, sfc);
                }
                inField = !inField;
            } else if (c == separator && !(inQuotes && !ignoreQuotations)) {
                tokensOnThisLine.add(convertEmptyToNullIfNeeded(sfc.takeOutput(), fromQuotedField));
                fromQuotedField = false;
                inField = false;
            } else {
                if (!strictQuotes || (inQuotes && !ignoreQuotations)) {
                    sfc.appendPrev();
                    inField = true;
                    fromQuotedField = true;
                }
            }

        }
        // line is done - check status
        line_done: {
            if (inQuotes && !ignoreQuotations) {
                if (multi) {
                    // continuing a quoted section, re-append newline
                    sfc.append('\n');
                    pending = sfc.peekOutput();
                    break line_done; // this partial content is not to be added to field list yet
                } else {
                    throw new IOException(String.format(
                            "Unterminated quoted field at end of CSV line. Beginning of lost text: [%s]",
                            sfc.peekOutput()));
                }
            } else {
                inField = false;
            }

            tokensOnThisLine.add(convertEmptyToNullIfNeeded(sfc.takeOutput(), fromQuotedField));
        }

        tokensOnLastCompleteLine = tokensOnThisLine.size();
        return tokensOnThisLine.toArray(ArrayUtils.EMPTY_STRING_ARRAY);

    }

    private void handleQuoteCharButNotStrictQuotes(String nextLine, StringFragmentCopier sfc) {
        if (!strictQuotes) {
            final int i = sfc.i;
            if (i > BEGINNING_OF_LINE //not on the beginning of the line
                    && nextLine.charAt(i - 2) != this.separator //not at the beginning of an escape sequence
                    && nextLine.length() > (i) &&
                    nextLine.charAt(i) != this.separator //not at the	end of an escape sequence
            ) {

                if (ignoreLeadingWhiteSpace && !sfc.isEmptyOutput() && StringUtils.isWhitespace(sfc.peekOutput())) {
                    sfc.clearOutput();
                } else {
                    sfc.appendPrev();
                }
            }
        }
    }

    private void handleEscapeCharacter(String nextLine, StringFragmentCopier sfc, boolean inQuotes) {
        if (isNextCharacterEscapable(nextLine, inQuotes(inQuotes), sfc.i - 1)) {
            sfc.takeInput();
            sfc.appendPrev();
        }
    }

    private String convertEmptyToNullIfNeeded(String s, boolean fromQuotedField) {
        if (s.isEmpty() && shouldConvertEmptyToNull(fromQuotedField)) {
            return null;
        }
        return s;
    }

    private boolean shouldConvertEmptyToNull(boolean fromQuotedField) {
        switch (nullFieldIndicator) {
            case BOTH:
                return true;
            case EMPTY_SEPARATORS:
                return !fromQuotedField;
            case EMPTY_QUOTES:
                return fromQuotedField;
            default:
                return false;
        }
    }

    /**
     * Determines if we can process as if we were in quotes.
     *
     * @param inQuotes Are we currently in quotes?
     * @return True if we should process as if we are inside quotes.
     */
    private boolean inQuotes(boolean inQuotes) {
        return (inQuotes && !ignoreQuotations) || inField;
    }

    /**
     * Checks to see if the character after the index is a quotation character.
     *
     * Precondition: the current character is a quote or an escape.
     *
     * @param nextLine The current line
     * @param inQuotes True if the current context is quoted
     * @param i        Current index in line
     * @return True if the following character is a quote
     */
    private boolean isNextCharacterEscapedQuote(String nextLine, boolean inQuotes, int i) {
        return inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > (i + 1)  // there is indeed another character to check.
                && isCharacterQuoteCharacter(nextLine.charAt(i + 1));
    }

    /**
     * Checks to see if the passed in character is the defined quotation character.
     *
     * @param c Source character
     * @return True if c is the defined quotation character
     */
    private boolean isCharacterQuoteCharacter(char c) {
        return c == quotechar;
    }

    /**
     * Checks to see if the character is the defined escape character.
     *
     * @param c Source character
     * @return True if the character is the defined escape character
     */
    private boolean isCharacterEscapeCharacter(char c) {
        return c == escape;
    }

    /**
     * Checks to see if the character is the defined separator.
     *
     * @param c Source character
     * @return True if the character is the defined separator
     */
    private boolean isCharacterSeparator(char c) {
        return c == separator;
    }

    /**
     * Checks to see if the character passed in could be escapable.
     * Escapable characters for opencsv are the quotation character, the
     * escape character, and the separator.
     *
     * @param c Source character
     * @return True if the character could be escapable.
     */
    private boolean isCharacterEscapable(char c) {
        return isCharacterQuoteCharacter(c) || isCharacterEscapeCharacter(c) || isCharacterSeparator(c);
    }

    /**
     * Checks to see if the character after the current index in a String is an
     * escapable character.
     * <p>Meaning the next character is a quotation character, the escape
     * char, or the separator and you are inside quotes.</p>
     * <p>"Inside quotes" in this context is interpreted liberally. For
     * instance, if quotes are not expected but we are inside a field, that
     * still counts for the purposes of this method as being "in quotes".</p>
     *
     * Precondition: the current character is an escape.
     *
     * @param nextLine The current line
     * @param inQuotes True if the current context is quoted
     * @param i        Current index in line
     * @return True if the following character is a quote
     */
    protected boolean isNextCharacterEscapable(String nextLine, boolean inQuotes, int i) {
        return inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > (i + 1)  // there is indeed another character to check.
                && isCharacterEscapable(nextLine.charAt(i + 1));
    }

    /**
     * This class serves to optimize {@link CSVParser#parseLine(String)},
     * which is the hot inner loop of opencsv.
     */
    private static class StringFragmentCopier {
        private final String input;
        // Index of the next character in input to consume
        private int i = 0;

        // This holds what is known of the next token to be output so far. We initialize this lazily because for
        // CSVs where there are no escaped characters we can actually avoid creating this entirely.
        private StringBuilder sb;
        // Indexes of a substring of nextLine that is logically already appended to the sb buffer. If possible,
        // we just fiddle these indices rather than actually appending anything to sb.
        private int pendingSubstrFrom = 0;
        private int pendingSubstrTo = 0;

        StringFragmentCopier(String input) {
            this.input = input;
        }

        public boolean isEmptyInput() {
            return i >= input.length();
        }

        public char takeInput() {
            return input.charAt(i++);
        }

        private StringBuilder materializeBuilder() {
            if (sb == null) {
                sb = new StringBuilder(input.length() + READ_BUFFER_SIZE);
            }

            if (pendingSubstrFrom < pendingSubstrTo) {
                sb.append(input, pendingSubstrFrom, pendingSubstrTo);
                pendingSubstrFrom = pendingSubstrTo = i;
            }

            return sb;
        }

        public void append(String pending) {
            materializeBuilder().append(pending);
        }

        public void append(char pending) {
            materializeBuilder().append(pending);
        }

        public void appendPrev() {
            if (pendingSubstrTo == pendingSubstrFrom) {
                pendingSubstrFrom = i - 1;
                pendingSubstrTo = i;
            } else if (pendingSubstrTo == i - 1) {
                pendingSubstrTo++;
            } else {
                materializeBuilder().append(input.charAt(i - 1));
            }
        }

        public boolean isEmptyOutput() {
            return pendingSubstrFrom >= pendingSubstrTo && (sb == null || sb.length() == 0);
        }

        public void clearOutput() {
            if (sb != null) {
                sb.setLength(0);
            }

            pendingSubstrFrom = pendingSubstrTo = i;
        }

        public String peekOutput() {
            if (sb == null || sb.length() == 0) {
                return input.substring(pendingSubstrFrom, pendingSubstrTo);
            } else {
                return materializeBuilder().toString();
            }
        }

        public String takeOutput() {
            final String result = peekOutput();
            clearOutput();
            return result;
        }
    }
}
