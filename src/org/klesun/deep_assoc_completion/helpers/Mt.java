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
    final public L<DeepType> types;

    private boolean isGettingKey = false;

    public Mt(Iterable<DeepType> types, REASON reason)
    {
        // there sometimes appear thousands of duplicates
        // I'm not sure I'm good mathematician enough to find
        // out the algorithm that would not produce them with
        // all these recursions, so I'm just removing dupes here
        Set occurences = new HashSet<>();
        this.types = L(It(types).unq());
        this.reason = reason;
    }
    public Mt(Iterable<DeepType> types)
    {
        this(types, REASON.OK);
    }

    public static DeepType getInArraySt(It<DeepType> types, PsiElement call)
    {
        DeepType result = new DeepType(call, PhpType.ARRAY);
        result.listElTypes.add(Tls.onDemand(() -> new Mt(types)));
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
            ++i;
        }
        return value;
    }

    public String getStringValue()
    {
        return getStringValueSt(types);
    }

    // TODO: return iterator
    public L<String> getStringValues()
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
            Tls.ife(keyName != null, () -> It.cnc(
                Lang.getKey(type.keys, keyName)
                    .fap(v -> v.getTypes()),
                Tls.ifi(Tls.isNum(keyName), () -> type.listElTypes.fap(t -> t.get().types))
            ), () -> It.cnc(
                It(type.keys.values())
                    .fap(v -> v.getTypes()),
                type.listElTypes.fap(t -> t.get().types)
            )),
            type.anyKeyElTypes.fap(t -> t.get().types),
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

        L<DeepType> keyTsIt = types.fap(t -> getKeySt(t, keyName)).arr();

        isGettingKey = false;
        return new Mt(keyTsIt);
    }

    public static PhpType getKeyBriefTypeSt(Iterable<PhpType> ideaTypes)
    {
        PhpType ideaType = new PhpType();
        ideaTypes.forEach(ideaType::add);
        return ideaType;
    }

    public PhpType getKeyBriefType(@NonNull String keyName)
    {
        PhpType ideaType = new PhpType();
        Map<PsiElement, L<PhpType>> psiToType = new LinkedHashMap<>();
        It<DeepType.Key> keyObjs = types.itr().fop(t -> Lang.getKey(t.keys, keyName));
        // getting rid of duplicates, temporary solution
        // TODO: 7681 types!!! and only 2 of them are actually unique. should do something
        keyObjs.fch(k -> psiToType.put(k.definition, k.getBriefTypes()));

        L(psiToType.values()).fap(a -> a).fch(ideaType::add);
        return ideaType;
    }

    public L<String> getKeyNames()
    {
        L<String> names = L();
        HashSet<String> repeations = new HashSet<>();
        types.fap(t -> L(t.keys.keySet())).fch(name -> {
            if (!repeations.contains(name)) {
                repeations.add(name);
                names.add(name);
            }
        });
        return names;
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
        L<String> keyNames = getKeyNames();

        if (keyNames.size() > 0) {
            if (keyNames.all((k,i) -> (k + "").equals(i + ""))) {
                briefValues.add("(" + Tls.implode(", ", keyNames.map(i -> getKey(i + "")
                    .getBriefValueText(15, circularRefs))) + ")");
            } else {
                briefValues.add("{" + Tls.implode(", ", keyNames.map(k -> k + ":")) + "}");
            }
        }
        L<String> strvals = types.fop(t -> opt(t.stringValue));
        if (strvals.size() > 0) {
            briefValues.add(Tls.implode("|", strvals
                .grp(a -> a).kys()
                .map(s -> (types.all((t,i) -> t.definition.getText().equals(s)))
                    ? s : "'" + s + "'")));
        }
        if (types.any(t -> t.getListElemTypes().has())) {
            briefValues.add("[" + getEl().getBriefValueText(maxLen, circularRefs) + "]");
        }
        if (briefValues.isEmpty() && types.size() > 0) {
            L<String> psiParts = types.flt(t -> t.isExactPsi).map(t -> Tls.singleLine(t.definition.getText(), 40));
            briefValues.add(Tls.implode("|", psiParts.grp(a -> a).kys()));
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
        return types.any(t -> t.hasNumberIndexes() || t.listElTypes.size() > 0);
    }

    public boolean isInt()
    {
        return types.any(t -> t.isNumber());
    }
}
