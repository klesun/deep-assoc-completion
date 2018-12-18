package org.klesun.deep_assoc_completion.resolvers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.It;
import org.klesun.lang.Lang;

public class ArrAccRes extends Lang
{
    final private IExprCtx ctx;

    public ArrAccRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    public It<DeepType> resolve(ArrayAccessExpressionImpl keyAccess)
    {
        It<DeepType> tit = opt(keyAccess.getValue())
            .fop(toCast(PhpExpression.class))
            .fap(expr -> ctx.findExprType(expr));

        return opt(keyAccess.getIndex()).itr()
            .map(v -> v.getValue())
            .fop(toCast(PhpExpression.class))
            .fap(keyPsi -> {
                // resolving key type can be a complex operation - we don't
                // want that if we already know that mt has no known key names
                @Nullable String keyName = ctx.limitResolveDepth(15, keyPsi)
                    .wap(Mt::getStringValueSt);
                return tit.fap(t -> Mt.getKeySt(t, keyName));
            });
    }
}
