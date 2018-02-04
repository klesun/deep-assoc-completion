package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.*;

/**
 * go to declaration functionality for associative array keys
 */
public class DeepKeysGoToDecl extends Lang implements GotoDeclarationHandler
{
    private static PsiElement truncateOnLineBreak(PsiElement psi)
    {
        PsiElement truncated = psi.getFirstChild();
        while (psi.getText().contains("\n") && truncated != null) {
            psi = truncated;
            truncated = psi.getFirstChild();
        }
        return psi;
    }

    // just treating a symptom. i dunno why duplicates appear - they should not
    private static void removeDuplicates(L<PsiElement> psiTargets)
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
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        SearchContext search = new SearchContext().setDepth(35);
        FuncCtx funcCtx = new FuncCtx(search);

        L<PsiElement> psiTargets = L();
        opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(PhpExpression.class))
            .thn(literal -> Lang.opt(literal.getParent())
                .fop(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fop(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fop(toCast(PhpExpression.class))
                .map(srcExpr -> funcCtx.findExprType(srcExpr).types)
                .thn(arrayTypes -> arrayTypes.forEach(arrayType -> {
                    String key = funcCtx.findExprType(literal).getStringValue();
                    if (arrayType.keys.containsKey(key)) {
                        psiTargets.add(arrayType.keys.get(key).definition);
                    }
                })))
            .els(() -> opt(psiElement)
                .fop(v -> Tls.findParent(v, PhpDocParamTag.class, psi -> true))
                .fop(tag -> new DocParamRes(funcCtx).resolve(tag))
                .map(mt -> mt.types)
                .thn(types -> types.forEach(t -> psiTargets.add(t.definition))));

        removeDuplicates(psiTargets);

        return psiTargets
            .map(psi -> truncateOnLineBreak(psi))
            .toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        // dunno what this does
        return null;
    }
}
