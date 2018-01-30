package org.klesun.deep_assoc_completion.helpers;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class KeyType
{
    public enum EKeyType {STRING, INTEGER, UNKNOWN}

    final public EKeyType keyType;
    final public @Nullable String name;

    private KeyType(EKeyType keyType, @Nullable String name)
    {
        this.keyType = keyType;
        this.name = name;
    }

    public static KeyType string(@NotNull  String name)
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
