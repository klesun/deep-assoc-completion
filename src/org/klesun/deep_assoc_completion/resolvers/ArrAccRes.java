package org.klesun.deep_assoc_completion.resolvers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;

public class ArrAccRes extends Lang
{
    final private IFuncCtx ctx;

    public ArrAccRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public MultiType resolve(ArrayAccessExpressionImpl keyAccess)
    {
        MultiType mt = opt(keyAccess.getValue())
            .fop(toCast(PhpExpression.class))
            .map(expr -> ctx.findExprType(expr))
            .def(MultiType.INVALID_PSI);

        return opt(keyAccess.getIndex())
            .map(v -> v.getValue())
            .fop(toCast(PhpExpression.class))
            .map(v -> opt(ctx.findExprType(v).getStringValue()))
            .map(keyName -> mt.getKey(keyName.def(null)))
            .def(MultiType.INVALID_PSI);
    }
}
