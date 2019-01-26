package org.klesun.lang;

import org.klesun.lang.iterators.EndIterator;
import org.klesun.lang.iterators.FilterIterator;
import org.klesun.lang.iterators.FlatMapIterator;
import org.klesun.lang.iterators.MapIterator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import static org.klesun.lang.Lang.*;

/**
 * an interface implemented by both It (a disposable iterator) and L (a reusable list)
 * TODO: make Opt laos implement this (will need to refactor everything to work with IIt instead of It)
 */
public interface IIt<A> extends Iterable<A> {
    boolean has();

    default It<A> itr() {
        return Lang.It(this);
    }

    default <B> It<B> map(Lang.F<A, B> mapper)
    {
        return map((el, i) -> mapper.apply(el));
    }

    default <B> It<B> map(Lang.F2<A, Integer, B> mapper)
    {
        Iterator<B> mator = new MapIterator<>(iterator(), mapper);
        return new It<>(() -> mator);
    }

    default It<A> flt(Lang.F2<A, Integer, Boolean> pred)
    {
        Iterator<A> fitor = new FilterIterator<>(iterator(), pred);
        return new It<>(() -> fitor);
    }

    default It<A> flt(Predicate<A> pred)
    {
        return flt((el, i) -> pred.test(el));
    }

    default <B> It<B> fap(Lang.F2<A, Integer, Iterable<B>> flatten)
    {
        Iterator<B> fator = new FlatMapIterator<>(iterator(), flatten);
        return It(() -> fator);
    }

    default <B> It<B> fap(Lang.F<A, Iterable<B>> flatten)
    {
        return fap((el, i) -> flatten.apply(el));
    }

    default <B extends A> It<B> cst(Class<B> cls)
    {
        return fap(val -> Tls.cast(cls, val));
    }

    /**
     * "fop" stands for "Filter Optional"
     * this is a combination of map and filter
     */
    default <B> It<B> fop(Lang.F2<A, Integer, Opt<B>> convert)
    {
        return map(convert).fap(a -> a.itr());
    }

    /** flat map optional, remove elements that don't match */
    default <B> It<B> fop(Lang.F<A, Opt<B>> convert)
    {
        return map(convert).fap(a -> a.itr());
    }

    /** flat map optional, become empty optional if at least one element does not match */
    default <B> Opt<L<B>> fal(Lang.F<A, Opt<B>> convert)
    {
        L<Opt<B>> opts = map(convert).arr();
        if (opts.any(opt -> !opt.has())) {
            return new Opt<>(null);
        } else {
            L<B> vals = opts.map(opt -> opt.unw()).arr();
            return som(vals);
        }
    }

    default It<A> unq()
    {
        Set<A> occurences = new HashSet<>();
        return flt(t -> {
            if (occurences.contains(t)) {
                return false;
            } else {
                occurences.add(t);
                return true;
            }
        });
    }

    default It<A> lmt(int limit)
    {
        Iterator<A> lator = new EndIterator<>(iterator(), (el, i) -> i >= limit, false);
        return It(() -> lator);
    }

    default It<A> unq(Lang.F<A, Object> getHash)
    {
        Set<Object> occurences = new HashSet<>();
        return flt(t -> {
            Object hash = getHash.apply(t);
            if (occurences.contains(hash)) {
                return false;
            } else {
                occurences.add(hash);
                return true;
            }
        });
    }

    default It<A> def(Iterable<A> fallback)
    {
        return has() ? It(this) : It(fallback);
    }

    // like fch() but executed not at once, but when iterator is actually iteratord. for debug
    default It<A> btw(Lang.C2<A, Integer> f)
    {
        return flt((el,i) -> {
            f.accept(el, i);
            return true;
        });
    }

    default It<A> btw(Lang.C<A> f)
    {
        return btw((el, i) -> f.accept(el));
    }

    default Opt<A> fst() {
        return has() ? Lang.som(iterator().next()) : Lang.non();
    }

    /** "rdc" stands for "reduce" */
    default <Tnew> Tnew rdc(Lang.F2<Tnew, A, Tnew> f, Tnew initialValue)
    {
        Tnew value = initialValue;
        for (A el: this) {
            value = f.apply(value, el);
        }
        return value;
    }

    default String str(String delim)
    {
        return Tls.implode(delim, map(val -> val.toString()));
    }

    default String str()
    {
        return str(", ");
    }

    default boolean any(Predicate<A> pred)
    {
        Iterable<A> iter = this::iterator;
        for (A el: iter) {
            if (pred.test(el)) {
                return true;
            }
        }
        return false;
    }

    default boolean all(F2<A, Integer, Boolean> pred)
    {
        Iterable<A> iter = this::iterator;
        int i = 0;
        for (A el: iter) {
            if (!pred.apply(el, i++)) {
                return false;
            }
        }
        return true;
    }

    default boolean all(Predicate<A> pred)
    {
        return all((el, i) -> pred.test(el));
    }

    // end stream the moment endPred returns true (in case of timeout for example)
    default It<A> end(Boolean exclusive, F<A, Boolean> endPred)
    {
        Iterator<A> endor = new EndIterator<>(iterator(), endPred, exclusive);
        return new It<>(() -> endor);
    }

    default It<A> end(F<A, Boolean> endPred)
    {
        return end(false, endPred);
    }

}
