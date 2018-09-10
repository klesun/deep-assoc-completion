package org.klesun.deep_assoc_completion.resolvers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;

public class ArrAccRes extends Lang
{
    final private FuncCtx ctx;

    public ArrAccRes(FuncCtx ctx)
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
            .map(keyPsi -> {
                // resolving key type can be a complex operation - we don't
                // want that if we already know that mt has no known key names
                @Nullable String keyName = mt.getKeyNames().size() > 0
                    ? ctx.limitResolve(10, keyPsi).getStringValue()
                    : null;
                return mt.getKey(keyName);
            })
            .def(MultiType.INVALID_PSI);
    }
}
