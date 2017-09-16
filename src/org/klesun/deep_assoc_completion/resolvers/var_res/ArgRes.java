package org.klesun.deep_assoc_completion.resolvers.var_res;

import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocCommentImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocRefImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.tags.PhpDocDataProviderImpl;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.deep_assoc_completion.resolvers.MethRes;
import org.klesun.deep_assoc_completion.resolvers.NsFuncRes;
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
            .fap(parent -> Opt.fst(list(opt(null)
                , Tls.cast(FunctionReferenceImpl.class, parent)
                    .flt(call -> "array_map".equals(call.getName()))
                    .fap(call -> L(call.getParameters()).gat(1))
                , Tls.cast(MethodReferenceImpl.class, parent)
                    .flt(call -> MethRes.nameIs(call, "Fp", "map"))
                    .fap(call -> L(call.getParameters()).gat(1))
            )))
            .fap(toCast(PhpExpression.class))
            .map(arr -> trace.subCtx(L()).findExprType(arr).getEl())
            .def(MultiType.INVALID_PSI)
            ;
    }

    private MultiType resolveFromDataProviderDoc(ParameterImpl param)
    {
        return opt(param.getDocComment())
            .fap(toCast(PhpDocCommentImpl.class))
            .map(doc -> doc.getDocTagByClass(PhpDocDataProviderImpl.class))
            .fap(tags -> L(tags).fst())
            .fap(tag -> L(tag.getChildren())
                .fop(toCast(PhpDocRefImpl.class))
                .fst())
            .fap(ref -> L(ref.getReferences()).fst())
            .map(ref -> ref.resolve())
            .fap(toCast(MethodImpl.class))
            .map(met -> ClosRes.findFunctionReturns(met))
            .map(rets -> rets
                .fop(ret -> opt(ret.getArgument()))
                .fop(toCast(PhpExpression.class))
                .map(val -> trace.subCtx(L()).findExprType(val))
                .fap(mt -> mt.types))
            .map(ts -> new MultiType(ts).getEl())
            .fap(mt -> getArgOrder(param)
                .map(order -> mt.getKey(order + "")))
            .def(MultiType.INVALID_PSI);
    }

    public MultiType resolveArg(ParameterImpl param)
    {
        MultiType result = new MultiType(L());

        opt(param.getDocComment())
            .map(doc -> doc.getParamTagByName(param.getName()))
            .fap(doc -> new DocParamRes(trace).resolve(doc))
            .thn(mt -> result.types.addAll(mt.types));

        result.types.addAll(resolveFromDataProviderDoc(param).types);
        result.types.addAll(peekOutside(param).types);

        getArgOrder(param)
            .fap(i -> trace.getArg(i))
            .thn(mt -> result.types.addAll(mt.types));

        return result;
    }
}
