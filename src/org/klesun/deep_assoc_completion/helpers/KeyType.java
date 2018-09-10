package org.klesun.deep_assoc_completion.helpers;

import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.lang.Dict;
import org.klesun.lang.L;

import static org.klesun.lang.Lang.list;
import static org.klesun.lang.Lang.opt;

/** is there actually a point in having this and MultiType as two separate classes? */
public class KeyType
{
    public enum EKeyType {STRING, INTEGER, UNKNOWN}

    final public EKeyType keyType;
    final private L<DeepType> types;

    private KeyType(EKeyType keyType, @NotNull L<DeepType> types)
    {
        this.keyType = keyType;
        this.types = types;
    }

    public static KeyType mt(MultiType mt)
    {
        return mt.getStringValues().size() > 0
            ? new KeyType(EKeyType.STRING, mt.types)
            : (mt.isInt()
                ? KeyType.integer()
                : KeyType.unknown());
    }

    public static KeyType integer()
    {
        return new KeyType(EKeyType.INTEGER, list());
    }

    public static KeyType unknown()
    {
        return new KeyType(EKeyType.UNKNOWN, list());
    }

    public L<String> getNames()
    {
        return new MultiType(types).getStringValues();
    }

    public Dict<L<DeepType>> getNameToMt()
    {
        return types.gop(t -> opt(t.stringValue));
    }
}
