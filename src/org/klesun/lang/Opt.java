package org.klesun.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.lang.shortcuts.C;
import org.klesun.lang.shortcuts.F;
import org.klesun.lang.shortcuts.R;
import org.klesun.lang.shortcuts.S;

/**
 * very similar stuff to Java-s Optional,
 * but requires less characters to use it
 * and some additional convenient methods
 *
 * if value is null, Opt is empty
 */
public class Opt<T>
{
    final private T value;

    /**
     * @param value - if null, Opt is empty
     */
    public Opt(T value)
    {
        this.value = value;
    }

    /** is value present? */
    public boolean has()
    {
        return value != null;
    }

    /** transform value if present */
    public <T1> Opt<T1> map(F<T, T1> f)
    {
        if (has()) {
            return new Opt(f.apply(value));
        } else {
            return new Opt(null);
        }
    }

    /** Flat Map - to combine Opt-s */
    public <T1> Opt<T1> fap(F<T, Opt<T1>> f)
    {
        return map(f).uni(
            (opt) -> opt,
            () -> new Opt(null)
        );
    }

    /** run if present */
    public Opt<T> thn(C<T> f)
    {
        if (has()) {
            f.accept(value);
        }
        return this;
    }

    /** run if absent */
    public Opt<T> els(R f)
    {
        if (!has()) {
            f.run();
        }
        return this;
    }

    public T def(T defaultValue)
    {
        if (has()) {
            return value;
        } else {
            return defaultValue;
        }
    }

    /**
     * reduces both cases to a common type
     * @param thn - you get result of this function if value is present
     * @param els - you get result of this function if value is absent
     */
    public <Tuni> Tuni uni(F<T, Tuni> thn, S<Tuni> els)
    {
        if (has()) {
            return thn.apply(value);
        } else {
            return els.get();
        }
    }

    /** return result of first successful function in the passed list */
    public static <Ts> Opt<Ts> fst(Iterable<Opt<Ts>> attempts)
    {
        for (Opt<Ts> a: attempts) {
            if (a.has()) {
                return a;
            }
        }
        return new Opt(null);
    }
}
