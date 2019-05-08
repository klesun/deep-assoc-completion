package org.klesun.deep_assoc_completion.structures.psalm;

import java.util.LinkedHashMap;

public class TAssoc implements IType
{
    final public LinkedHashMap<String, IType> keys;

    public TAssoc(LinkedHashMap<String, IType> keys)
    {
        this.keys = keys;
    }
}
