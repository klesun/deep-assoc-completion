package org.klesun.deep_keys.entry;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.lang.Lang;

import java.util.ArrayList;
import java.util.Collection;

/**
 * go to declaration functionality for associative array keys
 */
public class DeepKeysGoToDecl extends Lang implements GotoDeclarationHandler
{
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor)
    {
        Collection<PsiElement> psiTargets = new ArrayList<>();
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
                })));

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        // dunno what this does
        return null;
    }
}
