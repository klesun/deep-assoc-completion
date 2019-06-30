package org.klesun.deep_assoc_completion.structures.psalm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.klesun.lang.Lang.It;

public class TAssoc implements IType
{
    final public LinkedHashMap<String, IType> keys;
    final public Map<String, List<String>> keyToComments;
    final public String unparsed;

    public TAssoc(LinkedHashMap<String, IType> keys, Map<String, List<String>> keyToComments, String unparsed)
    {
        this.keys = keys;
        this.keyToComments = keyToComments;
        this.unparsed = unparsed;
    }

    @Override
    public String toString() {
        return "array{" + It(keys.entrySet())
            .map(k -> k.getKey() + ": " + k.getValue())
            .str(", ") + "}";
    }
}
