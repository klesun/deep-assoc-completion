package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.*;
import org.klesun.lang.Tls.IOnDemand;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.klesun.lang.Lang.*;
import static org.klesun.lang.Lang.non;

/**
 * represents a pair of key type and value type
 * in an associative array or sequential array
 */
public class KeyEntry
{
    final public KeyType keyType;
    // TODO: rename to valueTypeGetters
    final public L<Tls.IOnDemand<Mt>> typeGetters = Lang.L();
    // to get quick built-in type info
    final private L<PhpType> briefTypes = Lang.L();
    // where Go To Definition will lead
    final public PsiElement definition;
    public Set<String> comments = new LinkedHashSet<>();

    public KeyEntry(String name, @NotNull PsiElement definition)
    {
        DeepType kt = new DeepType(definition, PhpType.STRING, name);
        KeyType keyType = KeyType.mt(Lang.som(kt), definition);
        this.keyType = keyType;
        this.definition = definition;
    }

    public KeyEntry(KeyType keyType, @NotNull PsiElement definition)
    {
        this.keyType = keyType;
        this.definition = definition;
    }

    public KeyEntry(KeyType keyType)
    {
        this(keyType, keyType.definition);
    }

    public KeyEntry addType(Lang.S<Mt> getter, PhpType briefType)
    {
        if (getter instanceof Granted) {
            typeGetters.add(Granted(getter.get()));
        } else {
            typeGetters.add(Tls.onDemand(getter));
        }
        briefTypes.add(briefType);
        return this;
    }

    public KeyEntry addComments(Iterable<String> comments)
    {
        comments.forEach(this.comments::add);
        return this;
    }

    public KeyEntry addType(Lang.S<Mt> getter)
    {
        addType(getter, PhpType.MIXED);
        return this;
    }

    public It<DeepType> getValueTypes()
    {
        return typeGetters.fap(g -> g.get().types);
    }

    public L<PhpType> getBriefTypes()
    {
        return briefTypes;
    }

    public Opt<String> getBriefKey(boolean resolveIter)
    {
        L<DeepType> usedTypes = resolveIter || keyType.types instanceof IResolvedIt ? keyType.types.arr() : L.non();
        return usedTypes.fst()
            .fop(t -> Lang.opt(t.stringValue))
            .map(n -> n + ":");
    }

    public Opt<String> getBriefVal()
    {
        return typeGetters
            .fap(IOnDemand::ifHas)
            .fap(mt -> mt.types)
            .fap(t -> t.getBriefVal()).unq()
            .wap(IIt::arr)
            .wap(fqns -> fqns.has() ? Lang.som(fqns.itr().str("|")) : Lang.non());
    }

    /** values that are cheap to obtain, like phpdoc or direct array creation */
    public L<DeepType> getGrantedValues()
    {
        return typeGetters
            .fap(g -> g instanceof Granted ? som(g.get()) : non())
            .fap(mt -> mt.types instanceof IResolvedIt ? mt.types.arr() : non())
            .arr();
    }

    @Override
    public String toString()
    {
        return "Key<" + getBriefKey(true) + ", " + getBriefVal() + ">";
    }
}
