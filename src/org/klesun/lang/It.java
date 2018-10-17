package org.klesun.lang;

import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.iterators.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.klesun.lang.Lang.*;

/**
 * similar to L, but works with an iterator over elements rather
 * than on a complete array, taking each next element on demand
 *
 * supposed to be significantly faster thanks to map/filter/etc not
 * recreating the array as well as the ability to progress step by step
 * to distribute processor time evenly among different type sources
 */
public class It<A> implements Iterable<A>
{
    private Opt<Iterator<A>> iterator = Lang.non();
    private Iterable<A> source;
    private Opt<Exception> createdAt = Lang.non();
    private Opt<Exception> disposedAt = Lang.non();
    private boolean disposed = false;

    public It(Iterable<A> sourceIter)
    {
        if (SearchContext.DEBUG_DEFAULT) {
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

    public static <B> It<B> frs(S<Iterable<B>>... args)
    {
        for (S<Iterable<B>> itb: args) {
            Iterator<B> itr = itb.get().iterator();
            if (itr.hasNext()) {
                return It(() -> itr);
            }
        }
        return non();
    }

    public static <B> It<B> cnc(Iterable<B>... args)
    {
        // my implementation of these functions seems to be slower than built-in Stream-s, but I can't
        // make Stream versions to actually return a lazy iterator without instantly resolving all values
        return It(args).fap(ble -> ble);
//        return new It<>(Arrays.stream(args)
//            .flatMap(iter -> StreamSupport.stream(iter.spliterator(), false)));
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
        if (SearchContext.DEBUG_DEFAULT) {
            disposedAt = som(new Exception());
        }
        Iterator<A> iter = getIterator();
        disposed = true;
        this.iterator = Lang.non();
        return iter;
    }

    private Stream<A> disposeStream()
    {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(dispose(), Spliterator.ORDERED),
            false);
    }

    public It<A> cct(Iterable<A> more)
    {
        return It.cnc(this, more);
    }

    public <B> It<B> map(F<A, B> mapper)
    {
        return map((el, i) -> mapper.apply(el));
        //return new It<>(disposeStream().map(mapper));
    }

    public <B> It<B> map(F2<A, Integer, B> mapper)
    {
        Iterator<B> mator = new MapIterator<>(dispose(), mapper);
        return new It<>(() -> mator);
//        Mutable<Integer> mutI = new Mutable<>(0);
//        return new It<>(disposeStream().map(el -> {
//            int i = mutI.get();
//            mutI.set(i + 1);
//            return mapper.apply(el, i);
//        }));
    }

    public It<A> flt(F2<A, Integer, Boolean> pred)
    {
        Iterator<A> fitor = new FilterIterator<>(dispose(), pred);
        return new It<>(() -> fitor);
//        return new It<>(disposeStream().filter(el -> {
//            int i = mutI.get();
//            mutI.set(i + 1);
//            return pred.apply(el, i);
//        }));
    }

    public It<A> flt(Predicate<A> pred)
    {
        return flt((el, i) -> pred.test(el));
    }

    public <B> It<B> fap(F2<A, Integer, Iterable<B>> flatten)
    {
        Iterator<B> fator = new FlatMapIterator<>(dispose(), flatten);
        return It(() -> fator);
//        Mutable<Integer> mutI = new Mutable<>(0);
//        return new It<>(disposeStream()
//            .map(el -> {
//                int i = mutI.get();
//                mutI.set(i + 1);
//                return flatten.apply(el, i);
//            })
//            .flatMap(a -> StreamSupport.stream(a.spliterator(), false)));
    }

    public <B> It<B> fap(F<A, Iterable<B>> flatten)
    {
        return fap((el, i) -> flatten.apply(el));
    }

    public <B> It<B> fop(F2<A, Integer, Opt<B>> convert)
    {
        return map(convert).fap(a -> a.itr());
    }

    public <B> It<B> fop(F<A, Opt<B>> convert)
    {
        return map(convert).fap(a -> a.itr());
    }

    public It<A> unq()
    {
        Set<A> occurences = new HashSet<>();
        return flt(t -> {
            if (occurences.contains(t)) {
                return false;
            } else {
                occurences.add(t);
                return true;
            }
        });
    }

    public It<A> unq(F<A, Object> getHash)
    {
        Set<Object> occurences = new HashSet<>();
        return flt(t -> {
            Object hash = getHash.apply(t);
            if (occurences.contains(hash)) {
                return false;
            } else {
                occurences.add(hash);
                return true;
            }
        });
    }

    public It<A> def(Iterable<A> fallback)
    {
        return has() ? this : It(fallback);
    }

    public void fch(C<A> f)
    {
        dispose().forEachRemaining(f);
        //disposeStream().forEach(f);
    }

    // like fch() but executed not at once, but when iterator is actually disposed. for debug
    public It<A> thn(C2<A, Integer> f)
    {
        return flt((el,i) -> {
            f.accept(el, i);
            return true;
        });
    }

    public It<A> thn(C<A> f)
    {
        return thn((el,i) -> f.accept(el));
    }

    public void fch(C2<A, Integer> f)
    {
        Mutable<Integer> mutI = new Mutable<>(0);
        dispose().forEachRemaining(el -> {
            int i = mutI.get();
            mutI.set(i + 1);
            f.accept(el, i);
        });
    }

//    public Opt<A> fst()
//    {
//        return disposeStream().findFirst()
//            .map(val -> opt(val))
//            .orElse(opt(null));
//    }

    public <B> B wap(F<It<A>, B> wrapper)
    {
        return wrapper.apply(this);
    }

    public Opt<A> fst() {
        return has() ? Lang.som(dispose().next()) : Lang.non();
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

    public L<A> arr()
    {
        L<A> arr = L();
        dispose().forEachRemaining(arr::add);
        return arr;
    }

    public boolean any(Predicate<A> pred)
    {
        Iterable<A> iter = this::dispose;
        for (A el: iter) {
            if (pred.test(el)) {
                return true;
            }
        }
        return false;
    }

    public boolean all(F2<A, Integer, Boolean> pred)
    {
        Iterable<A> iter = this::dispose;
        int i = 0;
        for (A el: iter) {
            if (!pred.apply(el, i++)) {
                return false;
            }
        }
        return true;
    }

    public boolean all(Predicate<A> pred)
    {
        return all((el, i) -> pred.test(el));
    }

    // end stream the moment endPred returns true (in case of timeout for example)
    // includes the value on which endPred returned true
    public It<A> end(F<A, Boolean> endPred)
    {
        Iterator<A> endor = new EndIterator<>(dispose(), endPred);
        return new It<>(() -> endor);
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
}
