package org.klesun.lang;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiElement;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * put here wrappers for things you use extremely
 * frequently like lists, dicts, functions, etc...
 */
public class Lang
{
    // tuple of 2 elements
    public static class T2<T1, T2>
    {
        final public T1 a;
        final public T2 b;

        public T2(T1 a, T2 b)
        {
            this.a = a;
            this.b = b;
        }
    }

    // tuple of 3 elements
    public static class T3<T1, T2, T3>
    {
        final public T1 a;
        final public T2 b;
        final public T3 c;

        public T3(T1 a, T2 b, T3 c)
        {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    public static class Mutable<T>
    {
        private T value;

        public Mutable(T value)
        {
            this.value = value;
        }

        public T set(T value)
        {
            return this.value = value;
        }

        public T get()
        {
            return value;
        }
    }

    public interface S<T> extends Supplier<T> {}
    public interface R extends Runnable {}
    public interface F<Tin, Tout> extends Function<Tin, Tout> {}
    public interface F2<Tin1, Tin2, Tout> extends BiFunction<Tin1, Tin2, Tout> {}
    public interface C<Tin> extends Consumer<Tin> {}
    public interface C2<Tin1, Tin2> extends BiConsumer<Tin1, Tin2> {}

    // following methods are supposed to be used after extending
    // this class to be able to use them without `new ` or `ClassName.`

    public static String substr(String str, int startIndex, int endIndex)
    {
        return str.length() > 0
            ? str.substring(startIndex, Math.min(str.length(), endIndex))
            : "";
    }

    public static String substr(String str, int startIndex)
    {
        return substr(str, startIndex, str.length());
    }

    /** i HATE writing "new " before every usage! */
    public static <T> Opt<T> opt(T value)
    {
        return new Opt(value);
    }

    public static <T> Opt<T> getKey(Map<String, T> dict, String key)
    {
        if (dict.containsKey(key)) {
            return opt(dict.get(key));
        } else {
            return opt(null);
        }
    }

    public static <T> S<T> S(S<T> getter)
    {
        return getter;
    }

    public static <Ta, Tb> T2<Ta, Tb> T2(Ta a, Tb b)
    {
        return new T2<Ta, Tb>(a, b);
    }

    public static <T> L<T> list(T... args)
    {
        L<T> result = L();
        Collections.addAll(result, args);
        return result;
    }

    public static <T, Tin extends PsiElement> Lang.F<Tin, Opt<T>> toCast(Class<T> cls)
    {
        return obj -> Tls.cast(cls, obj);
    }

    public static void debug(String msg, Object data)
    {
        /** @debug */
        System.out.println(msg + "\n" + new GsonBuilder()
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return PsiElement.class.isAssignableFrom(fieldAttributes.getDeclaringClass());
                }
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            })
            .create().toJson(data));
    }

    /** L stands for List */
    public static <T> L<T> L(List<T> source)
    {
        return new L<T>(source);
    }

    public static <T> L<T> L(T[] source)
    {
        return new L<T>(new ArrayList<T>(Arrays.asList(source)));
    }

    public static <T> L<T> L(Collection<T> source)
    {
        return new L(new ArrayList(source));
    }

    public static <T> L<T> L(Iterable<T> source)
    {
        List<T> list = list();
        source.forEach(list::add);
        return new L(list);
    }

    public static <T> L<T> L()
    {
        return new L<T>(new ArrayList<T>());
    }

    /**
     * shorter names; convenient map(), filter() right inside
     * the class (yeah, less performance, but easier to read)
     * some integration with Opt class
     */
    public static class L<@NonNull T> extends ListWrapper<T> implements List<T>
    {
        private L(List<T> source)
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
            return index >= 0 && index < s.size() ? opt(s.get(index)) : opt(null);
        }

        public Opt<T> fst()
        {
            return gat(0);
        }

        public Opt<T> lst()
        {
            return gat(-1);
        }

        public L<T> flt(Predicate<T> pred)
        {
            return L(s.stream().filter(pred).collect(Collectors.toList()));
        }

        public <@NonNull Tnew> L<Tnew> map(F<T, Tnew> f)
        {
            return this.map((el, i) -> f.apply(el));
        }

        public <@NonNull Tnew> L<Tnew> map(F2<T, Integer, Tnew> f)
        {
            L<Tnew> result = L();
            for (int i = 0; i < s.size(); ++i) {
                T el = s.get(i);
                result.add(f.apply(el, i));
            }
            return result;
        }

        public <Tnew> L<Tnew> mop(F<T, Tnew> f)
        {
            return this.mop((el, i) -> f.apply(el));
        }

        /** same as map, but also removes null values from the result */
        public <Tnew> L<Tnew> mop(F2<T, Integer, Tnew> f)
        {
            L<Tnew> result = L();
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
        public <Tnew> L<Tnew> fop(F<T, Opt<Tnew>> convert)
        {
            return fop((el, i) -> convert.apply(el));
        }

        public <Tnew> L<Tnew> fop(F2<T, Integer, Opt<Tnew>> convert)
        {
            List<Tnew> result = list();
            for (int i = 0; i < s.size(); ++i) {
                convert.apply(s.get(i), i).thn(result::add);
            }
            return new L(result);
        }

        /**
         * "fop" stands for flat map - flattens list by lambda
         */
        public <Tnew> L<Tnew> fap(F<T, List<Tnew>> flatten)
        {
            return fap((el, i) -> flatten.apply(el));
        }

        public <Tnew> L<Tnew> fap(F2<T, Integer, List<Tnew>> flatten)
        {
            List<Tnew> result = list();
            for (int i = 0; i < s.size(); ++i) {
                result.addAll(flatten.apply(s.get(i), i));
            }
            return new L(result);
        }

        public boolean any(Predicate<T> pred)
        {
            return flt(pred).size() > 0;
        }

        public boolean all(F2<T, Integer, Boolean> pred)
        {
            return map(pred).flt(b -> b).size() == this.size();
        }

        /**
         * you can read it as "wide map" or "wrap", either  way it does the
         * opposite of "fop" - it takes this list and makes something else
         * it is often handy since declaring a var in php is too verbose to be usable
         */
        public <Tnew> Tnew wap(F<L<T>, Tnew> wrapper)
        {
            return wrapper.apply(this);
        }

        /**
         * "fch" stands for For Each
         */
        public void fch(C<T> f)
        {
            fch((el, i) -> f.accept(el));
        }

        public void fch(C2<T, Integer> f)
        {
            for (int i = 0; i < s.size(); ++i) {
                f.accept(s.get(i), i);
            }
        }

        /** "rdc" stands for "reduce" */
        public <Tnew> Tnew rdc(F2<Tnew, T, Tnew> f, Tnew initialValue)
        {
            Tnew value = initialValue;
            for (T el: s) {
                value = f.apply(value, el);
            }
            return value;
        }

        /** @debug */
        public L<T> wth(C<L<T>> f)
        {
            f.accept(this);
            return this;
        }

        /** group values by passed function. could be used to get read of duplicate values */
        public L<L<T>> grp(F<T, String> getHash)
        {
            LinkedHashMap<String, L<T>> grouped = new LinkedHashMap<>();
            for (T val: this) {
                String hash = getHash.apply(val);
                if (!grouped.containsKey(hash)) {
                    grouped.put(hash, list());
                }
                grouped.get(hash).add(val);
            }
            return L(grouped.values());
        }

        /** stands for "concatenate" */
        public L<T> cct(List<T> more)
        {
            return list(this.s, more).fap(a -> a);
        }

        public L<T> srt(F<T, String> makeValue)
        {
            L<String> weights = map(makeValue);
            L<Integer> indexes = Tls.range(0, size());
            indexes.sort(Comparator.comparing(weights::get));
            return indexes.map(idx -> s.get(idx));
        }

        /** for "reverse" */
        public L<T> rvr()
        {
            L<T> reversed = L();
            for (int i = size() - 1; i >= 0; --i) {
                reversed.add(get(i));
            }
            return reversed;
        }

        /** for "take away" - take elements that match a predicate and remove them from this list */
        public <Tnew> L<Tnew> tkw(F<T, Opt<Tnew>> convert)
        {
            L<Tnew> takenFromEnd = L();
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
    }

    /**
     * takes a List interface instance, stores it in a
     * field and implements List interface itself using it
     *
     * intended to be extended by a class that would provide
     * more convenient functions to work with list
     */
    private static class ListWrapper<T> implements List<T>
    {
        public final List<T> s;
        private ListWrapper(List<T> source)
        {
            this.s = source;
        }

        public int size() {return s.size();}
        public boolean isEmpty() {return s.isEmpty();}
        public boolean contains(Object o) {return s.contains(o);}
        public Iterator<T> iterator() {return s.iterator();}
        public Object[] toArray() {return s.toArray();}
        public <T1> T1[] toArray(T1[] a) {return s.toArray(a);}
        public boolean add(T t) {return s.add(t);}
        public boolean remove(Object o) {return s.remove(o);}
        public boolean containsAll(Collection<?> c) {return s.containsAll(c);}
        public boolean addAll(Collection<? extends T> c) {return s.addAll(c);}
        public boolean addAll(int index, Collection<? extends T> c) {return s.addAll(index, c);}
        public boolean removeAll(Collection<?> c) {return s.removeAll(c);}
        public boolean retainAll(Collection<?> c) {return s.retainAll(c);}
        public void clear() {s.clear();}
        public T get(int index) {
            if (index >= 0) {
                return s.get(index);
            } else {
                return s.get(s.size() + index);
            }
        }
        public T set(int index, T element) {return s.set(index, element);}
        public void add(int index, T element) {s.add(index, element);}
        public T remove(int index) {return s.remove(index);}
        public int indexOf(Object o) {return s.indexOf(o);}
        public int lastIndexOf(Object o) {return s.lastIndexOf(o);}
        public ListIterator<T> listIterator() {return s.listIterator();}
        public ListIterator<T> listIterator(int index) {return s.listIterator(index);}
        public List<T> subList(int fromIndex, int toIndex) {return s.subList(fromIndex, toIndex);}
    }
}
