package org.klesun.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import static org.klesun.lang.Lang.*;

/**
 * a wrapper for an iterator, that remembers all retrieved elements
 * and reuses them on the next attempt to iterate over it
 */
public class MemoizingIterable<A> implements Iterable<A>
{
    final private Node head = new Node(null); // first value will be skipped
    final private Iterator<A> source;
    private boolean isNexting = false;

    public MemoizingIterable(Iterator<A> source)
    {
        this.source = source;
    }

    public Iterator<A> iterator()
    {
        return new Iterator<A>() {
            Node current = head;
            public boolean hasNext() {
                if (isNexting) {
                    // expression resolved through itself results in such recursion in the
                    // iterator. I guess it's safe to answer "empty" on circular reference
                    return false;
                }
                isNexting = true;
                boolean has;
                try {
                    has =  current.next != null
                        // getting stackoverflow here
                        || source.hasNext();
                } catch (StackOverflowError exc) {
//                    System.out.println("zalupa overflow iterator has next " + source.toString());
//                    System.out.println(Tls.substr(Tls.getStackTrace(), -10000));
                    throw exc;
                }
                isNexting = false;
                return has;
            }
            public A next() {
                if (isNexting) {
                    throw new NoSuchElementException("shalava");
                }
                isNexting = true;
                if (current.next != null) {
                    // look further
                } else if (source.hasNext()) {
                    A value = source.next();
                    current.next = new Node(value);
                } else {
                    throw new NoSuchElementException("loh");
                }
                current = current.next;
                isNexting = false;
                return current.value;
            }
        };
    }

    private class Node
    {
        final A value;
        Node next = null;

        public Node(A value)
        {
            this.value = value;
        }
    }

    public <Tnew> It<Tnew> fop(Lang.F<A, Opt<Tnew>> convert)
    {
        return fop((el, i) -> convert.apply(el));
    }

    public <Tnew> It<Tnew> fop(Lang.F2<A, Integer, Opt<Tnew>> convert)
    {
        return It(this).fop(convert);
    }

    /**
     * "fop" stands for flat map - flattens list by lambda
     */
    public <Tnew> It<Tnew> fap(Lang.F<A, Iterable<Tnew>> flatten)
    {
        return fap((el, i) -> flatten.apply(el));
    }

    public <Tnew> It<Tnew> fap(Lang.F2<A, Integer, Iterable<Tnew>> flatten)
    {
        return It(this).fap(flatten);
    }

    public boolean any(Predicate<A> pred)
    {
        return It(this).any(pred);
    }

    public boolean has()
    {
        return It(this).has();
    }

    public It<A> flt(Lang.F2<A, Integer, Boolean> pred)
    {
        return Lang.It(this).flt(pred);
    }

    public It<A> flt(Predicate<A> pred)
    {
        return flt((val, i) -> pred.test(val));
    }

    public <@NonNull Tnew> It<Tnew> map(Lang.F<A, @NonNull Tnew> f)
    {
        return Lang.It(this).map(f);
    }

    public <@NonNull Tnew> It<Tnew> map(Lang.F2<A, Integer, Tnew> f)
    {
        return Lang.It(this).map(f);
    }

    public L<A> arr()
    {
        return It(this).arr();
    }
}
