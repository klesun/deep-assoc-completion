package org.klesun.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * a wrapper for an iterator, that remembers all retrieved elements
 * and reuses them on the next attempt to iterate over it
 */
public class MemoizingIterable<A> implements Iterable<A>
{
    Node head = new Node(null); // first value will be skipped
    final Iterator<A> source;

    public MemoizingIterable(Iterator<A> source)
    {
        this.source = source;
    }

    public Iterator<A> iterator()
    {
        return new Iterator<A>() {
            Node current = head;
            public boolean hasNext() {
                try {
                    return current.next != null
                        // getting stackoverflow here
                        || source.hasNext();
                } catch (StackOverflowError exc) {
//                    System.out.println("zalupa overflow iterator has next " + source.toString());
//                    System.out.println(Tls.substr(Tls.getStackTrace(), -10000));
                    throw exc;
                }
            }
            public A next() {
                if (current.next != null) {
                    // look further
                } else if (source.hasNext()) {
                    A value = source.next();
                    current.next = new Node(value);
                } else {
                    throw new NoSuchElementException("loh");
                }
                current = current.next;
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
}
