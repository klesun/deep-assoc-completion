package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.Lang;

/**
 * go to method name in such construction: [self::class, 'soSomeStuff']
 * IDEA already provides such functionality, but only when you pass
 * such array somewhere where it is explicitly said to be `callable`
 * this provider, on the other hand, resolves method  _always_
 */
public class ArrFuncRefGoToDecl extends Lang implements GotoDeclarationHandler
{
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        SearchContext search = new SearchContext()
            .setDepth(DeepKeysPvdr.getMaxDepth(false, editor.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        L<Method> psiTargets = opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(StringLiteralExpressionImpl.class))
            .fap(literal -> opt(literal.getParent())
                .map(arrVal -> arrVal.getParent())
                .fop(toCast(ArrayCreationExpressionImpl.class))
                .map(arrCtor -> L(arrCtor.getChildren()))
                .flt(params -> params.size() == 2)
                .flt(params -> literal.isEquivalentTo(params.get(1).getFirstChild()))
                .fop(params -> params.gat(0))
                .fap(clsPsi -> list(
                    ArrCtorRes.resolveClass(clsPsi)
                        .fap(cls -> L(cls.getMethods())
                            .flt(meth -> meth.isStatic()))
                    ,
                    new ArrCtorRes(funcCtx).resolveInstance(clsPsi)
                        .fap(cls -> L(cls.getMethods())
                            .flt(meth -> meth.getMethodType(false) != Method.MethodType.CONSTRUCTOR)
                            .flt(meth -> !meth.isStatic()))))
                .fap(a -> a)
                .flt(meth -> meth.getName().equals(literal.getContents()))
            );

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext)
    {
        return null;
    }
}
