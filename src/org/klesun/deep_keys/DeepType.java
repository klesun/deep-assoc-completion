package org.klesun.deep_keys;

import com.google.gson.annotations.Expose;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.PhpExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.klesun.lang.Lang;

import java.util.*;

import static org.klesun.lang.Lang.list;
import static org.klesun.lang.Lang.opt;

/**
 * contains info about associative
 * array key types among other things
 */
public class DeepType
{
    // keys and types of associative array
    public final LinkedHashMap<String, Key> keys = new LinkedHashMap<>();
    // possible types of list element
    public final List<DeepType> indexTypes = new ArrayList<>();
    // applicable to closures and function names
    // (starting with self::) and [$obj, 'functionName'] tuples
    public final List<DeepType> returnTypes = new ArrayList<>();
    public final String stringValue;
    public final PsiElement definition;
    public final PhpType briefType;
    DeepType self = this;

    DeepType(PsiElement definition, PhpType briefType, String stringValue)
    {
        this.definition = definition;
        this.briefType = briefType;
        this.stringValue = stringValue;
    }

    DeepType(PsiElement definition, PhpType briefType)
    {
        this(definition, briefType, null);
    }

    DeepType(PhpExpression definition)
    {
        this(definition, definition.getType());
    }

    DeepType(StringLiteralExpressionImpl lit)
    {
        this(lit, lit.getType(), lit.getContents());
    }

    DeepType(PhpExpressionImpl numPsi, Integer number)
    {
        this(numPsi, numPsi.getType(), "" + number);
    }

    public Key addKey(String name, PsiElement definition)
    {
        Key keyEntry = new Key(name, definition);
        keys.put(keyEntry.name, keyEntry);
        return keyEntry;
    }

    public static class Key
    {
        final public String name;
        final public List<DeepType> types = new ArrayList<>();
        // where Go To Definition will lead
        final public PsiElement definition;

        private Key(String name, PsiElement definition)
        {
            this.name = name;
            this.definition = definition;
        }
    }

    private static String indent(int level)
    {
        return new String(new char[level]).replace("\0", "  ");
    }

    public static String toJson(List<DeepType> types, int level)
    {
        LinkedHashMap<String, List<DeepType>> mergedKeys = new LinkedHashMap<>();
        List<DeepType> indexTypes = list();
        List<String> briefTypes = list();

        types.forEach(t -> {
            t.keys.forEach((k,v) -> {
                if (!mergedKeys.containsKey(k)) {
                    mergedKeys.put(k, list());
                }
                mergedKeys.get(k).addAll(v.types);
            });
            t.indexTypes.forEach(indexTypes::add);
            briefTypes.add(opt(t.stringValue).map(s -> "'" + s + "'").def(t.briefType.toString()));
        });

        if (mergedKeys.size() > 0) {
            String result = "{\n";
            ++level;
            for (Map.Entry<String, List<DeepType>> e: mergedKeys.entrySet()) {
                result += indent(level) + "\"" + e.getKey() + "\"" + ": " + toJson(e.getValue(), level) + ",\n";
            }
            --level;
            result += indent(level) + "}";
            return result;
        } else if (indexTypes.size() > 0) {
            return "[" + toJson(indexTypes, level) + "]";
        } else if (briefTypes.size() > 0) {
            List<String> bytes = new ArrayList(new HashSet(briefTypes));
            return "\"" + StringUtils.join(bytes, "|") + "\"";
        } else {
            return "\"unknown\"";
        }
    }

    @Override
    public String toString()
    {
        return toJson(list(this), 0);
    }
}
