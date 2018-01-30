package org.klesun.deep_assoc_completion;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.PhpExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
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
    // slowly migrating returnTypes from constant values to a function
    // list of functions that take arg list and return list of return types
    public final L<F<FuncCtx, L<DeepType>>> returnTypeGetters = L();
    public final L<DeepType> pdoTypes = L();
    public final @Nullable String stringValue;
    public final PsiElement definition;
    public final PhpType briefType;
    public boolean isInt = false;
    public boolean hasIntKeys = false;

    public DeepType(PsiElement definition, PhpType briefType, String stringValue)
    {
        this.definition = definition;
        this.briefType = briefType;
        this.stringValue = stringValue;
    }

    public DeepType(PsiElement definition, PhpType briefType)
    {
        this(definition, briefType, null);
        this.isInt = briefType.getTypes().contains("\\int");
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
        this.isInt = true;
    }

    public L<DeepType> getReturnTypes(FuncCtx ctx)
    {
        L<DeepType> result = returnTypeGetters.fap(g -> g.apply(ctx));
        return result;
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
        // to get quick built-in type info
        final private L<PhpType> briefTypes = L();
        // where Go To Definition will lead
        final public PsiElement definition;

        private Key(String name, PsiElement definition)
        {
            this.name = name;
            this.definition = definition;
        }

        public void addType(S<MultiType> getter, PhpType briefType)
        {
            typeGetters.add(Tls.onDemand(getter));
            briefTypes.add(briefType);
        }

        public L<DeepType> getTypes()
        {
            return typeGetters.fap(g -> g.get().types);
        }

        public L<PhpType> getBriefTypes()
        {
            return briefTypes;
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

    public boolean hasNumberIndexes()
    {
        return hasIntKeys || L(keys.values()).any(k -> Tls.regex("^\\d+$", k.name).has());
    }

    public boolean isInt()
    {
        if (stringValue != null &&
            Tls.regex("^\\d+$", stringValue).has()
        ) {
            return true;
        } else {
            return isInt;
        }
    }
}
