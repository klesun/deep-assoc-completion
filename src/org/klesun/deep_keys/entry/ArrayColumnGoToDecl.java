package org.klesun.deep_keys.entry;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_keys.helpers.FuncCtx;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.deep_keys.helpers.SearchContext;
import org.klesun.deep_keys.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * go to declaration functionality for key name in `array_column($segments, 'segmentNumber')`
 */
public class ArrayColumnGoToDecl extends Lang implements GotoDeclarationHandler
{
    // just treating a symptom. i dunno why duplicates appear - they should not
    private static void removeDuplicates(List<PsiElement> psiTargets)
    {
        Set<PsiElement> fingerprints = new HashSet<>();
        int size = psiTargets.size();
        for (int k = size - 1; k >= 0; --k) {
            if (fingerprints.contains(psiTargets.get(k))) {
                psiTargets.remove(k);
            }
            fingerprints.add(psiTargets.get(k));
        }
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor)
    {
        SearchContext search = new SearchContext().setDepth(35);
        IFuncCtx funcCtx = new FuncCtx(search, L());

        L<PsiElement> psiTargets = opt(psiElement.getParent())
            .fap(toCast(StringLiteralExpressionImpl.class))
            .map(literal -> opt(literal.getParent())
                .map(argList -> argList.getParent())
                .fap(toCast(FunctionReferenceImpl.class))
                .flt(call -> "array_column".equals(call.getName()))
                .fap(call -> L(call.getParameters()).gat(0))
                .fap(toCast(PhpExpression.class))
                .map(arr -> funcCtx.findExprType(arr).getEl())
                .map(mt -> mt.types)
                .def(L())
                .fop(arrayType -> getKey(arrayType.keys, literal.getContents()))
                .map(keyRec -> keyRec.definition))
            .def(L());

        removeDuplicates(psiTargets);

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        // dunno what this does
        return null;
    }
}
