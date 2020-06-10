package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.structures.Build;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyEntry;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.klesun.lang.Lang.*;

/**
 * this data structure represents a list of
 * DeepTypes-s that some variable mya have
 * it's more readable type annotation than L<DeepType>
 *
 * it also probably could give some handy methods
 * like getKey(), elToArr(), arToEl() - all the
 * static functions that take list of typeGetters
 */
public class Mt
{
    static enum REASON {OK, CIRCULAR_REFERENCE, FAILED_TO_RESOLVE, DEPTH_LIMIT, INVALID_PSI}
    public static Mt CIRCULAR_REFERENCE = new Mt(L(), REASON.CIRCULAR_REFERENCE);
    public static Mt INVALID_PSI = new Mt(L(), REASON.INVALID_PSI);

    private REASON reason;
    final public IReusableIt<DeepType> types;

    private boolean isGettingKey = false;

    public Mt(IReusableIt<DeepType> types, REASON reason)
    {
        this.types = types;
        this.reason = reason;
    }

    /** @deprecated - keeping for deep-js-completion */
    public Mt(Iterable<DeepType> types)
    {
        this(new MemIt<>(types), REASON.OK);
    }

    public Mt(IReusableIt<DeepType> types)
    {
        this(types, REASON.OK);
    }

    public static Mt mem(IIt<DeepType> iter)
    {
        return new Mt(iter.mem());
    }

    public static DeepType getInArraySt(IIt<DeepType> types, PsiElement call)
    {
        KeyEntry keyEntry = new KeyEntry(KeyType.integer(call), call)
            .addType(Tls.onDemand(() -> new Mt(types.mem())), PhpType.MIXED);
        return new Build(call, PhpType.ARRAY)
            .keys(som(keyEntry)).get();
    }

    /** transforms type T to T[] */
    public DeepType getInArray(PsiElement call)
    {
        return getInArraySt(It(types), call);
    }

    public static String getStringValueSt(Iterable<DeepType> types)
    {
        int i = 0;
        String value = null;
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

    public IIt<String> getStringValues()
    {
        return types.fop(t -> opt(t.stringValue));
    }

    public static It<DeepType> getElSt(DeepType arrt)
    {
        return getKeySt(arrt, null);
    }

    public Mt getEl()
    {
        String nullKey = null;
        return getKey(nullKey);
    }

    public static It<DeepType> getPropOfName(IIt<KeyEntry> allProps, String keyName)
    {
        return allProps
            .flt(k -> keyName == null || k.keyType.getTypes()
                .any(kt -> keyName.equals(kt.stringValue)
                    || kt.stringValue == null))
            .fap(k -> k.getValueTypes());
    }

    public static It<DeepType> getDynaPropSt(DeepType type, String keyName)
    {
        return getPropOfName(type.props.vls(), keyName);
    }

    public static It<DeepType> getKeySt(DeepType type, String keyName)
    {
        return It.cnc(
            type.keys
                .flt(k -> keyName == null || k.keyType.getTypes()
                    .any(kt -> keyName.equals(kt.stringValue)
                        || kt.stringValue == null
                        && (!kt.isNumber() || Tls.isNum(keyName))))
                .fap(k -> k.getValueTypes()),
            opt(type.briefType.elementType().filterUnknown().filterMixed())
                .flt(it -> !it.isEmpty()).itr()
                .map(it -> new DeepType(type.definition, it, false))
        );
    }

    public static It<DeepType> getKeyPath(DeepType type, L<String> keyPath)
    {
        Mt mt = type.mt();
        for (String key: keyPath) {
            // not sure how java will behave with this `key` var
            // in lazy calls, possibly it will screw things up...
            mt = mt.getKey(key);
        }
        return mt.types.itr();
    }

    public Mt getKey(String keyName)
    {
        if (isGettingKey) { // see issue #45
            return Mt.CIRCULAR_REFERENCE;
        }
        isGettingKey = true;

        It<DeepType> keyTsIt = types.fap(t -> getKeySt(t, keyName));

        isGettingKey = false;
        return new Mt(keyTsIt.mem());
    }

    public Mt getKey(KeyType kt)
    {
        String key = Mt.getStringValueSt(kt.types);
        return getKey(key);
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

    public It<KeyEntry> getAssignedProps()
    {
        return types.fap(t -> t.props.vls());
    }

    public static PhpType joinIdeaTypes(Iterable<PhpType> ideaTypes)
    {
        PhpType ideaType = new PhpType();
        It(ideaTypes).fch(ideaType::add);
        return ideaType;
    }

    public IIt<PhpType> getIdeaTypes()
    {
        return types.map(t -> t.briefType)
            .map(PhpType::filterMixed).unq();
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
        IIt<String> strvals = types.fop(litt -> opt(litt.stringValue)
            .map(s -> {
                boolean literal = types.all((t, i) -> t.definition.getText().equals(s))
                    || litt.briefType.equals(PhpType.INT)
                    || litt.briefType.equals(PhpType.FLOAT);
                if (literal) {
                    return litt.cstName.def(s);
                } else if (litt.briefType.equals(PhpType.BOOLEAN) && "1".equals(s)) {
                    return "true";
                } else if (litt.briefType.equals(PhpType.BOOLEAN) && "0".equals(s)) {
                    return "false";
                } else {
                    return "'" + s + "'";
                }
            })).unq();
        if (strvals.has()) {
            briefValues.add(Tls.implode("|", strvals));
        }
        if (types.any(t -> t.getListElemTypes().has())) {
            briefValues.add("[" + getEl().getBriefValueText(maxLen, circularRefs) + "]");
        }
        if (briefValues.isEmpty() && types.size() > 0) {
            IIt<String> psiParts = types
                .flt(t -> t.isExactPsi)
                .fap(t -> opt(t.definition.getText()))
                .map(text -> Tls.singleLine(text, 40));
            briefValues.add(Tls.implode("|", psiParts.unq()));
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
