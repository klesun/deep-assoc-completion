package org.klesun.deep_assoc_completion.structures.psalm;

import java.util.List;

public class TClass implements IType
{
    /** usually class name, but could also be "array", "string", "T", etc... */
    final public String fqn;
    final public List<IType> generics;

    public TClass(String fqn, List<IType> generics)
    {
        this.fqn = fqn;
        this.generics = generics;
    }
}
