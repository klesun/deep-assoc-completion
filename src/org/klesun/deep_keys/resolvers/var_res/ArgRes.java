package org.klesun.deep_keys.resolvers.var_res;

import com.jetbrains.php.lang.psi.elements.impl.ParameterImpl;
import com.jetbrains.php.lang.psi.elements.impl.ParameterListImpl;
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
        // TODO: test!
        return Tls.findParent(param, ParameterListImpl.class, psi -> true)
            .map(list -> L(list.getParameters()).indexOf(param));
    }

    public MultiType resolveArg(ParameterImpl param)
    {
        MultiType result = new MultiType(L());

        opt(param.getDocComment())
            .map(doc -> doc.getParamTagByName(param.getName()))
            .fap(doc -> new DocParamRes(trace).resolve(doc))
            .thn(mt -> result.types.addAll(mt.types));

        getArgOrder(param)
            .fap(i -> trace.getArg(i))
            .thn(mt -> result.types.addAll(mt.types));

        return result;
    }
}
