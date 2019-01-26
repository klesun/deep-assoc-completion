package org.klesun.lang;

/** an interface implemented by both It (a disposable iterator) and L (a reusable list) */
public interface IIt<A> {
    public <B> It<B> fap(Lang.F2<A, Integer, Iterable<B>> flatten);
}
