package org.klesun.lang;

import com.intellij.psi.PsiElement;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Predicate;

/**
 * provides static functions that would
 * be useful in any kind of situation
 */
public class Tls extends Lang
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

    private static String getStackTrace()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        new Exception().printStackTrace(pw);
        return sw.toString();
    }

    public static <T extends PsiElement> Lang.F<PsiElement, Opt<T>> toFindParent(Class<T> cls, Predicate<PsiElement> continuePred)
    {
        return (psi) -> {
            PsiElement parent = psi.getParent();
            while (parent != null) {
                Opt<T> matching = Tls.cast(cls, parent);
                if (matching.has()) {
                    return matching;
                } else if (!continuePred.test(parent)) {
                    break;
                }
                parent = parent.getParent();
            }
            return opt(null);
        };
    }
}
