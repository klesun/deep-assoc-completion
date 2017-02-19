package org.klesun.lang;

/**
 * provides static functions that would
 * be useful in any kind of situation
 */
public class Tls
{
    /**
     * unlike built-in SomeClass.class.cast(),
     * this returns empty optional instead of
     * throwing exception if cast fails
     */
    public static <T> Opt<T> cast(Class<T> cls, Object value)
    {
        if (cls.isInstance(value)) {
            return new Opt(cls.cast(value));
        } else {
            return new Opt(null);
        }
    }
}
