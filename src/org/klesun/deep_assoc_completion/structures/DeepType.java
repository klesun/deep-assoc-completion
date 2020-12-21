package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.*;

import java.util.*;

/**
 * contains info about associative
 * array key typeGetters among other things
 */
public class DeepType extends Lang
{
    // please, do not change this fields directly - ise Build.java
    // probably should make them all protected and add getters...

    public IReusableIt<Key> keys = L.non();
    // just like array keys, but dynamic object properties
    public IReusableIt<Key> props = L.non();
    // applicable to closures and function names
    // (starting with self::) and [$obj, 'functionName'] tuples
    // slowly migrating returnTypes from constant values to a function
    // list of functions that take arg list and return list of return types
    //
    // probably would make sense to make it MemIt as well...
    public L<F<IExprCtx, MemIt<DeepType>>> returnTypeGetters = L();
    public final L<DeepType> pdoFetchTypes = L();
    public final LinkedHashSet<String> pdoBindVars = new LinkedHashSet<>();
    public Opt<IExprCtx> ctorArgs = opt(null);
    // from PSALM @template phpdoc
    public L<Mt> generics = L();
    public Opt<PhpType> clsRefType = non();
    // constant name
    public Opt<String> cstName = non();
    public @Nullable String stringValue = null;
    public Opt<Boolean> booleanValue = non();
    public final PsiElement definition;
    public final PhpType briefType;
    public boolean isNumber = false;
    public boolean isExactPsi = true;
    public boolean isNull = false;

    public DeepType(@NotNull PsiElement definition, PhpType briefType, String stringValue, boolean isExactPsi)
    {
        this.definition = definition;
        this.briefType = briefType.filterUnknown().filterMixed();
        this.stringValue = stringValue;
        this.isExactPsi = isExactPsi;
    }

    public DeepType(PsiElement definition, PhpType briefType, String stringValue)
    {
        this(definition, briefType, stringValue, true);
        if (briefType.equals(PhpType.NULL)) {
            this.isNull = true;
        }
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
        this(definition, Tls.getIdeaType(definition));
        String exprStr = opt(definition.getText())
            .map(txt -> txt.toLowerCase())
            .def("");
        if ("true".equals(exprStr)) {
            booleanValue = som(true);
        } else if ("false".equals(exprStr)) {
            booleanValue = som(false);
        } else if ("null".equals(exprStr)) {
            isNull = true;
        }
    }

    public DeepType(StringLiteralExpressionImpl lit)
    {
        this(lit, PhpType.STRING, lit.getContents());
    }

    /** new object creation */
    public static DeepType makeNew(NewExpression newExp, IExprCtx ctorArgs, L<Mt> generics, PhpType ideaType)
    {
        DeepType self = new DeepType(newExp, ideaType, null);
        self.ctorArgs = opt(ctorArgs);
        self.generics = generics;
        return self;
    }

    public static DeepType makeInt(PsiElement numPsi, String number)
    {
        DeepType self = new DeepType(numPsi, PhpType.INT, number);
        self.isNumber = true;
        return self;
    }

    public static DeepType makeFloat(PsiElement numPsi, String number)
    {
        DeepType self = new DeepType(numPsi, PhpType.FLOAT, number);
        self.isNumber = true;
        return self;
    }

    public static DeepType makeClsRef(PhpExpression definition, PhpType clsType)
    {
        It<String> fqns = It(clsType.filterUnknown().filterMixed().filterNull().filterPlurals().filterPrimitives().getTypes());
        DeepType self = new DeepType(definition, PhpType.STRING, fqns.fst().def(null));
        self.clsRefType = som(clsType);
        return self;
    }

    public It<DeepType> getReturnTypes(IExprCtx ctx)
    {
        return returnTypeGetters.fap(g -> g.apply(ctx));
    }

    public It<DeepType> getListElemTypes()
    {
        // TODO: get rid of this function, we should not iterate through all types during resolution
        return keys
            .flt(k -> k.valueTypeGetters.has() && k.keyType.getTypes().any(kt -> kt.isNumber() || kt.stringValue == null))
            .fap(k -> k.valueTypeGetters.fap(mtg -> mtg.get().types));
    }

    /** @deprecated - kept for compatibility with deep-js, shall be remove eventually - use Build.java instead */
    public Key addKey(KeyType keyType)
    {
        Key keyEntry = new Key(keyType, definition);
        keys = It.cnc(keys, som(keyEntry)).mem();
        return keyEntry;
    }

    private static String indent(int level)
    {
        return new String(new char[level]).replace("\0", "  ");
    }

    public static String varExport(List<DeepType> types)
    {
        Set<DeepType> circularRefs = new HashSet<>();
        return varExport(types, 0, circularRefs);
    }

    public static String varExport(List<DeepType> types, int level, Set<DeepType> circularRefs)
    {
        if (L(types).any(circularRefs::contains)) {
            return "'*circ*'";
        }
        circularRefs.addAll(types);

        LinkedHashMap<String, List<DeepType>> mergedKeys = new LinkedHashMap<>();
        Set<String> mergedProps = new HashSet<>(L(types)
            .fap(t -> t.props.fap(ke -> ke.keyType.types))
            .fap(t -> opt(t.stringValue))
            .arr());
        List<DeepType> indexTypes = list();
        L<String> briefTypes = list();

        types.forEach(t -> {
            t.keys.forEach((v) -> {
                v.keyType.getNames().fch(k -> {
                    if (!mergedKeys.containsKey(k)) {
                        mergedKeys.put(k, list());
                    }
                    v.getValueTypes().fch(el -> mergedKeys.get(k).add(el));
                });
            });
            t.getListElemTypes().forEach(indexTypes::add);
            briefTypes.add(opt(t.stringValue).def(t.briefType.filterUnknown().toString()));
        });

        String result = "'unknown'";
        if (mergedKeys.size() > 0) {
            result = "[\n";
            ++level;
            for (Map.Entry<String, List<DeepType>> e : mergedKeys.entrySet()) {
                result += indent(level) + "'" + e.getKey() + "'" + " => " + varExport(e.getValue(), level, circularRefs) + ",\n";
            }
            --level;
            result += indent(level) + "]";
        } else if (mergedProps.size() > 0) {
            result = "{" + Tls.implode(", ", It(mergedProps).map(p -> "$" + p)) + "}";
        } else if (indexTypes.size() > 0) {
            result = "[" + varExport(indexTypes, level, circularRefs) + "]";
        } else if (briefTypes.size() > 0) {
            IIt<String> briefs = briefTypes.unq()
                .flt(t -> !"".equals(t));
            result = "'" + Tls.implode("|", briefs) + "'";
        }
        circularRefs.removeAll(types);
        return result;
    }

    public Opt<String> getBriefVal(boolean resolveIter)
    {
        String typeInfo = null;
        Opt<PhpType> asObjPst = ArrCtorRes.filterObjPst(briefType);
        if (clsRefType.has()) {
            typeInfo = clsRefType.unw() + "::class";
        } else if (isNull) {
            typeInfo = "null";
        } else if (stringValue != null) {
            if (isNumber) {
                typeInfo = stringValue;
            } else {
                typeInfo = "'" + stringValue + "'";
            }
        } else if (keys.has()) {
            L<Key> usedKeys = resolveIter || keys instanceof IResolvedIt ? keys.arr() : L.non();
            typeInfo = "[" + usedKeys.fap(k -> k.getBriefKey(resolveIter)).unq().str() + "]";
        } else if (returnTypeGetters.has()) {
            typeInfo = "(...) ==> {...}";
        } else if (props.has()) {
            typeInfo = "obj(" + props
                .fap(ke -> ke.keyType.types)
                .fap(t -> opt(t.stringValue))
                .str() + ")";
        } else if (booleanValue.has()) {
            typeInfo = booleanValue.unw() + "";
        } else if (asObjPst.has()) {
            typeInfo = "new " + asObjPst.unw();
        }
        return opt(typeInfo);
    }

    public Opt<String> getBriefVal()
    {
        return getBriefVal(false);
    }

    @Override
    public String toString()
    {
        Opt<String> typeInfoOpt = getBriefVal(true);
        String typeInfo = typeInfoOpt.def("unk()");
        if (!typeInfoOpt.has()) {
            if (isNumber) {
                typeInfo = "int";
            } else if (ctorArgs.has()) {
                typeInfo = "new()";
            } else if (generics.size() > 0) {
                typeInfo = "new<" + generics.size() + ">";
            }
        }
        return "deep(" + typeInfo + ")" + " " + briefType + " " + Tls.singleLine(definition.getText(), 30);
    }

    public boolean hasNumberIndexes()
    {
        // TODO: get rid of this function, we should not iterate through all types during resolution
        return It(keys).any(k -> k.keyType.getTypes().any(kt -> kt.isNumber()));
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

    public Mt mt()
    {
        return new Mt(list(this));
    }

    /** a handy method to do stuff in same expression */
    public DeepType btw(C<DeepType> addData)
    {
        addData.accept(this);
        return this;
    }
}
