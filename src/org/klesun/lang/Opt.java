package org.klesun.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import static org.klesun.lang.Lang.*;

/**
 * very similar stuff to Java-s Optional,
 * but requires less characters to use it
 * and some additional convenient methods
 *
 * if value is null, Opt is empty
 */
public class Opt<T> implements Iterable<T>
{
    final private boolean has;
    final private T value;

    /**
     * @param value - if null, Opt is empty
     */
    public Opt(T value)
    {
        this.has = value != null;
        this.value = value;
    }

    public Opt(T value, boolean has)
    {
        this.has = has;
        this.value = value;
    }

    /** is value present? */
    public boolean has()
    {
        return has;
    }

    /** transform value if present */
    public <T1> Opt<T1> map(Lang.F<T, T1> f)
    {
        if (has()) {
            return new Opt<>(f.apply(value));
        } else {
            return new Opt<>(null);
        }
    }

    public Opt<T> flt(Lang.F<T, Boolean> f)
    {
        return map(v -> f.apply(v) ? v : null);
    }

    /** Flat Map - to combine Opt-s
     * just fap() should be enough since Opt is iterable, but it
     * does not infer properly with method references */
    public <T1> Opt<T1> fop(Lang.F<T, Opt<T1>> f)
    {
        return map(f).uni(
            (opt) -> opt,
            () -> new Opt<>(null)
        );
    }

    public L<T> arr()
    {
        return L(fap(a -> list(a)));
    }

    /** transform opt to array */
    public <Tnew> It<Tnew> fap(F<T, Iterable<Tnew>> f)
    {
        return uni((val) -> It(f.apply(val)), () -> It.non());
    }

    public It<T> itr()
    {
        return It(arr());
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
            throw new NoSuchElementException("Tried to unwrap when value not present");
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

    public static <Ts> Opt<Ts> fst(S<Opt<Ts>> ... attempts)
    {
        for (S<Opt<Ts>> a: attempts) {
            Opt<Ts> result = a.get();
            if (result.has()) {
                return result;
            }
        }
        return new Opt<>(null);
    }

    @Override
    public Iterator<T> iterator() {
        return itr().iterator();
    }

    /** returned by .thn() - needed since i want to limit chaining to end
     * on .thn() cuz else it's very easy to make a mistake with brackets */
    private static class Then
    {
        // TODO: implement!
    }

    public boolean equals(Object other)
    {
        return opt(other)
            .fop(that -> Tls.cast(Opt.class, that))
            .flt(that -> {
                if (!this.has()) return !that.has();
                if (!that.has()) return false;
                return Objects.equals(this.unw(), that.unw());
            })
            .has();
    }
}
