package org.klesun.lang;

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
    public <T1> Opt<T1> map(Lang.F<T, T1> f)
    {
        if (has()) {
            return new Opt(f.apply(value));
        } else {
            return new Opt(null);
        }
    }

    public Opt<T> flt(Lang.F<T, Boolean> f)
    {
        return map(v -> f.apply(v) ? v : null);
    }

    /** Flat Map - to combine Opt-s */
    public <T1> Opt<T1> fap(Lang.F<T, Opt<T1>> f)
    {
        return map(f).uni(
            (opt) -> opt,
            () -> new Opt(null)
        );
    }

    /**
     * run if present
     * TODO: it should not return an optional since this causes confusion with nested .thn-s
     * instead it should return an object with single property - .els()
     */
    public Opt<T> thn(Lang.C<T> f)
    {
        if (has()) {
            f.accept(value);
        }
        return this;
    }

    /** run if absent */
    public Opt<T> els(Lang.R f)
    {
        if (!has()) {
            f.run();
        }
        return this;
    }

    /** "elf" stands for "else if"
     * call f if no value */
    public Opt<T> elf(Lang.S<Opt<T>> f)
    {
        return !has() ? f.get() : this;
    }

    /** stands for "unwrap" - get value or throw exception */
    public T unw()
    {
        if (has()) {
            return value;
        } else {
            throw new RuntimeException("Tried to unwrap when value not present");
        }
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
    public <Tuni> Tuni uni(Lang.F<T, Tuni> thn, Lang.S<Tuni> els)
    {
        if (has()) {
            return thn.apply(value);
        } else {
            return els.get();
        }
    }

    /** return result of fst successful function in the passed list */
    public static <Ts> Opt<Ts> fst(Iterable<Opt<Ts>> attempts)
    {
        for (Opt<Ts> a: attempts) {
            if (a.has()) {
                return a;
            }
        }
        return new Opt(null);
    }

    /** returned by .thn() - needed since i want to limit chaining to end
     * on .thn() cuz else it's very easy to make a mistake with brackets */
    private static class Then
    {
        // TODO: implement!
    }
}
