package org.klesun.lang.iterators;

import org.klesun.lang.Lang;

import java.util.Iterator;

public class EndIterator<A> implements Iterator<A> {
    private final Iterator<A> sourceIt;
    private final Lang.F2<A, Integer, Boolean> endPred;
    boolean end;
    int i = 0;

    public EndIterator(Iterator<A> sourceIt, Lang.F2<A, Integer, Boolean> endPred) {
        this.sourceIt = sourceIt;
        this.endPred = endPred;
        end = false;
    }

    public EndIterator(Iterator<A> sourceIt, Lang.F<A, Boolean> endPred) {
        this(sourceIt, (el,i) -> endPred.apply(el));
    }

    public boolean hasNext() {
        return !end && sourceIt.hasNext();
    }

    public A next() {
        A next = sourceIt.next();
        end = endPred.apply(next, i++);
        return next;
    }
}
