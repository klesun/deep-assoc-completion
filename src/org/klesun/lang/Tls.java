package org.klesun.lang;

import com.intellij.psi.PsiElement;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String getStackTrace()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        new Exception().printStackTrace(pw);
        return sw.toString();
    }

    public static <T extends PsiElement> Opt<T> findParent(
        PsiElement psi,
        Class<T> cls,
        Predicate<PsiElement> continuePred
    ) {
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
    }

    public static <T extends PsiElement> L<T> findChildren(
        PsiElement parent,
        Class<T> cls,
        Predicate<PsiElement> goDeeperPred
    ) {
        if (goDeeperPred.test(parent)) {
            return L(parent.getChildren())
                .fap(c -> findChildren(c, cls, goDeeperPred))
                .cct(list(parent).fop(toCast(cls)));
        } else {
            return L();
        }
    }

    /**
     * be careful, java's regex implementation matches WHOLE string, in other
     * words, it implicitly adds "^" and "$" at beginning and end of your regex
     */
    public static Opt<L<String>> regex(String patternText, String subjectText)
    {
        List<String> result = list();
        Pattern pattern = Pattern.compile(patternText);
        Matcher matcher = pattern.matcher(subjectText);
        if (matcher.matches()) {
            for (int i = 1; i < matcher.groupCount(); ++i) {
                result.add(matcher.group(i));
            }
            return opt(L(result));
        } else {
            return opt(null);
        }
    }

    /** make object string representation _approximately_ resembling json for debug */
    public static String json(Object value)
    {
        if (value instanceof List) {
            List list = (List)value;
            String result = "[";
            for (int i = 0; i < list.size(); ++i) {
                result += json(list.get(i));
                if (i < list.size() - 1) {
                    result += ",";
                }
            }
            return result + "]";
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return value.toString();
        }
    }

    public static String implode(String delimiter, L<String> values)
    {
        String result = "";
        for (int i = 0; i < values.size(); ++i) {
            result += values.get(i);
            if (i < values.size() - 1) {
                result += delimiter;
            }
        }
        return result;
    }

    public static L<String> diff(L<String> a, L<String> b)
    {
        L<String> result = list();
        Set<String> bSet = new HashSet<>(b);
        for (String value: a) {
            if (!bSet.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public static <T> S<T> onDemand(S<T> f)
    {
        Mutable<T> value = new Mutable<>(null);
        Mutable<Boolean> demanded = new Mutable<>(false);
        return () -> {
            if (!demanded.get()) {
                value.set(f.get());
                demanded.set(true);
            }
            return value.get();
        };
    }

    public static L<Integer> range(int l, int r)
    {
        L<Integer> result = L();
        for (int i = l; i < r; ++i) {
            result.add(i);
        }
        return result;
    }
}
