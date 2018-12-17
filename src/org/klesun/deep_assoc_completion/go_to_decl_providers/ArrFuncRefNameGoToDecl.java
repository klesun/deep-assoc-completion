package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.ArrFuncRefNamePvdr;
import org.klesun.lang.L;
import org.klesun.lang.Lang;

/**
 * go to method name in such construction: [self::class, 'soSomeStuff']
 */
public class ArrFuncRefNameGoToDecl extends Lang implements GotoDeclarationHandler
{
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        L<Method> psiTargets = opt(psiElement)
            .map(psi -> psi.getParent())
            .cst(StringLiteralExpressionImpl.class)
            .fap(literal -> ArrFuncRefNamePvdr.resolve(literal, true, editor)
                .flt(meth -> meth.getName().equals(literal.getContents()))
            ).arr();

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }
}
