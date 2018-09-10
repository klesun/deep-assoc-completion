package org.klesun.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * takes a List interface instance, stores it in a
 * field and implements List interface itself using it
 *
 * intended to be extended by a class that would provide
 * more convenient functions to work with list
 */
public class ListWrapper<T> implements List<T>
{
    public final List<T> s;
    public ListWrapper(List<T> source)
    {
        this.s = source;
    }

    public int hashCode() {return s.hashCode();}
    public boolean equals(Object other) {
        return Tls.cast(ListWrapper.class, other)
            .flt(that -> that.s.equals(this.s))
            .has();
    }
    public int size() {return s.size();}
    public boolean isEmpty() {return s.isEmpty();}
    public boolean contains(Object o) {return s.contains(o);}
    @NotNull
    public Iterator<T> iterator() {return s.iterator();}
    @NotNull public Object[] toArray() {return s.toArray();}
    @NotNull public <T1> T1[] toArray(T1[] a) {return s.toArray(a);}
    public boolean add(T t) {return s.add(t);}
    public boolean remove(Object o) {return s.remove(o);}
    public boolean containsAll(Collection<?> c) {return s.containsAll(c);}
    public boolean addAll(Collection<? extends T> c) {return s.addAll(c);}
    public boolean addAll(int index, Collection<? extends T> c) {return s.addAll(index, c);}
    public boolean removeAll(Collection<?> c) {return s.removeAll(c);}
    public boolean retainAll(Collection<?> c) {return s.retainAll(c);}
    public void clear() {s.clear();}
    public T get(int index) {
        if (index >= 0) {
            return s.get(index);
        } else {
            return s.get(s.size() + index);
        }
    }
    public T set(int index, T element) {return s.set(index, element);}
    public void add(int index, T element) {s.add(index, element);}
    public T remove(int index) {return s.remove(index);}
    public int indexOf(Object o) {return s.indexOf(o);}
    public int lastIndexOf(Object o) {return s.lastIndexOf(o);}
    @NotNull public ListIterator<T> listIterator() {return s.listIterator();}
    @NotNull public ListIterator<T> listIterator(int index) {return s.listIterator(index);}
    @NotNull public List<T> subList(int fromIndex, int toIndex) {return s.subList(fromIndex, toIndex);}
}
