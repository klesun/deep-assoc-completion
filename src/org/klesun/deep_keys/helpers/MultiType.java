package org.klesun.deep_keys.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_keys.DeepType;
import org.klesun.lang.Lang;

/**
 * this data structure represents a list of
 * DeepTypes-s that some variable mya have
 * it's more readable type annotation than L<DeepType>
 *
 * it also probably could give some handy methods
 * like getKey(), elToArr(), arToEl() - all the
 * static functions that take list of types
 */
public class MultiType extends Lang
{
    public L<DeepType> types;

    public MultiType(L<DeepType> types)
    {
        this.types = types;
    }

    public MultiType getEl()
    {
        return new MultiType(types.fap(arrt -> {
            L<DeepType> mixed = L();
            mixed.addAll(arrt.indexTypes);
            arrt.keys.forEach((k,v) -> mixed.addAll(v.types));
            return mixed;
        }));
    }

    public DeepType getInArray(PsiElement call)
    {
        DeepType result = new DeepType(call, PhpType.ARRAY);
        result.indexTypes.addAll(types);
        return result;
    }

    public String toJson()
    {
        return DeepType.toJson(types, 0);
    }
}
