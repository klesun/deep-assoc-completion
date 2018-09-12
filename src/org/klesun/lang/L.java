package org.klesun.lang;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;

/**
 * shorter names; convenient map(), filter() right inside
 * the class (yeah, less performance, but easier to read)
 * some integration with Opt class
 */
public class L<@NonNull T> extends ListWrapper<T> implements List<T>
{
    L(List<T> source)
    {
        super(source);
    }

    public L()
    {
        super(new ArrayList<>());
    }

    /** "gat" stands for "get at" */
    public Opt<T> gat(int index)
    {
        if (index < 0) {
            index = s.size() + index;
        }
        return index >= 0 && index < s.size() ? Lang.opt(s.get(index)) : Lang.opt(null);
    }

    public Opt<T> fst()
    {
        return gat(0);
    }

    public Opt<T> lst()
    {
        return gat(-1);
    }

    public L<T> def(L<T> fallback)
    {
        return size() > 0 ? this : fallback;
    }

    public L<T> flt(Lang.F2<T, Integer, Boolean> pred)
    {
        L<T> result = Lang.list();
        for (int i = 0; i < size(); ++i) {
            if (pred.apply(get(i), i)) {
                result.add(get(i));
            }
        }
        return result;
    }

    public L<T> flt(Predicate<T> pred)
    {
        return flt((val, i) -> pred.test(val));
    }

    public <@NonNull Tnew> L<Tnew> map(Lang.F<T, @NonNull Tnew> f)
    {
        return this.map((el, i) -> f.apply(el));
    }

    public <@NonNull Tnew> L<Tnew> map(Lang.F2<T, Integer, Tnew> f)
    {
        L<Tnew> result = Lang.L();
        for (int i = 0; i < s.size(); ++i) {
            T el = s.get(i);
            result.add(f.apply(el, i));
        }
        return result;
    }

    public <Tnew> L<Tnew> mop(Lang.F<T, Tnew> f)
    {
        return this.mop((el, i) -> f.apply(el));
    }

    /** same as map, but also removes null values from the result */
    public <Tnew> L<Tnew> mop(Lang.F2<T, Integer, Tnew> f)
    {
        L<Tnew> result = Lang.L();
        for (int i = 0; i < s.size(); ++i) {
            Tnew fed = f.apply(s.get(i), i);
            if (fed != null) {
                result.add(fed);
            }
        }
        return result;
    }

    /**
     * "fop" stands for "Filter Optional"
     * this is a combination of map and filter
     */
    public <Tnew> L<Tnew> fop(Lang.F<T, Opt<Tnew>> convert)
    {
        return fop((el, i) -> convert.apply(el));
    }

    public <Tnew> L<Tnew> fop(Lang.F2<T, Integer, Opt<Tnew>> convert)
    {
        List<Tnew> result = Lang.list();
        for (int i = 0; i < s.size(); ++i) {
            convert.apply(s.get(i), i).thn(result::add);
        }
        return new L(result);
    }

    /**
     * "fop" stands for flat map - flattens list by lambda
     */
    public <Tnew> L<Tnew> fap(Lang.F<T, List<Tnew>> flatten)
    {
        return fap((el, i) -> flatten.apply(el));
    }

    public <Tnew> L<Tnew> fap(Lang.F2<T, Integer, List<Tnew>> flatten)
    {
        List<Tnew> result = Lang.list();
        for (int i = 0; i < s.size(); ++i) {
            result.addAll(flatten.apply(s.get(i), i));
        }
        return new L<>(result);
    }

    public boolean any(Predicate<T> pred)
    {
        return flt(pred).size() > 0;
    }

    public boolean all(Lang.F2<T, Integer, Boolean> pred)
    {
        return map(pred).flt(b -> b).size() == this.size();
    }

    /**
     * you can read it as "wide map" or "wrap", either  way it does the
     * opposite of "fop" - it takes this list and makes something else
     * it is often handy since declaring a var in php is too verbose to be usable
     */
    public <Tnew> Tnew wap(Lang.F<L<T>, Tnew> wrapper)
    {
        return wrapper.apply(this);
    }

    /**
     * "fch" stands for For Each
     */
    public L<T> fch(Lang.C<T> f)
    {
        fch((el, i) -> f.accept(el));
        return this;
    }

    public void fch(Lang.C2<T, Integer> f)
    {
        for (int i = 0; i < s.size(); ++i) {
            f.accept(s.get(i), i);
        }
    }

    /** "rdc" stands for "reduce" */
    public <Tnew> Tnew rdc(Lang.F2<Tnew, T, Tnew> f, Tnew initialValue)
    {
        Tnew value = initialValue;
        for (T el: s) {
            value = f.apply(value, el);
        }
        return value;
    }

    /** "with" */
    public L<T> wth(Lang.C<L<T>> f) {
        f.accept(this);
        return this;
    }
    /** group values by passed function. could be used to get read of duplicate values */
    public Dict<L<T>> grp(Lang.F<T, String> getHash)
    {
        LinkedHashMap<String, L<T>> grouped = new LinkedHashMap<>();
        for (T val: this) {
            String hash = getHash.apply(val);
            if (!grouped.containsKey(hash)) {
                grouped.put(hash, Lang.list());
            }
            grouped.get(hash).add(val);
        }
        return new Dict<>(grouped);
    }
    /** "group thru optional - skip null keys" */
    public Dict<L<T>> gop(Lang.F<T, Opt<String>> getHash)
    {
        return fop(v -> getHash.apply(v).map(k -> Lang.T2(k,v)))
            .grp(p -> p.a).map(pairs -> pairs.map(p -> p.b));
    }

    /** stands for "concatenate" */
    public L<T> cct(List<T> more)
    {
        return Lang.list(this.s, more).fap(a -> a);
    }

    public L<T> cct(Opt<T> more)
    {
        return cct(more.fap(a -> Lang.list(a)));
    }

    public <U extends Comparable> L<T> srt(Lang.F<T, U> makeValue)
    {
        L<U> weights = map(makeValue);
        L<Integer> indexes = Tls.range(0, size());
        indexes.sort(Comparator.comparing(weights::get));
        return indexes.map(idx -> s.get(idx));
    }

    /** for "reverse" */
    public L<T> rvr()
    {
        L<T> reversed = Lang.L();
        for (int i = size() - 1; i >= 0; --i) {
            reversed.add(get(i));
        }
        return reversed;
    }

    /** for "take away" - take elements that match a predicate and remove them from this list */
    public <Tnew> L<Tnew> tkw(Lang.F<T, Opt<Tnew>> convert)
    {
        L<Tnew> takenFromEnd = Lang.L();
        for (int i = size() - 1; i >=0; --i) {
            Opt<Tnew> converted = convert.apply(get(i));
            if (converted.has()) {
                takenFromEnd.add(converted.unw());
                remove(i);
            }
        }
        return takenFromEnd.rvr();
    }

    public L<T> sub(int start, int length)
    {
        if (length < 0) {
            length = this.size() - start + length;
        }
        L<T> subList = new L<>();
        int ceil = Math.min(start + length, this.size());
        for (int i = start; i < ceil; ++i) {
            subList.add(this.get(i));
        }
        return subList;
    }

    public L<T> sub(int start)
    {
        return sub(start, this.size() - start);
    }

    /** for "dict" - maps each value to a key-value pair using the passed function */
    public <Tnew> Dict<Tnew> dct(Lang.F<T, Lang.T2<String, Tnew>> makeKey)
    {
        return new Dict<>(this.map(makeKey));
    }

    /** make dict with same values mapped by key returned by the func */
    public Dict<T> key(Lang.F<T, String> makeKey)
    {
        return dct((v) -> Lang.T2(makeKey.apply(v), v));
    }

    public static <Ts> L<Ts> fst(Lang.S<L<Ts>>... attempts)
    {
        for (Lang.S<L<Ts>> a: attempts) {
            L<Ts> result = a.get();
            if (result.size() > 0) {
                return result;
            }
        }
        return Lang.list();
    }

    public It<T> itr()
    {
        return new It<>(s);
    }
}
