package org.klesun.deep_assoc_completion.structures.psalm;

import java.util.List;

import static org.klesun.lang.Lang.It;

public class TMulti implements IType
{
    final public List<IType> types;

    public TMulti(List<IType> types)
    {
        this.types = types;
    }

    @Override
    public String toString() {
        return It(types).str("|");
    }
}
