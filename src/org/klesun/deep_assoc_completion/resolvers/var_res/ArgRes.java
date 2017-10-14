package org.klesun.deep_assoc_completion.resolvers.var_res;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocCommentImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocRefImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.tags.PhpDocDataProviderImpl;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.deep_assoc_completion.resolvers.MethRes;
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

    private static L<VariableImpl> findVarReferences(VariableImpl caretVar)
    {
        return Tls.findParent(caretVar, GroupStatementImpl.class, a -> true)
            .map(funcBody -> Tls.findChildren(
                funcBody, VariableImpl.class,
                subPsi -> !(subPsi instanceof FunctionImpl)
            ))
            .def(L())
            .flt(varUsage -> caretVar.getName().equals(varUsage.getName()));
    }

    private Opt<MultiType> getArgFromNsFuncCall(FunctionReferenceImpl call, int lambdaArgOrder)
    {
        PsiElement[] params = call.getParameters();
        if (lambdaArgOrder == 0 && params.length > 1) {
            // functions where array is passed in the second argument
            if ("array_map".equals(call.getName())) {
                return L(call.getParameters()).gat(1)
                    .fap(toCast(PhpExpression.class))
                    .map(arr -> trace.subCtx(L()).findExprType(arr).getEl());
            }
        } else if (lambdaArgOrder == 1 && params.length > 1) {
            // functions where array is passed in the first argument
            if ("array_filter".equals(call.getName()) ||
                "array_walk".equals(call.getName()) ||
                "array_walk_recursive".equals(call.getName()) ||
                "usort".equals(call.getName())
            ) {
                return L(call.getParameters()).gat(0)
                    .fap(toCast(PhpExpression.class))
                    .map(arr -> trace.subCtx(L()).findExprType(arr).getEl());
            }
        }
        return opt(null);
    }

    private Opt<MultiType> getArgPassedTo(VariableImpl variable)
    {
        return opt(variable.getParent())
            .fap(parent -> Opt.fst(list(opt(null)
                , Tls.cast(ParameterListImpl.class, parent)
                    .fap(parl -> opt(parl.getParent())
                        .fap(toCast(FunctionReferenceImpl.class))
                        .fap(func -> getArgFromNsFuncCall(func, L(parl.getParameters()).indexOf(variable))))
            )));
    }

    // $getAirline = function($seg){return $seg['airline'];};
    // array_map($getAirline, $segments);
    private Opt<MultiType> getFuncVarUsageArg(FunctionImpl clos, int order)
    {
        return opt(clos.getParent())
            .map(expr -> expr.getParent())
            .fap(toCast(AssignmentExpressionImpl.class))
            .fap(ass -> opt(ass.getParent())
                .fap(toCast(StatementImpl.class))
                .map(state -> state.getNextPsiSibling())
                .map(nextSt -> {
                    int startOffset = nextSt.getTextOffset();
                    return opt(ass.getVariable())
                        .fap(toCast(VariableImpl.class))
                        .map(variable -> findVarReferences(variable))
                        .def(L())
                        .fop(res -> opt(res.getElement()))
                        .flt(ref -> ref.getTextOffset() >= startOffset)
                        .fop(toCast(VariableImpl.class))
                        .fop(ref -> getArgPassedTo(ref));
                })
                .map(mts -> new MultiType(mts.fap(mt -> mt.types))));
    }

    // array_map(function($seg){return $seg['airline'];}, $segments);
    private Opt<MultiType> getInlineFuncArg(FunctionImpl clos, int order)
    {
        return opt(clos.getParent())
            .flt(expr -> order == 0) //TODO: fix, this is not arg order of lambda, but arg order _in_ lambda
            .map(expr -> expr.getParent())
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
            .map(arr -> trace.subCtx(L()).findExprType(arr).getEl());
    }

    private MultiType peekOutside(ParameterImpl param)
    {
        return opt(param.getParent())
            .map(paramList -> paramList.getParent())
            .fap(toCast(FunctionImpl.class)) // closure
            .fap(clos -> getArgOrder(param)
                .fap(order -> Opt.fst(list(opt(null)
                    , getInlineFuncArg(clos, order)
                    , getFuncVarUsageArg(clos, order)
                ))))
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
