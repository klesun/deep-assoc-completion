package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.lang.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * this data structure represents a list of
 * DeepTypes-s that some variable mya have
 * it's more readable type annotation than L<DeepType>
 *
 * it also probably could give some handy methods
 * like getKey(), elToArr(), arToEl() - all the
 * static functions that take list of typeGetters
 */
public class Mt extends Lang
{
    static enum REASON {OK, CIRCULAR_REFERENCE, FAILED_TO_RESOLVE, DEPTH_LIMIT, INVALID_PSI}
    public static Mt CIRCULAR_REFERENCE = new Mt(L(), REASON.CIRCULAR_REFERENCE);
    public static Mt INVALID_PSI = new Mt(L(), REASON.INVALID_PSI);

    private REASON reason;
    final public MemoizingIterable<DeepType> types;

    private boolean isGettingKey = false;

    public Mt(Iterable<DeepType> types, REASON reason)
    {
        // there sometimes appear thousands of duplicates
        // I'm not sure I'm good mathematician enough to find
        // out the algorithm that would not produce them with
        // all these recursions, so I'm just removing dupes here
        this.types = new MemoizingIterable<>(types.iterator());
        this.reason = reason;
    }
    public Mt(Iterable<DeepType> types)
    {
        this(types, REASON.OK);
    }

    public static DeepType getInArraySt(It<DeepType> types, PsiElement call)
    {
        DeepType result = new DeepType(call, PhpType.ARRAY);
        result.addKey(KeyType.integer(call), call)
            .addType(Tls.onDemand(() -> new Mt(types)), PhpType.MIXED);
        return result;
    }

    /** transforms type T to T[] */
    public DeepType getInArray(PsiElement call)
    {
        return getInArraySt(It(types), call);
    }

    public static String getStringValueSt(Iterable<DeepType> types)
    {
        int i = 0;
        @Nullable String value = null;
        for (DeepType t: types) {
            if (i > 0 && !Objects.equals(t.stringValue, value)) {
                return null;
            }
            value = t.stringValue;
            if (value == null) {
                return null;
            }
            ++i;
        }
        return value;
    }

    public String getStringValue()
    {
        return getStringValueSt(types);
    }

    public It<String> getStringValues()
    {
        return types.fop(t -> opt(t.stringValue));
    }

    public static It<DeepType> getElSt(DeepType arrt)
    {
        return getKeySt(arrt, null);
    }

    public Mt getEl()
    {
        return getKey(null);
    }

    public static It<DeepType> getKeySt(DeepType type, @Nullable String keyName)
    {
        return It.cnc(
            type.keys
                .flt(k -> keyName == null || k.keyType.getTypes.get()
                    .any(kt -> keyName.equals(kt.stringValue)
                        || kt.stringValue == null
                        && (!kt.isNumber() || Tls.isNum(keyName))))
                .fap(k -> k.getTypes()),
            opt(type.briefType.elementType().filterUnknown().filterMixed())
                .flt(it -> !it.isEmpty()).itr()
                .map(it -> new DeepType(type.definition, it, false))
        );
    }

    public Mt getKey(String keyName)
    {
        if (isGettingKey) { // see issue #45
            return Mt.CIRCULAR_REFERENCE;
        }
        isGettingKey = true;

        It<DeepType> keyTsIt = types.fap(t -> getKeySt(t, keyName));

        isGettingKey = false;
        return new Mt(keyTsIt);
    }

    public static PhpType getKeyBriefTypeSt(Iterable<PhpType> ideaTypes)
    {
        PhpType ideaType = new PhpType();
        ideaTypes.forEach(ideaType::add);
        return ideaType;
    }

    public It<String> getKeyNames()
    {
        return types.fap(t -> It(t.keys))
            .fap(k -> k.keyType.getNames()).unq();
    }

    public It<DeepType.Key> getProps()
    {
        return types.fap(t -> t.props.vls());
    }

    public static PhpType getIdeaTypeSt(It<DeepType> types)
    {
        PhpType ideaType = new PhpType();
        types.map(t -> t.briefType).fch(ideaType::add);
        return ideaType;
    }

    public PhpType getIdeaType()
    {
        return getIdeaTypeSt(It(types));
    }

    public String getBriefValueText(int maxLen, Set<DeepType> circularRefs)
    {
        L<DeepType> types = L(this.types);
        if (types.any(circularRefs::contains)) {
            return "*circ*";
        }
        circularRefs.addAll(types);

        L<String> briefValues = list();
        L<String> keyNames = getKeyNames().arr();

        if (keyNames.size() > 0) {
            if (keyNames.all((k,i) -> (k + "").equals(i + ""))) {
                briefValues.add("(" + Tls.implode(", ", keyNames.map(i -> getKey(i + "")
                    .getBriefValueText(15, circularRefs))) + ")");
            } else {
                briefValues.add("{" + Tls.implode(", ", keyNames.map(k -> k + ":")) + "}");
            }
        }
        It<String> strvals = types.fop(t -> opt(t.stringValue));
        if (strvals.has()) {
            briefValues.add(Tls.implode("|", strvals
                .arr().grp(a -> a).kys()
                .map(s -> (types.all((t,i) -> t.definition.getText().equals(s)))
                    ? s : "'" + s + "'")));
        }
        if (types.any(t -> t.getListElemTypes().has())) {
            briefValues.add("[" + getEl().getBriefValueText(maxLen, circularRefs) + "]");
        }
        if (briefValues.isEmpty() && types.size() > 0) {
            It<String> psiParts = types.flt(t -> t.isExactPsi).map(t -> Tls.singleLine(t.definition.getText(), 40));
            briefValues.add(Tls.implode("|", psiParts.arr().grp(a -> a).kys()));
        }
        String fullStr = Tls.implode("|", briefValues);

        circularRefs.removeAll(types);
        String truncated = Tls.substr(fullStr, 0, maxLen);
        return truncated.length() == fullStr.length()
            ? truncated : truncated + "...";
    }

    public String getBriefValueText(int maxLen)
    {
        Set<DeepType> circularRefs = new HashSet<>();
        return getBriefValueText(maxLen, circularRefs);
    }

    public String varExport()
    {
        return DeepType.varExport(L(types));
    }

    public boolean hasNumberIndexes()
    {
        return types.any(t -> t.hasNumberIndexes());
    }

    public boolean isInt()
    {
        return types.any(t -> t.isNumber());
    }
}
