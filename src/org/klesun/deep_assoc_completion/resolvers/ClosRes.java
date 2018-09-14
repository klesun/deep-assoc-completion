package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.L;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

public class ClosRes extends Lang
{
    final private FuncCtx ctx;

    public ClosRes(FuncCtx ctx)
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

    public static MultiType getReturnedValue(PsiElement funcBody, FuncCtx ctx)
    {
        return list(
            findFunctionReturns(funcBody)
                .fop(ret -> opt(ret.getArgument()))
                .fop(toCast(PhpExpression.class))
                .map(val -> ctx.findExprType(val)),
            findFunctionYields(funcBody)
                .fop(yld -> opt(yld.getArgument())
                    .fop(toCast(PhpExpression.class))
                    .map(val -> ctx.findExprType(val))
                    .map(mt -> opt(yld.getText())
                        .fop(txt -> Tls.regex("yield\\s+from[^A-Za-z].*", txt))
                        .map(txt -> mt)
                        .def(new MultiType(list(mt.getInArray(funcBody)))))
                )
        ).fap(a -> a).fap(mt -> mt.types).wap(MultiType::new);
    }

    public DeepType resolve(FunctionImpl func)
    {
        DeepType result = new DeepType(func, func.getLocalType(true));
        result.returnTypeGetters.add((funcCtx) ->
            getReturnedValue(func, funcCtx).types.arr());
        return result;
    }

}
