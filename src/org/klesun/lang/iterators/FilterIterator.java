package org.klesun.lang.iterators;

import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.util.Iterator;

import static org.klesun.lang.Lang.som;

public class FilterIterator<A> implements Iterator<A> {
    private final Iterator<A> sourceIt;
    private final Lang.F2<A, Integer, Boolean> pred;
    Opt<A> current;
    int i;

    public FilterIterator(Iterator<A> sourceIt, Lang.F2<A, Integer, Boolean> pred) {
        this.sourceIt = sourceIt;
        this.pred = pred;
        current = Lang.non();
        i = -1;
    }

    private Opt<A> getCurrent() {
        if (!current.has()) {
            while (sourceIt.hasNext()) {
                A value = sourceIt.next();
                ++i;
                if (pred.apply(value, i)) {
                    this.current = som(value);
                    break;
                }
            }
        }
        return this.current;
    }

    public boolean hasNext() {
        return getCurrent().has();
    }

    public A next() {
        A value = getCurrent().unw();
        current = Lang.non();
        return value;
    }
}
