package org.klesun.deep_assoc_completion.helpers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.lang.Lang;

public class KeyType
{
    public enum EKeyType {STRING, INTEGER, UNKNOWN}

    final public EKeyType keyType;
    final public @Nullable
    Lang.L<String> names;

    private KeyType(EKeyType keyType, @Nullable Lang.L<String> name)
    {
        this.keyType = keyType;
        this.names = name;
    }

    public static KeyType string(@NotNull  Lang.L<String> name)
    {
        return new KeyType(EKeyType.STRING, name);
    }

    public static KeyType integer()
    {
        return new KeyType(EKeyType.INTEGER, null);
    }

    public static KeyType unknown()
    {
        return new KeyType(EKeyType.UNKNOWN, null);
    }
}
