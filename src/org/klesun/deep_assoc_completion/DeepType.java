package org.klesun.deep_assoc_completion;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.lang.*;

import java.util.*;

/**
 * contains info about associative
 * array key typeGetters among other things
 */
public class DeepType extends Lang
{
    public final L<Key> keys = new L<>();
    // just like array keys, but dynamic object properties
    public final Dict<Key> props = new Dict<>(L());
    // applicable to closures and function names
    // (starting with self::) and [$obj, 'functionName'] tuples
    // slowly migrating returnTypes from constant values to a function
    // list of functions that take arg list and return list of return types
    public final L<F<IExprCtx, MemIt<DeepType>>> returnTypeGetters = L();
    public final L<DeepType> pdoFetchTypes = L();
    public final LinkedHashSet<String> pdoBindVars = new LinkedHashSet<>();
    public Opt<IExprCtx> ctorArgs = opt(null);
    public Opt<PhpType> clsRefType = non();
    public final @Nullable String stringValue;
    public final PsiElement definition;
    public final PhpType briefType;
    public boolean isNumber = false;
    final public boolean isExactPsi;

    private DeepType(PsiElement definition, PhpType briefType, String stringValue, boolean isExactPsi)
    {
        this.definition = definition;
        this.briefType = briefType.filterUnknown();
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
        this(definition, Tls.getIdeaType(definition));
    }

    DeepType(StringLiteralExpressionImpl lit)
    {
        this(lit, PhpType.STRING, lit.getContents());
    }

    /** new object creation */
    public static DeepType makeNew(NewExpression newExp, IExprCtx ctorArgs, PhpType ideaType)
    {
        DeepType self = new DeepType(newExp, ideaType, null);
        self.ctorArgs = opt(ctorArgs);
        return self;
    }

    public static DeepType makeInt(PsiElement numPsi, String number)
    {
        DeepType self = new DeepType(numPsi, PhpType.INT, number);
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
            .flt(k -> k.typeGetters.has() && k.keyType.getTypes.get().any(kt -> kt.isNumber() || kt.stringValue == null))
            .fap(k -> k.typeGetters.fap(mtg -> mtg.get().types));
    }

    public Key addKey(String name, PsiElement definition)
    {
        DeepType kt = new DeepType(definition, PhpType.STRING, name);
        KeyType keyType = KeyType.mt(() -> It(som(kt)), definition);
        Key keyEntry = new Key(keyType, definition);
        keys.add(keyEntry);
        return keyEntry;
    }

    public Key addKey(KeyType keyType, PsiElement definition)
    {
        Key keyEntry = new Key(keyType, definition);
        keys.add(keyEntry);
        return keyEntry;
    }

    public Key addKey(KeyType keyType)
    {
        return addKey(keyType, keyType.definition);
    }

    public Key addProp(String name, PsiElement definition)
    {
        DeepType kt = new DeepType(definition, PhpType.STRING, name);
        KeyType keyType = KeyType.mt(() -> It(som(kt)), definition);
        Key keyEntry = new Key(keyType, definition);
        props.put(name, keyEntry);
        return keyEntry;
    }

    public static class Key
    {
        final public KeyType keyType;
        // TODO: rename to valueTypeGetters
        final private L<S<Mt>> typeGetters = L();
        // to get quick built-in type info
        final private L<PhpType> briefTypes = L();
        // where Go To Definition will lead
        final public PsiElement definition;
        public Set<String> comments = new LinkedHashSet<>();

        private Key(KeyType keyType, PsiElement definition)
        {
            this.keyType = keyType;
            this.definition = definition;
        }

        public Key addType(S<Mt> getter, PhpType briefType)
        {
            typeGetters.add(Tls.onDemand(getter));
            briefTypes.add(briefType);
            return this;
        }

        public Key addComments(Iterable<String> comments)
        {
            comments.forEach(this.comments::add);
            return this;
        }

        public void addType(S<Mt> getter)
        {
            addType(getter, PhpType.MIXED);
        }

        public It<DeepType> getTypes()
        {
            return typeGetters.fap(g -> g.get().types);
        }

        public L<PhpType> getBriefTypes()
        {
            return briefTypes;
        }

        public L<S<Mt>> getTypeGetters()
        {
            return typeGetters;
        }
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
        Set<String> mergedProps = new HashSet<>(L(types).fap(t -> t.props.kys()).arr());
        List<DeepType> indexTypes = list();
        List<String> briefTypes = list();

        types.forEach(t -> {
            t.keys.forEach((v) -> {
                v.keyType.getNames().fch(k -> {
                    if (!mergedKeys.containsKey(k)) {
                        mergedKeys.put(k, list());
                    }
                    v.getTypes().fch(el -> mergedKeys.get(k).add(el));
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
            It<String> briefs = It(new HashSet<>(briefTypes)).flt(t -> !"".equals(t));
            result = "'" + Tls.implode("|", briefs) + "'";
        }
        circularRefs.removeAll(types);
        return result;
    }

    @Override
    public String toString()
    {
        String typeInfo = "unk()";
        if (stringValue != null) {
            typeInfo = "str(" + stringValue + ")";
        } else if (keys.has()) {
            typeInfo = "arr(" + keys.map(k -> k.definition.getText()).str() + ")";
        } else if (props.size() > 0) {
            typeInfo = "obj(" + props.kys().str() + ")";
        } else if (isNumber) {
            typeInfo = "int";
        } else if (returnTypeGetters.has()) {
            typeInfo = "fun()";
        } else if (clsRefType.has()) {
            typeInfo = "cls(" + clsRefType.unw() + ")";
        } else if (ctorArgs.has()) {
            typeInfo = "new()";
        }
        return typeInfo + " " + briefType + " " + Tls.singleLine(definition.getText(), 60);
    }

    public boolean hasNumberIndexes()
    {
        // TODO: get rid of this function, we should not iterate through all types during resolution
        return It(keys).any(k -> k.keyType.getTypes.get().any(kt -> kt.isNumber()));
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
}
