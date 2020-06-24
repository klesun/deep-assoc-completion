package org.klesun.deep_assoc_completion.structures.psalm;

import java.util.List;

import static org.klesun.lang.Lang.It;

/**
 * note, it also includes scalar types like `int`, `array`,
 * `callable` and psalm magic types like `array-key`
 */
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

    @Override
    public String toString() {
        return fqn + (generics.size() > 0 ? "<" + It(generics).str(", ") + ">" : "");
    }
}
