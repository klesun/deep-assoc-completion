package org.klesun.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

import static org.klesun.lang.Lang.*;

/**
 * very similar stuff to Java-s Optional,
 * but requires less characters to use it
 * and some additional convenient methods
 *
 * if value is null, Opt is empty
 */
public class Opt<T> implements IResolvedIt<T>
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
    @Override
    public <T1> Opt<T1> map(Lang.F<T, T1> f)
    {
        if (has()) {
            return new Opt<>(f.apply(value));
        } else {
            return new Opt<>(null);
        }
    }

    @Override
    public Opt<T> flt(Predicate<T> f)
    {
        return map(v -> f.test(v) ? v : null);
    }

    /**
     * Flat Map - to combine Opt-s
     * just fap() should be enough since Opt is iterable, but it
     * does not infer properly with method references
     * TODO: refactor all usages to cst(SomePsi.class) and fap(() -> ...)
     */
    @Override
    public <T1> Opt<T1> fop(Lang.F<T, Opt<T1>> f)
    {
        return map(f).uni(
            (opt) -> opt,
            () -> new Opt<>(null)
        );
    }

    @Override
    public <T1 extends T> Opt<T1> cst(Class<T1> cls)
    {
        return fap((val) -> Tls.cast(cls, val)).fst();
    }

    @Override
    public Opt<T> unq(Lang.F<T, Object> getHash)
    {
        // optional value is always unique ^_^
        return this;
    }
    @Override
    public Opt<T> unq()
    {
        // optional value is always unique ^_^
        return this;
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

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean done = false;
            public boolean hasNext() {
                return !done && has();
            }
            public T next() {
                if (!done) {
                    done = true;
                    return unw();
                } else {
                    String msg = "Tried to next() a finished Opt iterator";
                    throw new NoSuchElementException(msg);
                }
            }
        };
    }

    public int hashCode()
    {
        return map(val -> val.hashCode()).def(-100);
    }

    public boolean equals(Object other)
    {
        return opt(other)
            .fop(that -> Tls.cast(Opt.class, that))
            .any(that -> {
                if (!this.has()) return !that.has();
                if (!that.has()) return false;
                return Objects.equals(this.unw(), that.unw());
            });
    }

    public String toString()
    {
        return !has() ? "non" : "som(" + unw() + ")";
    }

    @Override
    public int size() {
        return has ? 1 : 0;
    }
}
