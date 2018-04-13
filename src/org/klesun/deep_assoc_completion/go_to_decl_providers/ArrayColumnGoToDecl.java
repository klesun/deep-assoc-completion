package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Lang;

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
        var size = psiTargets.size();
        for (var k = size - 1; k >= 0; --k) {
            if (fingerprints.contains(psiTargets.get(k))) {
                psiTargets.remove(k);
            }
            fingerprints.add(psiTargets.get(k));
        }
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        var search = new SearchContext()
            .setDepth(DeepKeysPvdr.getMaxDepth(false));
        var funcCtx = new FuncCtx(search);

        var psiTargets = opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(StringLiteralExpressionImpl.class))
            .map(literal -> opt(literal.getParent())
                .map(argList -> argList.getParent())
                .fop(toCast(FunctionReferenceImpl.class))
                .flt(call -> "array_column".equals(call.getName()))
                .fop(call -> L(call.getParameters()).gat(0))
                .fop(toCast(PhpExpression.class))
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
