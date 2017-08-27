package org.klesun.deep_keys;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.PhpExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringUtils;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.*;

/**
 * contains info about associative
 * array key typeGetters among other things
 */
public class DeepType extends Lang
{
    // keys and typeGetters of associative array
    public final LinkedHashMap<String, Key> keys = new LinkedHashMap<>();
    // possible typeGetters of list element
    public List<DeepType> indexTypes = new ArrayList<>();
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

    public DeepType(PsiElement definition, PhpType briefType)
    {
        this(definition, briefType, null);
    }

    public DeepType(PhpExpression definition)
    {
        this(definition, definition.getType());
    }

    DeepType(StringLiteralExpressionImpl lit)
    {
        this(lit, lit.getType(), lit.getContents());
    }

    public DeepType(PhpExpressionImpl numPsi, Integer number)
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
        final private L<S<MultiType>> typeGetters = L();
        // where Go To Definition will lead
        final public PsiElement definition;

        private Key(String name, PsiElement definition)
        {
            this.name = name;
            this.definition = definition;
        }

        public void addType(S<MultiType> getter)
        {
            typeGetters.add(Tls.onDemand(getter));
        }

        public L<DeepType> getTypes()
        {
            return typeGetters.fap(g -> g.get().types);
        }

        public L<S<MultiType>> getTypeGetters()
        {
            return typeGetters;
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
                mergedKeys.get(k).addAll(v.getTypes());
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

    /** not sure it works correctly */
    public DeepType deepCopy()
    {
        DeepType self = new DeepType(definition, briefType, stringValue);
        self.keys.forEach((name, srcKey) -> {
            Key newKey = self.addKey(name, srcKey.definition);
            srcKey.typeGetters.fch(newKey::addType);
        });
        self.indexTypes.addAll(L(indexTypes).map(t -> t.deepCopy()));
        self.returnTypes.addAll(L(returnTypes).map(t -> t.deepCopy()));
        return self;
    }
}
