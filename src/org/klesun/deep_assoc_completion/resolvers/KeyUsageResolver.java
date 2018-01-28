package org.klesun.deep_assoc_completion.resolvers;

import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

/**
 * takes associative array that caret points at and returns
 * all key names that will be accessed on this array later
 */
public class KeyUsageResolver extends Lang
{
    final private FuncCtx fakeCtx;

    public KeyUsageResolver(FuncCtx fakeCtx)
    {
        this.fakeCtx = fakeCtx;
    }

    private static L<String> resolveReplaceKeys(ParameterList argList, int order)
    {
        SearchContext search = new SearchContext();
        FuncCtx ctx = new FuncCtx(search);
        return L(argList.getParameters())
            .flt((psi, i) -> i < order)
            .fop(toCast(PhpExpression.class))
            .fap(exp -> ctx.findExprType(exp).getKeyNames());

    }

    private static L<ArrayIndex> findUsedIndexes(Function meth, String varName)
    {
        return Tls.findChildren(
            meth.getLastChild(),
            ArrayAccessExpressionImpl.class,
            subPsi -> !(subPsi instanceof FunctionImpl)
        )
            .fop(acc -> opt(acc.getValue())
                .fop(toCast(VariableImpl.class))
                .flt(varUsage -> varName.equals(varUsage.getName()))
                .map(varUsage -> acc.getIndex()));
    }

    private L<String> resolveArgUsedKeys(Function meth, int argOrder)
    {
        return L(meth.getParameters()).gat(argOrder)
            .fop(toCast(ParameterImpl.class))
            .map(arg -> list(
                findUsedIndexes(meth, arg.getName())
                    .map(idx -> idx.getValue())
                    .fop(toCast(StringLiteralExpressionImpl.class))
                    .map(lit -> lit.getContents()),
                opt(arg.getDocComment())
                    .map(doc -> doc.getParamTagByName(arg.getName()))
                    .fop(doc -> new DocParamRes(fakeCtx).resolve(doc))
                    .map(mt -> mt.getKeyNames())
                    .def(L())
            ).fap(a -> a))
            .def(L());
    }

    private static Opt<Function> resolveFunc(ParameterList argList)
    {
        return opt(argList.getParent())
            .fop(par -> Opt.fst(list(
                Tls.cast(FunctionReference.class, par)
                    .map(call -> call.resolve()),
                Tls.cast(MethodReferenceImpl.class, par)
                    .map(call -> call.resolve()),
                Tls.cast(NewExpressionImpl.class, par)
                    .map(newEx -> newEx.getClassReference())
                    .map(ref -> ref.resolve())
            ))  .fop(toCast(Function.class)));
    }

    private static L<Variable> findVarReferences(Variable caretVar)
    {
        return Tls.findParent(caretVar, Function.class, a -> true)
            .fap(meth -> Tls.findChildren(
                meth.getLastChild(),
                Variable.class,
                subPsi -> !(subPsi instanceof Function)
            ).flt(varUsage -> caretVar.getName().equals(varUsage.getName())));
    }

    private L<String> findKeysUsedOnExpr(PhpExpression arrCtor)
    {
        return opt(arrCtor.getParent())
            .fop(toCast(ParameterList.class))
            .fap(argList -> {
                int order = L(argList.getParameters()).indexOf(arrCtor);
                return resolveFunc(argList)
                    .fap(meth -> list(
                        resolveArgUsedKeys(meth, order),
                        opt(meth.getName())
                            .flt(n -> n.equals("array_merge") || n.equals("array_replace"))
                            .fap(n -> resolveReplaceKeys(argList, order))
                    ).fap(a -> a));
            });
    }

    private L<String> findKeysUsedOnVar(Variable caretVar)
    {
        return findVarReferences(caretVar)
            .flt(ref -> ref.getTextOffset() > caretVar.getTextOffset())
            .fop(toCast(Variable.class))
            .fap(refVar -> findKeysUsedOnExpr(refVar));
    }

    public L<String> resolve(ArrayCreationExpression arrCtor)
    {
        SearchContext fakeSearch = new SearchContext();
        FuncCtx fakeCtx = new FuncCtx(fakeSearch);

        // TODO: handle nested arrays
        return list(
            findKeysUsedOnExpr(arrCtor),
            opt(arrCtor.getParent())
                .fop(toCast(BinaryExpression.class))
                .flt(sum -> arrCtor.isEquivalentTo(sum.getRightOperand()))
                .map(sum -> sum.getLeftOperand())
                .fop(toCast(PhpExpression.class))
                .fap(exp -> fakeCtx.findExprType(exp).getKeyNames()),
            opt(arrCtor.getParent())
                .fop(toCast(AssignmentExpression.class))
                .flt(ass -> arrCtor.isEquivalentTo(ass.getValue()))
                .map(ass -> ass.getVariable())
                .fop(toCast(Variable.class))
                .fap(var -> list(
                    fakeCtx.findExprType(var).getKeyNames(),
                    findKeysUsedOnVar(var)
                ).fap(a -> a))
        ).fap(a -> a);
    }
}
