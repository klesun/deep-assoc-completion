package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

public class ClosRes extends Lang
{
    final private IFuncCtx ctx;

    public ClosRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public static L<PhpReturnImpl> findFunctionReturns(PsiElement funcBody)
    {
        L<PhpReturnImpl> result = list();
        for (PsiElement child: funcBody.getChildren()) {
            // anonymous functions
            if (child instanceof Function) continue;

            Tls.cast(PhpReturnImpl.class, child)
                .thn(result::add);

            findFunctionReturns(child).forEach(result::add);
        }
        return result;
    }

    public DeepType resolve(FunctionImpl func)
    {
        // TODO: think of a way how to pass arguments here
        IFuncCtx insideCtx = ctx.subCtx(L());

        DeepType result = new DeepType(func, func.getLocalType(true));
        findFunctionReturns(func)
            .map(ret -> ret.getArgument())
            .fop(toCast(PhpExpression.class))
            .fch(retVal -> {
                F<IFuncCtx, L<DeepType>> rtGetter =
                    (funcCtx) -> funcCtx.findExprType(retVal).types;
                result.returnTypeGetters.add(rtGetter);
            });
        return result;
    }

}
