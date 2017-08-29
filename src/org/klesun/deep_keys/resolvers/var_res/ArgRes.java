package org.klesun.deep_keys.resolvers.var_res;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.ParameterImpl;
import com.jetbrains.php.lang.psi.elements.impl.ParameterListImpl;
import org.klesun.deep_keys.helpers.FuncCtx;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

public class ArgRes extends Lang
{
    private IFuncCtx trace;

    public ArgRes(IFuncCtx trace)
    {
        this.trace = trace;
    }

    private static Opt<Integer> getArgOrder(ParameterImpl param)
    {
        return Tls.findParent(param, ParameterListImpl.class, psi -> true)
            .map(list -> L(list.getParameters()).indexOf(param));
    }

    private MultiType peekOutside(ParameterImpl param)
    {
        return opt(param.getParent())
            .map(psi -> psi.getParent())
            .fap(toCast(FunctionImpl.class)) // closure
            .map(clos -> clos.getParent())
            .map(clos -> clos.getParent())
            .fap(toCast(ParameterListImpl.class))
            .map(argList -> argList.getParent())
            .fap(toCast(FunctionReferenceImpl.class))
            .flt(call -> "array_map".equals(call.getName()))
            .fap(call -> L(call.getParameters()).gat(1))
            .fap(toCast(PhpExpression.class))
            .map(arr -> trace.subCtx(L()).findExprType(arr).getEl())
            .def(MultiType.INVALID_PSI)
            ;
    }

    public MultiType resolveArg(ParameterImpl param)
    {
        MultiType result = new MultiType(L());

        opt(param.getDocComment())
            .map(doc -> doc.getParamTagByName(param.getName()))
            .fap(doc -> new DocParamRes(trace).resolve(doc))
            .thn(mt -> result.types.addAll(mt.types));

        result.types.addAll(peekOutside(param).types);

        getArgOrder(param)
            .fap(i -> trace.getArg(i))
            .thn(mt -> result.types.addAll(mt.types));

        return result;
    }
}
