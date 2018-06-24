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
import org.klesun.deep_assoc_completion.ScopeFinder;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.deep_assoc_completion.resolvers.KeyUsageResolver;
import org.klesun.deep_assoc_completion.resolvers.MethCallRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

public class ArgRes extends Lang
{
    private FuncCtx trace;

    public ArgRes(FuncCtx trace)
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

    private Opt<MultiType> getArgFromNsFuncCall(FunctionReferenceImpl call, int argOrderOfLambda, int argOrderInLambda)
    {
        PsiElement[] params = call.getParameters();
        if (argOrderOfLambda == 0 && params.length > 1) {
            // functions where array is passed in the second argument
            if (argOrderInLambda == 0 && "array_map".equals(call.getName())) {
                return L(call.getParameters()).gat(1)
                    .fop(toCast(PhpExpression.class))
                    .map(arr -> new FuncCtx(trace.getSearch()).findExprType(arr).getEl());
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
                    .map(arr -> new FuncCtx(trace.getSearch()).findExprType(arr).getEl());
            }
        }
        return opt(null);
    }

    private Opt<MultiType> getArgFromMethodCall(MethodReferenceImpl call, int argOrderOfLambda, int argOrderInLambda)
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
                    .map(arr -> new FuncCtx(trace.getSearch()).findExprType(arr).getEl());
            }
        }
        return opt(null);
    }

    private Opt<MultiType> getArgPassedTo(PhpExpression funcVar, int caretArgOrder)
    {
        return opt(funcVar.getParent())
            .fop(parent -> Opt.fst(list(opt(null)
                // like array_map($mapper, ['a' => 5, 'b' => 6])
                // or SomeCls::doSomething($mapper)
                , Tls.cast(ParameterListImpl.class, parent)
                    .fop(parl -> opt(parl.getParent())
                        .fop(toCast(FunctionReference.class))
                        .fop(call -> {
                            FuncCtx subCtx = trace.subCtxDirect(call);
                            int funcVarOrder = L(parl.getParameters()).indexOf(funcVar);
                            return Opt.fst(list(opt(null)
                                , Tls.cast(FunctionReferenceImpl.class, call)
                                    .fop(func -> getArgFromNsFuncCall(func, funcVarOrder, caretArgOrder))
                                , Tls.cast(MethodReferenceImpl.class, call)
                                    .fop(func -> getArgFromMethodCall(func, funcVarOrder, caretArgOrder))
                                // go into the called function and look what
                                // is passed to the passed func var there
                                , L(call.multiResolve(false))
                                    .fop(res -> opt(res.getElement()))
                                    .fop(toCast(Function.class))
                                    .map(func -> new KeyUsageResolver(subCtx, 3)
                                        .resolveArgCallArrKeys(func, funcVarOrder, caretArgOrder))
                                    .fap(mt -> mt.types)
                                    .wap(types -> opt(new MultiType(types)))
                            ));
                        }))
                // like $mapper(['a' => 5, 'b' => 6])
                , Tls.cast(FunctionReferenceImpl.class, parent)
                    .fop(call -> L(call.getParameters()).gat(caretArgOrder))
                    .fop(toCast(PhpExpression.class))
                    .map(arg -> new FuncCtx(trace.getSearch()).findExprType(arg))
            )));
    }

    // $getAirline = function($seg){return $seg['airline'];};
    // array_map($getAirline, $segments);
    private Opt<MultiType> getFuncVarUsageArg(PsiElement closEpxr, int argOrderInLambda)
    {
        return opt(closEpxr)
            .map(expr -> expr.getParent())
            .fop(toCast(AssignmentExpressionImpl.class))
            .fop(ass -> opt(ass.getParent())
                .fop(toCast(StatementImpl.class))
                .map(state -> state.getNextPsiSibling())
                .map(nextSt -> {
                    int startOffset = nextSt.getTextOffset();
                    return opt(ass.getVariable())
                        .fop(toCast(VariableImpl.class))
                        .map(variable -> findVarReferences(variable))
                        .def(L())
                        .fop(res -> opt(res.getElement()))
                        .flt(ref -> ref.getTextOffset() >= startOffset)
                        .fop(toCast(VariableImpl.class))
                        .fop(ref -> getArgPassedTo(ref, argOrderInLambda));
                })
                .map(mts -> new MultiType(mts.fap(mt -> mt.types))));
    }

    // $result = static::doSomething($args);
    private Opt<MultiType> getPrivateFuncUsageArg(FunctionImpl func, int argOrderInLambda)
    {
        // it would be nice to also infer arg type when function is
        // called with array_map([$this, 'doStuff'], $args) one day...
        // would just Ctrl+F-ing the file for function name be slower?

        return Tls.cast(MethodImpl.class, func)
            // if caret is inside this function, when passed args are unknown
            .flt(a -> !trace.hasArgs())
            .flt(a -> func.getParameters().length > 0)
            .map(meth -> {
                PsiFile file = func.getContainingFile();
                return list(
                    L(PsiTreeUtil.findChildrenOfType(file, MethodReferenceImpl.class))
                        .flt(call -> meth.getName().equals(call.getName()))
                        .flt(call -> opt(call.getClassReference()).map(ref -> ref.getText())
                            .flt(txt -> txt.equals("$this") || txt.equals("self") || txt.equals("static"))
                            .has())
                        .fop(call -> L(call.getParameters()).gat(argOrderInLambda))
                        .fop(toCast(PhpExpression.class))
                        .map(arg -> new FuncCtx(trace.getSearch()).findExprType(arg))
                        .fap(mt -> mt.types),
                    L(PsiTreeUtil.findChildrenOfType(file, ArrayCreationExpressionImpl.class))
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
                        .fop(arr -> Opt.fst(list(
                            new ArgRes(new FuncCtx(trace.getSearch()))
                                .getInlineFuncArg(arr, argOrderInLambda),
                            new ArgRes(new FuncCtx(trace.getSearch()))
                                .getFuncVarUsageArg(arr, argOrderInLambda)
                        )))
                        .fap(mt -> mt.types)
                ).fap(a -> a);
            })
            .map(types -> new MultiType(types));
    }

    // array_map(function($seg){return $seg['airline'];}, $segments);
    private Opt<MultiType> getInlineFuncArg(@Nullable PsiElement funcExpr, int argOrderInLambda)
    {
        return opt(funcExpr)
            .fop(toCast(PhpExpression.class))
            .fop(call -> getArgPassedTo(call, argOrderInLambda));
    }

    private MultiType peekOutside(ParameterImpl param)
    {
        return opt(param.getParent())
            .map(paramList -> paramList.getParent())
            .fop(toCast(FunctionImpl.class)) // closure
            .fop(clos -> getArgOrder(param)
                .fop(order -> Opt.fst(list(opt(null)
                    , getInlineFuncArg(clos.getParent(), order)
                    , getFuncVarUsageArg(clos.getParent(), order)
                    , getPrivateFuncUsageArg(clos, order)
                ))))
            .def(MultiType.INVALID_PSI)
            ;
    }

    private MultiType resolveFromDataProviderDoc(ParameterImpl param)
    {
        return opt(param.getDocComment())
            .fop(toCast(PhpDocCommentImpl.class))
            .map(doc -> doc.getDocTagByClass(PhpDocDataProviderImpl.class))
            .fop(tags -> L(tags).fst())
            .fop(tag -> L(tag.getChildren())
                .fop(toCast(PhpDocRefImpl.class))
                .fst())
            .fop(ref -> L(ref.getReferences()).fst())
            .map(ref -> ref.resolve())
            .fop(toCast(MethodImpl.class))
            .map(met -> ClosRes.getReturnedValue(met, new FuncCtx(trace.getSearch())).types)
            .map(ts -> new MultiType(ts).getEl())
            .fop(mt -> getArgOrder(param)
                .map(order -> mt.getKey(order + "")))
            .def(MultiType.INVALID_PSI);
    }

    public MultiType resolveArg(ParameterImpl param)
    {
        MultiType result = new MultiType(L());
        int order = getArgOrder(param).def(-1);

        opt(param.getParent())
            // get doc comments from the initial abstract method if any
            .fop(Tls.toCast(ParameterListImpl.class))
            .map(lst -> lst.getParent())
            .fop(Tls.toCast(MethodImpl.class))
            .fop(meth -> opt(meth.getContainingClass())
                .map(cls -> L(cls.getImplementedInterfaces())
                    .fap(ifc -> L(ifc.getMethods()))
                    .flt(ifcMeth -> meth.getName().equals(ifcMeth.getName()))))
            .def(list())
            .fop(meth -> L(meth.getParameters()).gat(order))
            .fop(Tls.toCast(ParameterImpl.class))
            // get doc from current method
            .cct(list(param))
            .fop(arg -> opt(arg.getDocComment()))
            .fop(doc -> opt(doc.getParamTagByName(param.getName())))
            .fop(doc -> new DocParamRes(trace).resolve(doc))
            .fch(mt -> result.types.addAll(mt.types))
            ;
        result.types.addAll(resolveFromDataProviderDoc(param).types);
        if (!trace.hasArgs()) {
            // passed args not known - if caret was inside this function
            result.types.addAll(peekOutside(param).types);
        } else {
            getArgOrder(param)
                .fop(i -> trace.getArg(i))
                .thn(mt -> result.types.addAll(mt.types));
        }
        return result;
    }
}
