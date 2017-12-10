package org.klesun.deep_assoc_completion;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.ArrayList;
import java.util.List;

/**
 * determines whether var declaration
 * happen before usage or not
 */
public class ScopeFinder extends Lang
{
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

    private static Opt<ControlStatementImpl> isInElseIfCondition(PsiElement varReference)
    {
        // you probably want to optimize it cuz now it scans full
        // parent chain when no elseif was found, but correct would
        // be to stop when complete expression root was found
        // oh, and isPartOf() re-iterates through parents again

        PsiElement parent = varReference;
        while (parent != null) {
            Opt<ControlStatementImpl> maybeControl = Opt.fst(list(
                opt(null)
                , Tls.cast(ElseIfImpl.class, parent).map(v -> v)
                , Tls.cast(IfImpl.class, parent).flt(v -> opt(v.getParent()).fap(toCast(ElseImpl.class)).has())
                    .map(v -> v)
            ));
            if (maybeControl.has()) {
                return maybeControl.flt(v -> isPartOf(varReference, v.getCondition()));
            }
            parent = parent.getParent();
        }
        return opt(null);
    }

    private static boolean isPartOfAssignment(PsiElement assDest, PhpExpression caretVar)
    {
        return Tls.findParent(assDest, AssignmentExpressionImpl.class, par -> par instanceof ArrayAccessExpression)
            .flt(ass -> opt(ass.getVariable()).map(var -> isPartOf(assDest, var)).def(false))
            .map(ass -> ass.getValue())
            .map(val -> isPartOf(caretVar, val))
            .def(false);
    }

    /**
     * if () {...} else {...} - true
     * if () {...} if () {...} else {...} - false
     */
    private static boolean areIfElseChained(PsiElement ifPsi, PsiElement elsePsi)
    {
        if (ifPsi instanceof If) {
            // if () {...} else {...}
            return ifPsi.isEquivalentTo(elsePsi.getParent());
        } else if (ifPsi instanceof ElseIf) {
            // ... elseif () {...} else {...}
            return opt(ifPsi.getParent())
                .map(ifPar ->
                    ifPar.isEquivalentTo(elsePsi.getParent()) &&
                    ifPar instanceof If)
                .def(false);
        } else {
            return false;
        }
    }

    private static boolean isInALoop(PsiElement stmtPsi)
    {
        return Tls.findParent(stmtPsi, For.class, psi -> !(psi instanceof Function)).map(a -> true).def(false)
            || Tls.findParent(stmtPsi, While.class, psi -> !(psi instanceof Function)).map(a -> true).def(false)
            || Tls.findParent(stmtPsi, ForeachImpl.class, psi -> !(psi instanceof Function)).map(a -> true).def(false)
            || Tls.findParent(stmtPsi, DoWhile.class, psi -> !(psi instanceof Function)).map(a -> true).def(false);
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
    public static boolean didSurelyHappen(PsiElement reference, Variable caretVar)
    {
        if (isPartOfAssignment(reference, caretVar)) {
            return false;
        } else if (caretVar.getTextOffset() < reference.getTextOffset()) {
            return false;
        }

        Opt<PsiElement> refScope = getParentScope(reference);
        List<PsiElement> varScopes = getParentScopes(caretVar);

        Opt<ControlStatementImpl> elseIf = isInElseIfCondition(reference);
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
    public static boolean didPossiblyHappen(PsiElement reference, PhpExpression caretVar)
    {
        if (isPartOfAssignment(reference, caretVar)) {
            return false;
        } else if (caretVar.getTextOffset() < reference.getTextOffset()) {
            return false;
        }

        List<PsiElement> scopesL = getParentScopes(reference);
        List<PsiElement> scopesR = getParentScopes(caretVar);

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
                PsiElement scopeL = scopesL.get(l - 1);
                PsiElement scopeR = scopesR.get(r - 1);
                return opt(scopeL.getParent())
                    .flt(ifPar -> ifPar instanceof If || ifPar instanceof ElseIf)
                    .fap(ifPar -> opt(scopeR.getParent())
                        .flt(elsePar -> elsePar instanceof Else || elsePar instanceof ElseIf)
                        .map(elsePar -> {
                            boolean incompatibleScopes = areIfElseChained(ifPar, elsePar) && !isInALoop(ifPar);
                            return !incompatibleScopes;
                        }))
                    .def(true);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}
