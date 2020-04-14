package org.klesun.lang;

import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.lang.iterators.ArrayIterator;
import org.klesun.lang.iterators.ThenIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.klesun.lang.Lang.It;
import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.*;

/**
 * similar to L, but works with an iterator over elements rather
 * than on a complete array, taking each next element on demand
 *
 * supposed to be significantly faster thanks to map/filter/etc not
 * recreating the array as well as the ability to progress step by step
 * to distribute processor time evenly among different type sources
 */
public class It<A> implements IIt<A>
{
    private Opt<Iterator<A>> iterator = Lang.non();
    private Iterable<A> source;
    private Opt<Exception> createdAt = Lang.non();
    private Opt<Exception> disposedAt = Lang.non();
    private boolean disposed = false;

    public It(Iterable<A> sourceIter)
    {
        if (SearchCtx.DEBUG_DEFAULT) {
            createdAt = som(new Exception("here iterator was created"));
        }
        this.source = sourceIter;
    }

    public It(A[] source)
    {
        this(() -> new ArrayIterator<>(source));
    }

    public static <B> It<B> non()
    {
        return new It<>(list());
    }

    public static <B> It<B> frs(S<? extends Iterable<B>>... args)
    {
        return It(args)
            .map(ble -> ble.get().iterator())
            .flt(tor -> tor.hasNext())
            .lmt(1)
            .fap(tor -> () -> tor);
    }

    public static <B> It<B> cnc(Iterable<B>... args)
    {
        // my implementation of these functions seems to be slower than built-in Stream-s, but I can't
        // make Stream versions to actually return a lazy iterator without instantly resolving all values
        return It(args).fap(ble -> ble);
    }

    /** don't use this unless you just want to check if it is empty */
    private Iterator<A> getIterator()
    {
        if (!iterator.has()) {
            iterator = som(source.iterator());
        }
        return iterator.unw();
    }

    private Iterator<A> dispose()
    {
        if (disposed) {
            RuntimeException exc = new NoSuchElementException("Tried to re-use disposed iterator");
            disposedAt.thn(exc::initCause);
            throw exc;
        }
        if (SearchCtx.DEBUG_DEFAULT) {
            disposedAt = som(new Exception());
        }
        Iterator<A> iter = getIterator();
        disposed = true;
        this.iterator = Lang.non();
        return iter;
    }

    // execute on the first hasNext() call that returns false
    public It<A> thn(C<Integer> f)
    {
        return new It<>(() -> new ThenIterator<>(dispose(), f));
    }

    public boolean has()
    {
        if (disposed) {
            RuntimeException exc = new NoSuchElementException("Tried to re-use disposed iterator");
            disposedAt.thn(exc::initCause);
            throw exc;
        }
        return getIterator().hasNext();
    }

    public MemIt<A> mem()
    {
        return new MemIt<>(() -> dispose());
    }

    public void fch(Lang.C2<A, Integer> f)
    {
        Lang.Mutable<Integer> mutI = new Lang.Mutable<>(0);
        iterator().forEachRemaining(el -> {
            int i = mutI.get();
            mutI.set(i + 1);
            f.accept(el, i);
        });
    }

    public <B> B wap(Lang.F<It<A>, B> wrapper)
    {
        return wrapper.apply(It(this));
    }

    public void fch(Lang.C<A> f)
    {
        iterator().forEachRemaining(f);
    }

    public It<A> cct(Iterable<A> more)
    {
        return It.cnc(this, more);
    }

    public Opt<A> lst() {
        Opt<A> result = opt(null);
        for (A el: this) {
            result = som(el);
        }
        return result;
    }

    public Iterator<A> iterator()
    {
        return dispose();
    }

    /** Don't even think of casting it to the Iterable. Seriously, don't. */
    public Object getSourceHash()
    {
        return source;
    }

    public String toString()
    {
        return "It." + (has() ? "som()" : "non()");
    }
}
