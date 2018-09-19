package org.klesun.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** a convenient wrapper to Java's LinkedHashMap - with map/filter/toList/etc... methods */
public class Dict<T> implements Map<String, T>
{
    final LinkedHashMap<String, T> subject;

    public Dict(L<Lang.T2<String, T>> entries)
    {
        subject = new LinkedHashMap<>();
        entries.fch(t -> subject.put(t.a, t.b));
    }

    public Dict(Map<String, T> entries)
    {
        subject = new LinkedHashMap<>(entries);
    }

    public <Tnew> Dict<Tnew> map(Lang.F2<T, String, Tnew> f) {
        return new Dict<>(Lang.L(entrySet()).map(e -> Lang.T2(e.getKey(), f.apply(e.getValue(), e.getKey()))).arr());
    }
    public <Tnew> Dict<Tnew> map(Lang.F<T, Tnew> f) {
        return this.map((v,k) -> f.apply(v));
    }
    // "for each"
    public void fch(Lang.C<T> f) {
        fch((el, i) -> f.accept(el));
    }
    public void fch(Lang.C2<T, String> f) {
        subject.forEach((k,v) -> f.accept(v,k));
    }
    // "pairs"
    public L<Lang.T2<String, T>> prs() {
        L<Lang.T2<String, T>> result = Lang.list();
        fch((v,k) -> result.add(Lang.T2(k,v)));
        return result;
    }
    // "values"
    public It<T> vls() {
        return prs().map(p -> p.b);
    }
    // "keys"
    public It<String> kys() {
        return prs().map(p -> p.a);
    }
    // "flat map filter optional"
    public <Tnew> Dict<Tnew> fop(Lang.F<T, Opt<Tnew>> convert) {
        return fop((el, i) -> convert.apply(el));
    }
    public <Tnew> Dict<Tnew> fop(Lang.F2<T, String, Opt<Tnew>> convert) {
        Dict<Tnew> result = new Dict<>(Lang.list());
        return prs()
            .fop(t -> convert.apply(t.b, t.a)
                .map(newVal -> Lang.T2(t.a, newVal)))
            .arr().dct(a -> a);
    }
    // "get at"
    public Opt<T> gat(String key) {
        if (subject.containsKey(key)) {
            return Lang.opt(subject.get(key));
        } else {
            return Lang.opt(null);
        }
    }

    // following methods implement Java's Map interface
    public int size() {return subject.size();}
    public boolean isEmpty() {return subject.isEmpty();}
    public boolean containsKey(Object key) {return subject.containsKey(key);}
    public boolean containsValue(Object value) {return subject.containsValue(value);}
    public T get(Object key) {return subject.get(key);}
    public void clear() {subject.clear();}
    @NotNull
    public Set<String> keySet() {return subject.keySet();}
    @NotNull public Collection<T> values() {return subject.values();}
    @NotNull public Set<Entry<String, T>> entrySet() {return subject.entrySet();}
    // add them only for compatibility with Java's Map interface, but you
    // should instantly pass all values to the constructor nevertheless
    @Deprecated public T put(String key, T value) {return subject.put(key, value);}
    @Deprecated public T remove(Object key) {return subject.remove(key);}
    @Deprecated public void putAll(@NotNull Map<? extends String, ? extends T> m) {subject.putAll(m);}
}
