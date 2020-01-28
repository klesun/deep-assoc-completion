package org.klesun.lang;

import java.util.*;
import java.util.function.*;

/**
 * put here wrappers for things you use extremely
 * frequently like lists, dicts, functions, etc...
 */
public class Lang
{
    // tuple of 2 elements
    public static class T2<Ta, Tb>
    {
        final public Ta a;
        final public Tb b;

        public T2(Ta a, Tb b)
        {
            this.a = a;
            this.b = b;
        }

        public <Tnew> Tnew nme(F2<Ta, Tb, Tnew> namer)
        {
            return namer.apply(a,b);
        }

        public static <Ta, Tb> Opt<T2<Ta, Tb>> all(Opt<Ta> aOpt, Opt<Tb> bOpt) {
            if (aOpt.has() && bOpt.has()) {
                return som(T2(aOpt.unw(), bOpt.unw()));
            } else {
                return non();
            }
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

        public <Tnew> Tnew nme(F3<T1, T2, T3, Tnew> namer)
        {
            return namer.apply(a,b,c);
        }

        public void nme(C3<T1, T2, T3> namer)
        {
            namer.accept(a,b,c);
        }
    }

    // tuple of 4 elements
    public static class T4<T1, T2, T3, T4>
    {
        final public T1 a;
        final public T2 b;
        final public T3 c;
        final public T4 d;

        public T4(T1 a, T2 b, T3 c, T4 d)
        {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public <Tnew> Tnew nme(F4<T1, T2, T3, T4, Tnew> namer)
        {
            return namer.apply(a,b,c,d);
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

    @FunctionalInterface
    public interface F3<T,U,S, R> {
        R apply(T t, U u, S s);
    }

    @FunctionalInterface
    public interface C3<T,U,S> {
        void accept(T t, U u, S s);
    }

    @FunctionalInterface
    public interface F4<Tin1, Tin2, Tin3, Tin4, Tout> {
        Tout apply(Tin1 var1, Tin2 var2, Tin3 var3, Tin4 var4);
    }

    public interface C<Tin> extends Consumer<Tin> {}
    public interface C2<Tin1, Tin2> extends BiConsumer<Tin1, Tin2> {}

    // following methods are supposed to be used after extending
    // this class to be able to use them without `new ` or `ClassName.`

    public static String repeat(String str, int n)
    {
        String result = "";
        for (int i = 0; i < n; ++i) {
            result += str;
        }
        return result;
    }

    public static String substr(String str, int startIndex, int endIndex)
    {
        if (startIndex < 0) {
            startIndex = str.length() + startIndex;
        }
        startIndex = Math.max(0, startIndex);

        if (endIndex < 0) {
            endIndex = str.length() + endIndex;
        }
        endIndex = Math.min(str.length(), endIndex);

        return str.length() > 0 && startIndex < endIndex
            ? str.substring(startIndex, endIndex)
            : "";
    }

    public static String substr(String str, int startIndex)
    {
        return substr(str, startIndex, str.length());
    }

    /** i HATE writing "new " before every usage! */
    public static <T> Opt<T> opt(T value)
    {
        return new Opt<>(value);
    }

    public static <T> Opt<T> som(T value)
    {
        return new Opt(value, true);
    }

    public static <T> Opt<T> non()
    {
        return new Opt(null, false);
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
    public static <Ta, Tb, Tc> T3<Ta, Tb, Tc> T3(Ta a, Tb b, Tc c)
    {
        return new T3<Ta, Tb, Tc>(a, b, c);
    }
    public static <Ta, Tb, Tc, Td> T4<Ta, Tb, Tc, Td> T4(Ta a, Tb b, Tc c, Td d)
    {
        return new T4<Ta, Tb, Tc, Td>(a, b, c, d);
    }

    public static <T> L<T> list(T... args)
    {
        L<T> result = L();
        Collections.addAll(result, args);
        return result;
    }

    public static <T extends Tin, Tin> Lang.F<Tin, Opt<T>> toCast(Class<T> cls)
    {
        return obj -> Tls.cast(cls, obj);
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

    public static <T> L<T> L(Iterable<T> source)
    {
        List<T> list = list();
        source.forEach(list::add);
        return new L<>(list);
    }

    public static <T> L<T> L()
    {
        return new L<T>(new ArrayList<T>());
    }

    public static <T> It<T> It(Iterable<T> source)
    {
        return new It<>(source);
    }

    public static <T> It<T> It(T[] source)
    {
        return new It<>(source);
    }

    public static <T> It<T> It()
    {
        return new It<>(list());
    }

    /**
     * will have special behaviour in places where
     * we have a getter with already resolved value
     */
    public static <T> S<T> Granted(T value)
    {
        // return () -> value;
        return new Granted<>(value);
    }

    public static class Granted<T> implements Tls.IOnDemand<T>
    {
        final private T value;

        public Granted(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        @Override
        public Opt<T> ifHas() {
            return som(value);
        }
    }
}
