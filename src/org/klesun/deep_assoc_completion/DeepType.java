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
    public L<DeepType> anyKeyElTypes = new L<>();
    public L<DeepType> listElTypes = new L<>();
    // applicable to closures and function names
    // (starting with self::) and [$obj, 'functionName'] tuples
    // slowly migrating returnTypes from constant values to a function
    // list of functions that take arg list and return list of return types
    public final L<F<FuncCtx, L<DeepType>>> returnTypeGetters = L();
    public final L<DeepType> pdoTypes = L();
    public final @Nullable String stringValue;
    public final PsiElement definition;
    public final PhpType briefType;
    public boolean isNumber = false;
    final public boolean isExactPsi;
    public boolean hasIntKeys = false;

    public DeepType(PsiElement definition, PhpType briefType, String stringValue, boolean isExactPsi)
    {
        this.definition = definition;
        this.briefType = briefType;
        this.stringValue = stringValue;
        this.isExactPsi = isExactPsi;
    }

    public DeepType(PsiElement definition, PhpType briefType, String stringValue)
    {
        this(definition, briefType, stringValue, true);
    }

    public DeepType(PsiElement definition, PhpType briefType, boolean isExactPsi)
    {
        this(definition, briefType, null, isExactPsi);
        this.isNumber = briefType.getTypes().contains("\\int");
    }

    public DeepType(PsiElement definition, PhpType briefType)
    {
        this(definition, briefType, true);
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
        this.isNumber = true;
    }

    public L<DeepType> getReturnTypes(FuncCtx ctx)
    {
        L<DeepType> result = returnTypeGetters.fap(g -> g.apply(ctx));
        return result;
    }

    public L<DeepType> getElemTypes()
    {
        return listElTypes.cct(anyKeyElTypes);
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
        Set<DeepType> circularRefs = new HashSet<>();
        return toJson(types, level, circularRefs);
    }

    public static String toJson(List<DeepType> types, int level, Set<DeepType> circularRefs)
    {
        if (L(types).any(circularRefs::contains)) {
            return "\"*circ*\"";
        }
        circularRefs.addAll(types);

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
            t.getElemTypes().forEach(indexTypes::add);
            briefTypes.add(opt(t.stringValue).map(s -> "'" + s + "'").def(t.briefType.filterUnknown().toString()));
        });

        String result = "unknown";
        if (mergedKeys.size() > 0) {
            result = "{\n";
            ++level;
            for (Map.Entry<String, List<DeepType>> e: mergedKeys.entrySet()) {
                result += indent(level) + "\"" + e.getKey() + "\"" + ": " + toJson(e.getValue(), level, circularRefs) + ",\n";
            }
            --level;
            result += indent(level) + "}";
        } else if (indexTypes.size() > 0) {
            result = "[" + toJson(indexTypes, level, circularRefs) + "]";
        } else if (briefTypes.size() > 0) {
            List<String> bytes = new ArrayList(new HashSet(briefTypes));
            result = "\"" + StringUtils.join(bytes, "|") + "\"";
        }
        circularRefs.removeAll(types);
        return result;
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

    public boolean isNumber()
    {
        if (stringValue != null &&
            Tls.regex("^\\d+$", stringValue).has()
        ) {
            return true;
        } else {
            return isNumber;
        }
    }
}
