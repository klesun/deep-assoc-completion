package org.klesun.lang.iterators;

import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.util.Iterator;

import static org.klesun.lang.Lang.non;
import static org.klesun.lang.Lang.som;

public class EndIterator<A> implements Iterator<A> {
    private final Iterator<A> sourceIt;
    private final Lang.F2<A, Integer, Boolean> endPred;
    private final Boolean exclusive;
    private boolean end;
    private Opt<A> current = non();
    private int i = 0;

    private Opt<A> getCurrent() {
        if (!current.has() && sourceIt.hasNext()) {
            this.current = som(sourceIt.next());
        }
        return this.current;
    }

    public EndIterator(Iterator<A> sourceIt, Lang.F2<A, Integer, Boolean> endPred, Boolean exclusive) {
        this.sourceIt = sourceIt;
        this.endPred = endPred;
        this.exclusive = exclusive;
        end = false;
    }

    public EndIterator(Iterator<A> sourceIt, Lang.F<A, Boolean> endPred, Boolean exclusive) {
        this(sourceIt, (el,i) -> endPred.apply(el), exclusive);
    }

    public boolean hasNext() {
        if (exclusive) {
            return getCurrent().any(value -> !endPred.apply(value, i));
        } else {
            return !end && getCurrent().has();
        }
    }

    public A next() {
        A next = getCurrent().unw();
        end = endPred.apply(next, i++);
        return next;
    }
}
