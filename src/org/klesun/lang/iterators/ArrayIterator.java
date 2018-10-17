package org.klesun.lang.iterators;

import java.util.Iterator;

public class ArrayIterator<A> implements Iterator<A> {
    private final A[] source;
    private int pos;

    public ArrayIterator(A[] source) {
        this.source = source;
        pos = 0;
    }

    public boolean hasNext() {
        return source.length > pos;
    }

    public A next() {
        return source[pos++];
    }
}
