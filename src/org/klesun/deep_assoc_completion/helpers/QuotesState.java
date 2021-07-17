package org.klesun.deep_assoc_completion.helpers;

import java.util.Optional;

public class QuotesState {
    final public boolean lacksSurroundingQuotes;
    final public Optional<Character> unterminatedQuoteChar;

    private QuotesState(
        boolean lacksSurroundingQuotes,
        Optional<Character> unterminatedQuoteChar
    ) {
        this.lacksSurroundingQuotes = lacksSurroundingQuotes;
        this.unterminatedQuoteChar = unterminatedQuoteChar;
    }

    public static QuotesState hasSurroundingQuotes() {
        return new QuotesState(false, Optional.empty());
    }

    public static QuotesState lacksSurroundingQuotes() {
        return new QuotesState(true, Optional.empty());
    }

    public static QuotesState unterminatedQuoteChar(char quoteChar) {
        return new QuotesState(false, Optional.of(quoteChar));
    }
}
