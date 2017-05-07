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

    public interface S<T> extends Supplier<T> {};
    public interface R extends Runnable {};
    public interface F<Tin, Tout> extends Function<Tin, Tout> {};
    public interface F2<Tin1, Tin2, Tout> extends BiFunction<Tin1, Tin2, Tout> {};
    public interface C<Tin> extends Consumer<Tin> {};
    public interface C2<Tin1, Tin2> extends BiConsumer<Tin1, Tin2> {};

    // following methods are supposed to be used after extending
    // this class to be able to use them without `new ` or `ClassName.`

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

    public static <T> ArrayList<T> list(T... args)
    {
        ArrayList<T> result = new ArrayList<>();
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
        return new L(new ArrayList<T>(Arrays.asList(source)));
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

    public static class L<T>
    {
        public final List<T> s;

        private L(List<T> source)
        {
            this.s = source;
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

        public <Tnew> L<Tnew> map(F<T, Tnew> pred)
        {
            return L(s.stream().map(pred).collect(Collectors.toList()));
        }

        /**
         * "fop" stands for "Filter Optional"
         * this is a combination of map and filter
         */
        public <Tnew> L<Tnew> fop(F<T, Opt<Tnew>> convert)
        {
            List<Tnew> result = list();
            s.forEach(el -> convert.apply(el).thn(result::add));
            return new L(result);
        }

        /**
         * "fap" stands for flat map - flattens list by lambda
         */
        public <Tnew> L<Tnew> fap(F<T, List<Tnew>> flatten)
        {
            List<Tnew> result = list();
            s.forEach(el -> flatten.apply(el).forEach(result::add));
            return new L(result);
        }

        /**
         * "fch" stands for For Each
         */
        public void fch(C<T> f)
        {
            s.forEach(f);
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
    }
}
