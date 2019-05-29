package org.klesun.deep_assoc_completion.structures.psalm;

import com.jetbrains.php.lang.psi.resolve.types.PhpType;

public class TPrimitive implements IType
{
    final public PhpType kind;
    final public String stringValue;

    public TPrimitive(PhpType kind, String stringValue)
    {
        this.kind = kind;
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        // does not handle quote escaping and does not
        // distinct between 5.18 and "5.18", but nah
        return "'" + stringValue + "'";
    }
}
