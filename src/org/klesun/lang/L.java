package org.klesun.lang;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * shorter names; convenient map(), filter() right inside
 * the class (yeah, less performance, but easier to read)
 * some integration with Opt class
 */
public class L<@NonNull T> extends ListWrapper<T> implements List<T>, IResolvedIt<T>
{
    final private static L NON = new L(true);

    public static <A> L<A> non()
    {
        return NON;
    }

    L(List<T> source)
    {
        super(source, false);
    }

    public L(boolean immutable)
    {
        super(new ArrayList<>(), immutable);
    }

    public L()
    {
        this(false);
    }

    /** "gat" stands for "get at" */
    public Opt<T> gat(int index)
    {
        if (index < 0) {
            index = s.size() + index;
        }
        return index >= 0 && index < s.size() ? Lang.opt(s.get(index)) : Lang.opt(null);
    }

    public Opt<T> lst()
    {
        return gat(-1);
    }

    public L<T> def(L<T> fallback)
    {
        return size() > 0 ? this : fallback;
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

    public boolean has()
    {
        return size() > 0;
    }

    @Override
    public L<T> arr()
    {
        return this;
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

    public String toString()
    {
        return "[" + Tls.implode(", ", map(a -> a + "")) + "]";
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
        return fop(v -> getHash.apply(v).map(k -> Lang.T2(k,v))).arr()
            .grp(p -> p.a).map(pairs -> pairs.map(p -> p.b).arr());
    }

    /** stands for "concatenate" */
    public L<T> cct(Iterable<T> more)
    {
        return It.cnc(s, more).arr();
    }

    public L<T> cct(Opt<T> more)
    {
        return cct(more.arr());
    }

    public <U extends Comparable> IIt<T> srt(Lang.F<T, U> makeValue)
    {
        L<U> weights = map(makeValue).arr();
        L<Integer> indexes = Tls.range(0, size()).arr();
        indexes.sort(Comparator.comparing(weights::get));
        return indexes.map(s::get);
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
        if (start < 0) {
            start = Math.max(0, this.size() + start);
        }
        return sub(start, this.size() - start);
    }

    /** for "dict" - maps each value to a key-value pair using the passed function */
    public <Tnew> Dict<Tnew> dct(Lang.F<T, Lang.T2<String, Tnew>> makeKey)
    {
        return new Dict<>(this.map(makeKey).arr());
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
        return non();
    }
}
