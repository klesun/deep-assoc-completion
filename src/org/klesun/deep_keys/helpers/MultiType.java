package org.klesun.deep_keys.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_keys.DeepType;
import org.klesun.lang.Lang;

import javax.annotation.Nullable;
import java.util.HashSet;

/**
 * this data structure represents a list of
 * DeepTypes-s that some variable mya have
 * it's more readable type annotation than L<DeepType>
 *
 * it also probably could give some handy methods
 * like getKey(), elToArr(), arToEl() - all the
 * static functions that take list of typeGetters
 */
public class MultiType extends Lang
{
    static enum REASON {OK, CIRCULAR_REFERENCE, FAILED_TO_RESOLVE, DEPTH_LIMIT, INVALID_PSI}
    public static MultiType CIRCULAR_REFERENCE = new MultiType(L(), REASON.CIRCULAR_REFERENCE);
    public static MultiType INVALID_PSI = new MultiType(L(), REASON.INVALID_PSI);

    private REASON reason;
    final public L<DeepType> types;

    public MultiType(L<DeepType> types, REASON reason)
    {
        this.types = types;
        this.reason = reason;
    }
    public MultiType(L<DeepType> types)
    {
        this(types, REASON.OK);
    }

    public MultiType getEl()
    {
        return new MultiType(types.fap(arrt -> {
            L<DeepType> mixed = L();
            mixed.addAll(arrt.indexTypes);
            arrt.keys.forEach((k,v) -> mixed.addAll(v.getTypes()));
            return mixed;
        }));
    }

    public DeepType getInArray(PsiElement call)
    {
        DeepType result = new DeepType(call, PhpType.ARRAY);
        result.indexTypes.addAll(types);
        return result;
    }

    @Nullable
    public String getStringValue()
    {
        if (types.size() == 1) {
            return types.get(0).stringValue;
        } else {
            return null;
        }
    }

    public MultiType getKey(@Nullable String keyName)
    {
        return new MultiType(list(
            // if keyName is a constant string - return type of this key only
            types.flt(t -> keyName != null)
                .fop(type -> Lang.getKey(type.keys, keyName))
                .fap(v -> v.getTypes()),
            // if keyName is a var - return types of all keys
            types.flt(t -> keyName == null)
                .fap(t -> L(t.keys.values()))
                .fap(v -> v.getTypes()),
            types.fap(t -> t.indexTypes)
        ).fap(a -> a));
    }

    public L<String> getKeyNames()
    {
        L<String> names = L();
        HashSet<String> repeations = new HashSet<>();
        types.fap(t -> L(t.keys.keySet())).fch(name -> {
            if (!repeations.contains(name)) {
                repeations.add(name);
                names.add(name);
            }
        });
        return names;
    }

    public String toJson()
    {
        return DeepType.toJson(types, 0);
    }

    public MultiType deepCopy()
    {
        return new MultiType(types.map(t -> t.deepCopy()));
    }
}
