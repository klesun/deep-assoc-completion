package org.klesun.deep_keys.entry;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.*;

/**
 * go to declaration functionality for associative array keys
 */
public class DeepKeysGoToDecl extends Lang implements GotoDeclarationHandler
{
    // just treating a symptom. i dunno why duplicates appear - they should not
    private static void removeDuplicates(List<PsiElement> psiTargets)
    {
        Set<List<String>> fingerprints = new HashSet<>();
        int size = psiTargets.size();
        for (int k = size - 1; k >= 0; --k) {
            PsiElement psi = psiTargets.get(k);
            List<String> fingerprint = list(psi.getContainingFile().getName(), psi.getTextOffset() + "");
            if (fingerprints.contains(fingerprint)) {
                psiTargets.remove(k);
            }
            fingerprints.add(fingerprint);
        }
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor)
    {
        List<PsiElement> psiTargets = new ArrayList<>();
        opt(psiElement.getParent())
            .fap(toCast(StringLiteralExpressionImpl.class))
            .thn(literal -> Lang.opt(literal.getParent())
                .fap(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fap(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .map(srcExpr -> DeepTypeResolver.findExprType(srcExpr, 20))
                .thn(arrayTypes -> arrayTypes.forEach(arrayType -> {
                    String key = literal.getContents();
                    if (arrayType.keys.containsKey(key)) {
                        psiTargets.add(arrayType.keys.get(key).definition);
                    }
                })))
            .els(() -> opt(psiElement)
                .fap(v -> Tls.findParent(v, PhpDocTag.class, psi -> true))
                .map(tag -> tag.getTagValue())
                .fap(descr -> DeepTypeResolver.parseDoc(descr, 20))
                .thn(types -> types.forEach(t -> psiTargets.add(t.definition))));

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
