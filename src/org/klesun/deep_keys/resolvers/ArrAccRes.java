package org.klesun.deep_keys.resolvers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.lang.Lang;

import java.util.List;
import java.util.stream.Collectors;

public class ArrAccRes extends Lang
{
    final private IFuncCtx ctx;

    public ArrAccRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public MultiType resolve(ArrayAccessExpressionImpl keyAccess)
    {
        L<DeepType> dictTypes = opt(keyAccess.getValue())
            .fap(toCast(PhpExpression.class))
            .map(expr -> ctx.findExprType(expr).types)
            .def(L());

        List<DeepType> result = opt(keyAccess.getIndex())
            .map(v -> v.getValue())
            .fap(toCast(PhpExpression.class))
            .map(v -> ctx.findExprType(v).types)
            .fap(keyTypes -> L(keyTypes).fst())
            .map(t -> t.stringValue)
            .map(key -> dictTypes.stream()
                .map(type -> getKey(type.keys, key))
                .filter(v -> v.has())
                .map(v -> v.def(null))
                .flatMap(v -> v.types.stream())
                .collect(Collectors.toList())
            )
            .flt(types -> types.size() > 0)
            .def(dictTypes.stream()
                .map(type -> type.indexTypes)
                .flatMap(v -> v.stream())
                .collect(Collectors.toList()));

        return new MultiType(L(result));
    }
}
