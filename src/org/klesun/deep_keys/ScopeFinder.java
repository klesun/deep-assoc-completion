package org.klesun.deep_keys;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ElseIfImpl;
import com.jetbrains.php.lang.psi.elements.impl.GroupStatementImpl;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.ArrayList;
import java.util.List;

/**
 * determines whether var declaration
 * happen before usage or not
 */
public class ScopeFinder
{
    private static <T> Opt<T> opt(T value)
    {
        return new Opt(value);
    }

    private static Opt<PsiElement> getParentScope(PsiElement psi)
    {
        PsiElement next = psi.getParent();
        while (next != null) {
            Opt<PsiElement> scopeMb = Tls.cast(GroupStatementImpl.class, next).map(v -> v);
            if (scopeMb.has()) {
                return scopeMb;
            }
            next = next.getParent();
        }
        return new Opt(null);
    }

    private static List<PsiElement> getParentScopes(PsiElement psi)
    {
        List<PsiElement> result = new ArrayList<>();
        Opt<PsiElement> next = getParentScope(psi);
        while (next.has()) {
            next.thn(result::add);
            next = getParentScope(next.def(null));
        }
        return result;
    }

    private static boolean isPartOf(PsiElement child, PsiElement grandParent)
    {
        PsiElement parent = child;
        while (parent != null) {
            if (parent.isEquivalentTo(grandParent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static Opt<ElseIfImpl> isInElseIfCondition(PsiElement varReference)
    {
        // you probably want to optimize it cuz now it scans full
        // parent chain when no elseif was found, but correct would
        // be to stop when complete expression root was found
        // oh, and isPartOf() re-iterates through parents again

        PsiElement parent = varReference;
        while (parent != null) {
            Opt<ElseIfImpl> elseIf = Tls.cast(ElseIfImpl.class, parent);
            if (elseIf.has()) {
                return elseIf.flt(v -> isPartOf(varReference, v.getCondition()));
            }
            parent = parent.getParent();
        }
        return opt(null);
    }

    /**
     * // and this will be true
     * $someVar = ['someKey' => 'dsa'];
     * if (...) {
     *     // and this will be false
     *     $someVar = 456;
     * }
     * if (...) {
     *     // and this will be false
     *     $someVar = 123;
     * } else {
     *     // this will be true
     *     $someVar = ['someKey' => 'asd'];
     *     // for this statement
     *     print($someVar['someKey']);
     * }
     */
    public static boolean didSurelyHappen(PsiElement reference, Variable usage)
    {
        if (usage.getTextOffset() < reference.getTextOffset()) {
            return false;
        }

        Opt<PsiElement> refScope = getParentScope(reference);
        List<PsiElement> varScopes = getParentScopes(usage);

        Opt<ElseIfImpl> elseIf = isInElseIfCondition(reference);
        if (elseIf.has()) {
            for (PsiElement part: elseIf.def(null).getChildren()) {
                if (part instanceof GroupStatement) {
                    return varScopes.stream().anyMatch(s -> s.isEquivalentTo(part));
                }
            }
            return false;
        }

        return refScope
            .map(declScope -> varScopes
                .stream()
                .anyMatch(scope -> scope.isEquivalentTo(declScope)))
            .def(false);
    }

    /**
     * // and this will be true
     * $someVar = ['someKey' => 'dsa'];
     * if (...) {
     *     // and this will be true
     *     $someVar = 456;
     * }
     * if (...) {
     *     // and this will be false
     *     $someVar = 123;
     * } else {
     *     // this will be true
     *     $someVar = ['someKey' => 'asd'];
     *     // for this statement
     *     print($someVar['someKey']);
     * }
     */
    public static boolean didPossiblyHappen(PsiElement reference, Variable usage)
    {
        if (usage.getTextOffset() < reference.getTextOffset()) {
            return false;
        }

        List<PsiElement> scopesL = getParentScopes(reference);
        List<PsiElement> scopesR = getParentScopes(usage);

        int l = 0;
        int r = 0;

        // 1. find deepest same scope
        while (l < scopesL.size() && r < scopesR.size()) {
            if (scopesL.get(l).getTextOffset() < scopesR.get(r).getTextOffset()) {
                ++r;
            } else if (scopesL.get(l).getTextOffset() > scopesR.get(r).getTextOffset()) {
                ++l;
            } else {
                // could check offset of parent/child to handle
                // cases when different psi have same offset
                // or use textrange
                break;
            }
        }

        // 2. if right inside it are (if|elseif) and (elseif|else) respectively, then false
        if (l < scopesL.size() && r < scopesR.size()) {
            if (l > 0 && r > 0) {
                boolean lIsIf = opt(scopesL.get(l - 1).getParent())
                    .map(v -> v instanceof If || v instanceof ElseIf)
                    .def(false);
                boolean rIsElse = opt(scopesR.get(r - 1).getParent())
                    .map(v -> v instanceof Else || v instanceof ElseIf)
                    .def(false);
                boolean incompatibleScopes = lIsIf && rIsElse;

                return !incompatibleScopes;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}
