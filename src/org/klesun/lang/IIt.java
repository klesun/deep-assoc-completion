package org.klesun.lang;

import org.klesun.lang.iterators.EndIterator;
import org.klesun.lang.iterators.FilterIterator;
import org.klesun.lang.iterators.FlatMapIterator;
import org.klesun.lang.iterators.MapIterator;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.klesun.lang.Lang.It;
import static org.klesun.lang.Lang.*;

/**
 * an interface implemented by both It (a disposable iterator) and L (a reusable list)
 */
public interface IIt<A> extends Iterable<A> {
    boolean has();

    default <B> It<B> fap(Lang.F2<A, Integer, Iterable<B>> flatten)
    {
        return It(() -> new FlatMapIterator<>(iterator(), flatten));
    }

    default <B> It<B> fap(Lang.F<A, Iterable<B>> flatten)
    {
        return fap((el, i) -> flatten.apply(el));
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

    default It<A> lmt(int limit)
    {
        return It(() -> new EndIterator<>(iterator(), (el, i) -> i + 1 >= limit, false));
    }

    /** see https://doc.rust-lang.org/std/result/enum.Result.html#method.or */
    default It<A> orr(Iterable<A> fallback)
    {
        return It.frs(() -> this, () -> fallback);
    }

    // like fch() but executed not at once, but when iterator is actually disposedd. for debug
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
        return Tls.implode(delim, map(Object::toString));
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
        return new It<>(() -> new EndIterator<>(iterator(), endPred, exclusive));
    }

    default It<A> end(F<A, Boolean> endPred)
    {
        return end(false, endPred);
    }

    /**
     * you can read it as "wide map" or "wrap", either  way it does the
     * opposite of "fop" - it takes this list and makes something else
     * it is often handy since declaring a var in php is too verbose to be usable
     */
    default  <B> B wap(Lang.F<IIt<A>, B> wrapper)
    {
        return wrapper.apply(this);
    }

    default It<A> itr()
    {
        return Lang.It(this);
    }

    default L<A> arr()
    {
        L<A> arr = L();
        iterator().forEachRemaining(arr::add);
        return arr;
    }

    default MemIt<A> mem()
    {
        return new MemIt<>(this::iterator);
    }

    // following are expressed through It as default implementation

    default <B> IIt<B> map(Lang.F<A, B> mapper) {
        return itr().map(mapper);
    }
    default <B> It<B> map(Lang.F2<A, Integer, B> mapper) {
        return itr().map(mapper);
    }
    default It<A> flt(Lang.F2<A, Integer, Boolean> pred) {
        return itr().flt(pred);
    }
    default IIt<A> flt(Predicate<A> pred) {
        return itr().flt(pred);
    }
    /**
     * "fop" stands for "Filter Optional"
     * this is a combination of map and filter
     */
    default <B> It<B> fop(Lang.F2<A, Integer, Opt<B>> convert) {
        return itr().fop(convert);
    }
    /** flat map optional, remove elements that don't match */
    default <B> IIt<B> fop(Lang.F<A, Opt<B>> convert) {
        return itr().fop(convert);
    }
    default <B extends A> IIt<B> cst(Class<B> cls) {
        return itr().cst(cls);
    }
    default IIt<A> unq(Lang.F<A, Object> getHash) {
        return itr().unq(getHash);
    }
    default IIt<A> unq() {
        return itr().unq();
    }
}
