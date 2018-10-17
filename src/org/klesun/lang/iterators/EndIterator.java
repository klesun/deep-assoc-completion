package org.klesun.lang.iterators;

import org.klesun.lang.Lang;

import java.util.Iterator;

public class EndIterator<A> implements Iterator<A> {
    private final Iterator<A> sourceIt;
    private final Lang.F<A, Boolean> endPred;
    boolean end;

    public EndIterator(Iterator<A> sourceIt, Lang.F<A, Boolean> endPred) {
        this.sourceIt = sourceIt;
        this.endPred = endPred;
        end = false;
    }

    public boolean hasNext() {
        return !end && sourceIt.hasNext();
    }

    public A next() {
        A next = sourceIt.next();
        end = endPred.apply(next);
        return next;
    }
}
