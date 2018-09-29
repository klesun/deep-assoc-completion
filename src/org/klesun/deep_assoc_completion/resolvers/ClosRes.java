package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.*;

public class ClosRes extends Lang
{
    final private FuncCtx ctx;

    public ClosRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    // for some reason this implementation is 100 ms slower than the one that returns a list
//    public static It<PhpReturnImpl> findFunctionReturns(PsiElement funcBody)
//    {
//        return It(funcBody.getChildren()).fap(child -> {
//            if (child instanceof Function) {
//                return It.non(); // anonymous function, don't go deeper
//            } else {
//                return It.cnc(
//                    Tls.cast(PhpReturnImpl.class, child),
//                    findFunctionReturns(child)
//                );
//            }
//        });
//    }

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

    private static L<PhpYield> findFunctionYields(PsiElement funcBody)
    {
        L<PhpYield> result = list();
        for (PsiElement child: funcBody.getChildren()) {
            // anonymous functions
            if (child instanceof Function) continue;

            Tls.cast(PhpYield.class, child)
                .thn(result::add);

            findFunctionYields(child).forEach(result::add);
        }
        return result;
    }

    public static Mt getReturnedValue(PsiElement funcBody, FuncCtx ctx)
    {
        return It.cnc(
            findFunctionReturns(funcBody)
                .fop(ret -> opt(ret.getArgument()))
                .fop(toCast(PhpExpression.class))
                .fap(val -> ctx.findExprType(val)),
            findFunctionYields(funcBody)
                .fap(yld -> opt(yld.getArgument()).itr()
                    .fop(toCast(PhpExpression.class))
                    .map(val -> ctx.findExprType(val))
                    .fap(tit -> opt(yld.getText())
                        .fop(txt -> Tls.regex("yield\\s+from[^A-Za-z].*", txt))
                        .uni(txt -> tit, () -> list(Mt.getInArraySt(tit, funcBody))))
                )
        ).wap(types -> new Mt(types));
    }

    public DeepType resolve(FunctionImpl func)
    {
        DeepType result = new DeepType(func, func.getLocalType(true));
        result.returnTypeGetters.add((funcCtx) ->
            getReturnedValue(func, funcCtx).types);
        return result;
    }
}
