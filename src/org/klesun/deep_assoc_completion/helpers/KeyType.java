package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.lang.*;

import static org.klesun.lang.Lang.*;

import static org.klesun.lang.Lang.list;
import static org.klesun.lang.Lang.opt;

/** is there actually a point in having this and Mt as two separate classes? */
public class KeyType
{
    final public S<MemoizingIterable<DeepType>> getTypes;
    final public PsiElement definition;

    private KeyType(S<MemoizingIterable<DeepType>> getTypes, PsiElement definition)
    {
        this.definition = definition;
        this.getTypes = getTypes;
    }

    public static KeyType mt(S<It<DeepType>> mtg, PsiElement definition)
    {
        Iterable<DeepType> anyt = som(new DeepType(definition, PhpType.MIXED));
        return new KeyType(Tls.onDemand(() -> new MemoizingIterable<>(mtg.get().def(anyt).iterator())), definition);
    }

    public static KeyType integer(PsiElement psi)
    {
        return new KeyType(() -> new MemoizingIterable<>(som(new DeepType(psi, PhpType.INT)).iterator()), psi);
    }

    public static KeyType unknown(PsiElement psi)
    {
        return new KeyType(() -> new MemoizingIterable<>(som(new DeepType(psi, PhpType.MIXED)).iterator()), psi);
    }

    public It<String> getNames()
    {
        return getTypes.get().fap(t -> opt(t.stringValue));
    }

    public It<T2<String, DeepType>> getNameToMt()
    {
        return getTypes.get().fap(t -> opt(t.stringValue).map(str -> T2(str, t)));
    }
}
