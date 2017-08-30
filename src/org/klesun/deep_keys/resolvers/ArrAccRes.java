package org.klesun.deep_keys.resolvers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
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
            .fap(toCast(PhpExpression.class))
            .map(expr -> ctx.findExprType(expr))
            .def(MultiType.INVALID_PSI);

        return opt(keyAccess.getIndex())
            .map(v -> v.getValue())
            .fap(toCast(PhpExpression.class))
            .map(v -> opt(ctx.findExprType(v).getStringValue()))
            .map(keyName -> mt.getKey(keyName.def(null)))
            .def(MultiType.INVALID_PSI);
    }
}
