package org.klesun.lang;

import org.klesun.lang.iterators.FlatMapIterator;

import java.util.ArrayList;
import java.util.List;

import static org.klesun.lang.Lang.It;

/**
 * unlike IReusableIt, this one does not include MemIt - supposedly all iterables implementing this
 * interface have any of values available at constant time: either first, second, etc or last
 *
 * basically it's for collections implementing IIt
 */
public interface IResolvedIt<T> extends IReusableIt<T>
{
    public static <Ts> IResolvedIt<Ts> fst(Lang.S<IResolvedIt<Ts>>... attempts)
    {
        for (Lang.S<IResolvedIt<Ts>> a: attempts) {
            IResolvedIt<Ts> result = a.get();
            if (result.has()) {
                return result;
            }
        }
        return L.non();
    }

    /**
     * Reusable ConCat - same as It.cnc(), but try to preserve already resolved
     * collection without transforming it to an iterator when possible
     */
    public static <Ts> IIt<Ts> rnc(IIt<Ts>... attempts)
    {
        return Lang.L(attempts).rap(a -> a);
    }

    int size();

    /**
     * stands for Reusable Flat Map
     * runs through all elements and applies the `fapper`
     * if for all elements `fapper` results in IResolvedIt, they are instantly
     * joined returned as a complete L, otherwise they are returned as It
     *
     * needed to pass further the info that data was already calculated and
     * there are no hidden performance holes in iteration over the elements
     */
    default <U> IIt<U> rap(Lang.F<T, IIt<U>> fapper)
    {
        return rap((el, i) -> fapper.apply(el));
    }

    default <U> IIt<U> rap(Lang.F2<T, Integer, IIt<U>> fapper)
    {
        boolean hasLazy = false;
        List<IIt<U>> iters = new ArrayList<>(this.size());
        int i = 0;
        for (T el: this) {
            IIt<U> iter = fapper.apply(el, i);
            iters.add(iter);
            boolean isResolved =
                iter instanceof IResolvedIt ||
                (iter instanceof It) && ((It<T>)iter).knownToBeEmpty;
            hasLazy |= !isResolved;
            ++i;
        }
        It<U> flatIt = Lang.It(iters).fap(iter -> iter);
        return hasLazy ? flatIt : flatIt.arr();
    }
}
