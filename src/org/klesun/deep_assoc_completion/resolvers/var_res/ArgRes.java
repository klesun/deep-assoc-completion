package org.klesun.deep_assoc_completion.resolvers.var_res;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocCommentImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocRefImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.tags.PhpDocDataProviderImpl;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.built_in_typedefs.Cst;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.deep_assoc_completion.resolvers.MethCallRes;
import org.klesun.deep_assoc_completion.resolvers.PsalmRes;
import org.klesun.deep_assoc_completion.resolvers.UsageResolver;
import org.klesun.deep_assoc_completion.structures.ArgOrder;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Mkt;
import org.klesun.lang.It;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

public class ArgRes extends Lang
{
    private IExprCtx trace;

    public ArgRes(IExprCtx trace)
    {
        this.trace = trace;
    }

    private static Opt<Integer> getArgOrder(ParameterImpl param)
    {
        return Tls.findParent(param, ParameterListImpl.class, psi -> true)
            .map(list -> L(list.getParameters()).indexOf(param));
    }

    private static It<VariableImpl> findVarReferences(VariableImpl caretVar)
    {
        return Tls.findParent(caretVar, GroupStatementImpl.class, a -> true)
            .fap(funcBody -> Tls.findChildren(
                funcBody, VariableImpl.class,
                subPsi -> !(subPsi instanceof FunctionImpl)
            ))
            .flt(varUsage -> caretVar.getName().equals(varUsage.getName()));
    }

    private It<DeepType> getArgFromNsFuncCall(FunctionReferenceImpl call, int argOrderOfLambda, int argOrderInLambda)
    {
        PsiElement[] params = call.getParameters();
        if (argOrderOfLambda == 0 && params.length > 1) {
            // functions where array is passed in the second argument
            if (argOrderInLambda == 0 && "array_map".equals(call.getName())) {
                return L(call.getParameters()).gat(1)
                    .fop(toCast(PhpExpression.class))
                    .fap(arr -> trace.subCtxEmpty().findExprType(arr).fap(Mt::getElSt));
            }
        } else if (argOrderOfLambda == 1 && params.length > 1) {
            // functions where array is passed in the first argument
            if (argOrderInLambda == 0 && "array_filter".equals(call.getName()) ||
                argOrderInLambda == 0 && "array_walk".equals(call.getName()) ||
                argOrderInLambda == 0 && "array_walk_recursive".equals(call.getName()) ||
                argOrderInLambda == 0 && "usort".equals(call.getName()) ||
                argOrderInLambda == 1 && "usort".equals(call.getName()) ||
                argOrderInLambda == 1 && "array_reduce".equals(call.getName())
            ) {
                return L(call.getParameters()).gat(0)
                    .fop(toCast(PhpExpression.class))
                    .fap(arr -> trace.subCtxEmpty().findExprType(arr).fap(Mt::getElSt));
            } else if (argOrderInLambda == 0 && "pcntl_signal".equals(call.getName())) {
                return Mkt.cst(trace.subCtxEmpty(), Cst.SIG.map(a -> a.a));
            } else if (argOrderInLambda == 1 && "pcntl_signal".equals(call.getName())) {
                return som(Mkt.assoc(call, list(
                    T2("signo", Mkt.inte(call, 11).mt()),
                    T2("errno", Mkt.inte(call, 0).mt()),
                    T2("code", Mkt.inte(call, 0).mt()),
                    T2("addr", Mkt.inte(call, "4294967314173").mt())
                ))).itr();
            }
        }
        return It.non();
    }

    private It<DeepType> getArgFromMethodCall(MethodReferenceImpl call, int argOrderOfLambda, int argOrderInLambda)
    {
        PsiElement[] params = call.getParameters();
        if (argOrderOfLambda == 0 && params.length > 1 && argOrderInLambda == 0) {
            // TODO: remove Fp-specific functions once resolveArgCallArrKeys() supports built-ins
            if (MethCallRes.nameIs(call, "Fp", "map") ||
                MethCallRes.nameIs(call, "Fp", "filter") ||
                MethCallRes.nameIs(call, "Fp", "any") ||
                MethCallRes.nameIs(call, "Fp", "all") ||
                MethCallRes.nameIs(call, "Fp", "sortBy")
            ) {
                return L(call.getParameters()).gat(1)
                    .fop(toCast(PhpExpression.class))
                    .fap(arr -> trace.subCtxEmpty()
                        .findExprType(arr).fap(Mt::getElSt));
            }
        }
        return It.non();
    }

    private It<DeepType> getArgPassedTo(PhpExpression funcVar, int caretArgOrder)
    {
        return opt(funcVar.getParent())
            .fap(parent -> It.frs(() -> It.non()
                // like array_map($mapper, ['a' => 5, 'b' => 6])
                // or SomeCls::doSomething($mapper)
                , () -> Tls.cast(ParameterListImpl.class, parent)
                    .fap(parl -> opt(parl.getParent())
                        .fop(toCast(FunctionReference.class))
                        .fap(call -> {
                            IExprCtx subCtx = trace.subCtxDirect(call);
                            int funcVarOrder = L(parl.getParameters()).indexOf(funcVar);
                            return It.frs(() -> It.non()
                                , () -> Tls.cast(FunctionReferenceImpl.class, call)
                                    .fap(func -> getArgFromNsFuncCall(func, funcVarOrder, caretArgOrder))
                                , () -> Tls.cast(MethodReferenceImpl.class, call)
                                    .fap(func -> getArgFromMethodCall(func, funcVarOrder, caretArgOrder))
                                // go into the called function and look what
                                // is passed to the passed func var there
                                , () -> It(call.multiResolve(false))
                                    .fop(res -> opt(res.getElement()))
                                    .fop(toCast(Function.class))
                                    .fap(func -> new UsageResolver(subCtx, 0)
                                        .resolveArgCallArrKeys(func, funcVarOrder, caretArgOrder))
                            );
                        }))
                // like $mapper(['a' => 5, 'b' => 6])
                , () -> Tls.cast(FunctionReferenceImpl.class, parent)
                    .fop(call -> L(call.getParameters()).gat(caretArgOrder))
                    .fop(toCast(PhpExpression.class))
                    .fap(arg -> trace.subCtxEmpty().findExprType(arg))
            ));
    }

    // $getAirline = function($seg){return $seg['airline'];};
    // array_map($getAirline, $segments);
    private It<DeepType> getFuncVarUsageArg(PsiElement closEpxr, int argOrderInLambda)
    {
        return opt(closEpxr)
            .map(expr -> expr.getParent())
            .fop(toCast(AssignmentExpressionImpl.class))
            .fap(ass -> opt(ass.getParent())
                .fop(toCast(StatementImpl.class))
                .map(state -> state.getNextPsiSibling())
                .fap(nextSt -> {
                    int startOffset = nextSt.getTextOffset();
                    return opt(ass.getVariable())
                        .fop(toCast(VariableImpl.class))
                        .fap(variable -> findVarReferences(variable))
                        .fop(res -> opt(res.getElement()))
                        .flt(ref -> ref.getTextOffset() >= startOffset)
                        .fop(toCast(VariableImpl.class))
                        .fap(ref -> getArgPassedTo(ref, argOrderInLambda));
                }));
    }

    // $result = static::doSomething($args);
    private It<DeepType> getPrivateFuncUsageArg(FunctionImpl func, int argOrderInLambda)
    {
        return Tls.cast(MethodImpl.class, func)
            .flt(a -> func.getParameters().length > 0)
            .fap(meth -> {
                PsiFile file = func.getContainingFile();
                return It.cnc(
                    It(PsiTreeUtil.findChildrenOfType(file, MethodReferenceImpl.class))
                        .flt(call -> meth.getName().equals(call.getName()))
                        .flt(call -> opt(call.getClassReference()).map(ref -> ref.getText())
                            .flt(txt -> txt.equals("$this") || txt.equals("self") ||
                                        txt.equals("static") || func.equals(call.resolve()))
                            .has())
                        .fop(call -> L(call.getParameters()).gat(argOrderInLambda))
                        .fop(toCast(PhpExpression.class))
                        .fap(arg -> trace.subCtxEmpty().findExprType(arg)),
                    It(PsiTreeUtil.findChildrenOfType(file, ArrayCreationExpressionImpl.class))
                        .flt(arr -> arr.getChildren().length == 2
                                && L(arr.getChildren()).gat(0)
                                    .flt(psi -> psi.getText().equals("$this")
                                            || psi.getText().equals("self::class")
                                            || psi.getText().equals("static::class"))
                                    .has()
                                && L(arr.getChildren()).gat(1).map(psi -> psi.getFirstChild())
                                    .fop(toCast(StringLiteralExpression.class))
                                    .flt(str -> str.getContents().equals(meth.getName()))
                                    .has())
                        .fap(arr -> It.frs(
                            () -> new ArgRes(trace.subCtxEmpty())
                                .getInlineFuncArg(arr, argOrderInLambda),
                            () -> new ArgRes(trace.subCtxEmpty())
                                .getFuncVarUsageArg(arr, argOrderInLambda)
                        ))
                );
            });
    }

    // array_map(function($seg){return $seg['airline'];}, $segments);
    private It<DeepType> getInlineFuncArg(@Nullable PsiElement funcExpr, int argOrderInLambda)
    {
        return opt(funcExpr)
            .fop(toCast(PhpExpression.class))
            .fap(call -> getArgPassedTo(call, argOrderInLambda));
    }

    private It<DeepType> peekOutside(ParameterImpl param)
    {
        return opt(param.getParent())
            .map(paramList -> paramList.getParent())
            .fop(toCast(FunctionImpl.class)) // closure
            .fap(clos -> getArgOrder(param)
                .fap(order -> It.frs(() -> It.non()
                    , () -> getInlineFuncArg(clos.getParent(), order)
                    , () -> getFuncVarUsageArg(clos.getParent(), order)
                    , () -> getPrivateFuncUsageArg(clos, order)
                )))
            ;
    }

    private It<DeepType> resolveFromDataProviderDoc(ParameterImpl param)
    {
        return opt(param.getDocComment())
            .fop(toCast(PhpDocCommentImpl.class))
            .map(doc -> doc.getDocTagByClass(PhpDocDataProviderImpl.class))
            .fop(tags -> It(tags).fst())
            .fop(tag -> It(tag.getChildren())
                .fop(toCast(PhpDocRefImpl.class))
                .fst())
            .fop(ref -> It(ref.getReferences()).fst())
            .map(ref -> ref.resolve())
            .fop(toCast(MethodImpl.class))
            .fap(met -> ClosRes.getReturnedValue(met, trace.subCtxEmpty()))
            .fap(t -> Mt.getElSt(t))
            .fap(t -> getArgOrder(param)
                .fap(order -> Mt.getKeySt(t, order + "")));
    }

    public It<DeepType> resolveArg(ParameterImpl param)
    {
        int order = getArgOrder(param).def(-1);

        It<ParameterImpl> decls = It.cnc(
            // get doc comments from the initial abstract method if any
            opt(param.getParent()).itr()
                .fop(Tls.toCast(ParameterListImpl.class))
                .map(lst -> lst.getParent())
                .fop(Tls.toCast(MethodImpl.class))
                .fap(meth -> opt(meth.getContainingClass())
                    .fap(cls -> It(cls.getSupers())
                        .fap(ifc -> ifc.getMethods())
                        .flt(ifcMeth -> meth.getName().equals(ifcMeth.getName()))))
                .fop(meth -> L(meth.getParameters()).gat(order))
                .fop(Tls.toCast(ParameterImpl.class)),
            // get doc from current method
            list(param)
        );

        Opt<Method> methOpt = Tls.getParents(param)
            .cst(Method.class).fst();
        IExprCtx clsCtx = methOpt
            .fop(m -> Tls.getParents(m)
                .cst(PhpClass.class).fst()
                .flt(clsPsi -> !trace.func().areArgsKnown())
                .map(clsPsi -> m.isStatic()
                    ? trace.subCtxSelfCls(clsPsi)
                    : trace.subCtxThisCls(clsPsi)))
            .def(trace);

        It<DeepType> declTit = decls
            .fap(arg -> It.cnc(
                opt(arg.getDocComment())
                    .fop(doc -> opt(doc.getParamTagByName(param.getName())))
                    .fap(doc -> new DocParamRes(clsCtx).resolve(doc)),
                opt(arg.getParent()).fap(lst -> opt(lst.getParent()))
                    .cst(Function.class)
                    .fap(func -> It.cnc(
                        UsageResolver.findMetaArgType(func, order, clsCtx),
                        opt(func.getDocComment())
                            .fap(doc -> PsalmRes.resolveVar(doc, param.getName(), clsCtx))

                    )),
                opt(arg.getDefaultValue()).itr()
                    .cst(PhpExpression.class)
                    .fap(xpr -> trace.subCtxEmpty().findExprType(xpr))
            ));
        // treat empty args as any args in the doc,
        // since it's a pain to list all args every time
        boolean isNoArgDoc = !trace.func().hasArgs() && trace.isInComment();
        It<DeepType> genericTit = It();
        if (!trace.func().areArgsKnown() || isNoArgDoc) {
            // passed args not known - if caret was inside this function
            genericTit = It(peekOutside(param));
        } else {
            genericTit = getArgOrder(param)
                .fap(i -> {
                    if (param.getText().startsWith("...")) {
                        return trace.func().getArg(new ArgOrder(i, true)).fap(a -> a.types);
                    } else {
                        return trace.func().getArg(i).fap(mt -> mt.types);
                    }
                });
        }
        return It.cnc(
            declTit,
            resolveFromDataProviderDoc(param),
            genericTit
        );
    }
}
