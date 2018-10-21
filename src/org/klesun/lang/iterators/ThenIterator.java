package org.klesun.lang.iterators;

import org.klesun.lang.Lang;

import java.util.Iterator;

public class ThenIterator<A> implements Iterator<A> {
    private final Iterator<A> sourceIt;
    private final Lang.C<Integer> then;
    int i = 0;
    boolean thened = false;

    public ThenIterator(Iterator<A> sourceIt, Lang.C<Integer> then) {
        this.sourceIt = sourceIt;
        this.then = then;
    }

    public boolean hasNext() {
        boolean has = sourceIt.hasNext();
        if (!has && !thened) {
            thened = true;
            then.accept(i);
        }
        return has;
    }

    public A next() {
        ++i;
        return sourceIt.next();
    }
}
