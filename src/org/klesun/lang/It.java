package org.klesun.lang;

import java.util.Arrays;
import java.util.Iterator;
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
    final private Stream<A> source;

    public It(Stream<A> sourceStream)
    {
        this.source = sourceStream;
    }

    public It(Iterable<A> sourceIter)
    {
        this(StreamSupport.stream(sourceIter.spliterator(), false));
    }

    public It(A[] source)
    {
        this(Arrays.stream(source));
    }

    public static <B> It<B> cct(Iterable<B>... args)
    {
        return new It<>(Arrays.stream(args)
            .flatMap(iter -> StreamSupport.stream(iter.spliterator(), false)));
    }

    public <B> It<B> map(F<A, B> mapper)
    {
        return new It<>(source.map(mapper));
    }

    public It<A> flt(Predicate<A> pred)
    {
        return new It<>(source.filter(pred));
    }

    public <B> It<B> fop(F<A, Opt<B>> convert)
    {
        return new It<>(source.map(convert)
            .flatMap(a -> a.arr().stream()));
    }

    public <B> It<B> fap(F<A, Iterable<B>> flatten)
    {
        return new It<>(source.map(flatten)
            .flatMap(a -> StreamSupport.stream(a.spliterator(), false)));
    }

    public Opt<A> fst()
    {
        return source.findFirst()
            .map(val -> opt(val))
            .orElse(opt(null));
    }

    public L<A> arr()
    {
        return L(this);
    }

    // end stream at element that matches the predicate
    // includes the element that returned true
    public It<A> end(Predicate<A> pred)
    {
        Mutable<Boolean> ended = new Mutable<>(false);
        return new It<>(source.filter(el -> {
            boolean isEnd = ended.get();
            ended.set(pred.test(el));
            return !isEnd;
        }));
    }

    public Iterator<A> iterator()
    {
        return source.iterator();
    }
}
