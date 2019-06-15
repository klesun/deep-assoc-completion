package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.lang.It;
import org.klesun.lang.MemIt;

import static org.klesun.lang.Lang.*;

/** is there actually a point in having this and Mt as two separate classes? */
public class KeyType
{
    final public MemIt<DeepType> types;
    final public PsiElement definition;

    private KeyType(MemIt<DeepType> getTypes, PsiElement definition)
    {
        this.definition = definition;
        this.types = getTypes;
    }

    public static KeyType mt(Iterable<DeepType> mtg, PsiElement definition)
    {
        Iterable<DeepType> anyt = som(new DeepType(definition, PhpType.MIXED));
        return new KeyType(It(mtg).def(anyt).mem(), definition);
    }

    public static KeyType integer(PsiElement psi)
    {
        return new KeyType(new MemIt<>(som(new DeepType(psi, PhpType.INT))), psi);
    }

    public static KeyType unknown(PsiElement psi)
    {
        return new KeyType(new MemIt<>(som(new DeepType(psi, PhpType.MIXED))), psi);
    }

    public MemIt<DeepType> getTypes()
    {
        return types;
    }

    public It<String> getNames()
    {
        return getTypes().fap(t -> opt(t.stringValue));
    }

    public It<T2<String, DeepType>> getNameToMt()
    {
        return getTypes().fap(t -> opt(t.stringValue).map(str -> T2(str, t)));
    }
}
