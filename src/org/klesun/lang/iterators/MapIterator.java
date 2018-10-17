package org.klesun.lang.iterators;

import org.klesun.lang.Lang;

import java.util.Iterator;

public class MapIterator<A, B> implements Iterator<B> {
    private final Iterator<A> sourceIt;
    private final Lang.F2<A, Integer, B> mapper;
    int pos;

    public MapIterator(Iterator<A> sourceIt, Lang.F2<A, Integer, B> mapper) {
        this.sourceIt = sourceIt;
        this.mapper = mapper;
        pos = 0;
    }

    public boolean hasNext() {
        return sourceIt.hasNext();
    }

    public B next() {
        return mapper.apply(sourceIt.next(), pos++);
    }
}
