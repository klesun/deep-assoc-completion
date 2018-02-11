package org.klesun.deep_assoc_completion.helpers;

/**
 * represents order of a function argument and
 * whether it is variadic (function(...$arg) {})
 */
public class ArgOrder
{
    final public int order;
    final public boolean isVariadic;

    public ArgOrder(int order, boolean isVariadic)
    {
        this.order = order;
        this.isVariadic = isVariadic;
    }
}
