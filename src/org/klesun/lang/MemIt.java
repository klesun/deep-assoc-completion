package org.klesun.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.klesun.lang.Lang.It;

/**
 * a wrapper for an iterator, that remembers all retrieved elements
 * and reuses them on the next attempt to iterate over it
 */
public class MemIt<A> implements IIt<A>
{
    final private Node head = new Node(null); // first value will be skipped
    final private Lang.S<Iterator<A>> sourceBle;
    private boolean isNexting = false;

    public MemIt(Iterable<A> sourceBle)
    {
        this.sourceBle = Tls.onDemand(sourceBle::iterator);
    }

    public Iterator<A> iterator()
    {
        Iterator<A> source = sourceBle.get();
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
                    has = current.next != null
                        || source.hasNext();
                } catch (StackOverflowError exc) {
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

    public boolean has() {
        return It(this).has();
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

    public L<A> arr()
    {
        return It(this).arr();
    }

    /** probably should comment after debug cuz if it gets somewhere accidentally, it would be disaster for performance.... */
//    @Override
//    public String toString() {
//        return "MemIt" + arr();
//    }
}
