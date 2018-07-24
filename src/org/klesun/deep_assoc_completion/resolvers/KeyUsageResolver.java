package org.klesun.deep_assoc_completion.resolvers;

import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.HashSet;
import java.util.Set;

/**
 * takes associative array that caret points at and returns
 * all key names that will be accessed on this array later
 */
public class KeyUsageResolver extends Lang
{
    final private FuncCtx fakeCtx;
    final private int depthLeft;

    public KeyUsageResolver(FuncCtx fakeCtx, int depthLeft)
    {
        this.fakeCtx = fakeCtx;
        this.depthLeft = depthLeft;
    }

    private static L<String> resolveReplaceKeys(ParameterList argList, int order)
    {
        SearchContext search = new SearchContext(argList.getProject());
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

    public L<String> resolveArgUsedKeys(Function meht, int argOrder)
    {
        return L(meht.getParameters()).gat(argOrder)
            .fop(toCast(ParameterImpl.class))
            .map(arg -> list(
                findUsedIndexes(meht, arg.getName())
                    .map(idx -> idx.getValue())
                    .fop(toCast(StringLiteralExpressionImpl.class))
                    .map(lit -> lit.getContents()),
                opt(arg.getDocComment())
                    .map(doc -> doc.getParamTagByName(arg.getName()))
                    .fop(doc -> new DocParamRes(fakeCtx).resolve(doc))
                    .map(mt -> mt.getKeyNames())
                    .def(L()),
                new KeyUsageResolver(fakeCtx, depthLeft - 1).findKeysUsedOnVar(arg)
            ).fap(a -> a))
            .def(L());
    }

    // not sure this method belongs here... and name should be changed
    public MultiType resolveArgCallArrKeys(Function meth, int funcVarArgOrder, int caretArgOrder)
    {
        return L(meth.getParameters()).gat(funcVarArgOrder)
            .fop(toCast(ParameterImpl.class))
            .fap(arg -> findVarReferences(arg))
            .fop(var -> opt(var.getParent()))
            // TODO: include not just dirrect calls,
            // but also array_map and other built-ins
            .fop(toCast(FunctionReference.class))
            .fop(call -> L(call.getParameters()).gat(caretArgOrder))
            .fop(toCast(PhpExpression.class))
            .fap(exp -> fakeCtx.findExprType(exp).types)
            .wap(MultiType::new);
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

    private static L<Variable> findVarReferences(PhpNamedElement caretVar)
    {
        return Tls.findParent(caretVar, Function.class, a -> true)
            .fap(meth -> Tls.findChildren(
                meth.getLastChild(),
                Variable.class,
                subPsi -> !(subPsi instanceof Function)
            ).flt(varUsage -> caretVar.getName().equals(varUsage.getName())));
    }

    // add completion from new SomeClass() that depends
    // on class itself, not on the constructor function
    private L<String> findClsMagicCtorUsedKeys(NewExpressionImpl newEx, int order)
    {
        return opt(newEx.getClassReference())
            .map(ref -> ref.resolve())
            .fop(clsPsi -> Opt.fst(list(
                Tls.cast(Method.class, clsPsi).map(m -> m.getContainingClass()), // class has own constructor
                Tls.cast(PhpClass.class, clsPsi) // class does not have an own constructor
            )))
            // Laravel's Model takes array of initial column values in constructor
            .flt(cls -> order == 0)
            .fap(cls -> {
                L<PhpClass> supers = L(cls.getSupers());
                boolean isModel = supers.any(sup -> sup.getFQN()
                    .equals("\\Illuminate\\Database\\Eloquent\\Model"));
                if (!isModel) {
                    return list();
                } else {
                    Set<String> inherited = new HashSet<>(supers.fap(s -> L(s.getOwnFields()).map(f -> f.getName())));
                    return L(cls.getOwnFields())
                        .flt(fld -> !fld.getModifier().isPrivate())
                        .flt(fld -> !inherited.contains(fld.getName()))
                        .map(fld -> fld.getName());
                }
            });
    }

    private static L<? extends Function> getImplementations(Function meth)
    {
        L<Function> result = Tls.cast(Method.class, meth)
            .fap(m -> MethCallRes.findOverridingMethods(m)).map(a -> a);
        result.add(meth);
        return result;
    }

    private L<String> findKeysUsedOnExpr(PhpExpression arrCtor)
    {
        return opt(arrCtor.getParent())
            .fop(toCast(ParameterList.class))
            .fap(argList -> {
                int order = L(argList.getParameters()).indexOf(arrCtor);
                return resolveFunc(argList)
                    .fap(meth -> list(
                        getImplementations(meth)
                            .fap(ipl -> resolveArgUsedKeys(ipl, order)),
                        opt(meth.getName())
                            .flt(n -> n.equals("array_merge") || n.equals("array_replace"))
                            .fap(n -> resolveReplaceKeys(argList, order))
                    ).fap(a -> a))
                    .cct(opt(argList.getParent())
                        .fop(toCast(NewExpressionImpl.class))
                        .fap(newEx -> findClsMagicCtorUsedKeys(newEx, order)));
            });
    }

    private L<String> findKeysUsedOnVar(PhpNamedElement caretVar)
    {
        if (depthLeft < 1) {
            return list();
        }
        return findVarReferences(caretVar)
            .flt(ref -> ref.getTextOffset() > caretVar.getTextOffset())
            .fop(toCast(Variable.class))
            .fap(refVar -> findKeysUsedOnExpr(refVar));
    }

    public L<String> resolve(ArrayCreationExpression arrCtor)
    {
        SearchContext fakeSearch = new SearchContext(arrCtor.getProject());
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
                    new VarRes(fakeCtx).getDocType(var).getKeyNames(),
                    findKeysUsedOnVar(var)
                ).fap(a -> a))
        ).fap(a -> a);
    }
}
