package org.klesun.deep_assoc_completion.structures.psalm;

import java.util.LinkedHashMap;

import static org.klesun.lang.Lang.It;

public class TAssoc implements IType
{
    final public LinkedHashMap<String, IType> keys;

    public TAssoc(LinkedHashMap<String, IType> keys)
    {
        this.keys = keys;
    }

    @Override
    public String toString() {
        return "array{" + It(keys.entrySet())
            .map(k -> k.getKey() + ": " + k.getValue())
            .str(", ") + "}";
    }
}
