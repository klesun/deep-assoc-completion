package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.KeyType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.*;

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

    private Mt resolveReplaceKeys(ParameterList argList, int order)
    {
        return It(argList.getParameters())
            .flt((psi, i) -> i < order)
            .fop(toCast(PhpExpression.class))
            .fap(exp -> fakeCtx.findExprType(exp))
            .wap(Mt::new);
    }

    private static It<ArrayIndex> findUsedIndexes(Function meth, String varName)
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

    public It<DeepType> resolveArgUsedKeys(Function meth, int argOrder, FuncCtx nextCtx)
    {
        return L(meth.getParameters()).gat(argOrder)
            .fop(toCast(ParameterImpl.class))
            .fap(arg -> It.cnc(
                findUsedIndexes(meth, arg.getName())
                    .map(idx -> idx.getValue())
                    .fop(toCast(StringLiteralExpressionImpl.class))
                    .wap(lits -> {
                        DeepType assoct = new DeepType(arg, PhpType.ARRAY);
                        lits.fch(lit -> {
                            S<Mt> getType = () -> Tls.findParent(lit, ArrayAccessExpression.class, a -> true)
                                .fap(acc -> new KeyUsageResolver(nextCtx, depthLeft - 1).findKeysUsedOnExpr(acc)).wap(Mt::new);
                            assoct.addKey(lit.getContents(), lit)
                                .addType(getType, PhpType.UNSET);
                        });
                        return list(assoct);
                    }),
                opt(arg.getDocComment())
                    .map(doc -> doc.getParamTagByName(arg.getName()))
                    .fap(doc -> new DocParamRes(nextCtx).resolve(doc)),
                new KeyUsageResolver(nextCtx, depthLeft - 1).findKeysUsedOnVar(arg)
            ));
    }

    // not sure this method belongs here... and name should be changed
    public It<DeepType> resolveArgCallArrKeys(Function meth, int funcVarArgOrder, int caretArgOrder)
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
            .fap(exp -> fakeCtx.findExprType(exp));
    }

    private static Opt<Function> resolveFunc(ParameterList argList)
    {
        return opt(argList.getParent())
            .fop(par -> Opt.fst(
                () -> Tls.cast(FunctionReference.class, par)
                    .map(call -> call.resolve()),
                () -> Tls.cast(MethodReferenceImpl.class, par)
                    .map(call -> call.resolve()),
                () -> Tls.cast(NewExpressionImpl.class, par)
                    .map(newEx -> newEx.getClassReference())
                    .map(ref -> ref.resolve())
            )  .fop(toCast(Function.class)));
    }

    private static It<Variable> findVarReferences(PhpNamedElement caretVar)
    {
        return Tls.findParent(caretVar, Function.class, a -> true)
            .fap(meth -> Tls.findChildren(
                meth.getLastChild(),
                Variable.class,
                subPsi -> !(subPsi instanceof Function)
            ).flt(varUsage -> caretVar.getName().equals(varUsage.getName())));
    }

    public static DeepType makeAssoc(PsiElement psi, Iterable<T2<String, PsiElement>> keys)
    {
        DeepType assoct = new DeepType(psi, PhpType.ARRAY);
        for (T2<String, PsiElement> key: keys) {
            assoct.addKey(key.a, key.b);
        }
        return assoct;
    }

    // add completion from new SomeClass() that depends
    // on class itself, not on the constructor function
    private Mt findClsMagicCtorUsedKeys(NewExpressionImpl newEx, int order)
    {
        return opt(newEx.getClassReference())
            .map(ref -> ref.resolve())
            .fop(clsPsi -> Opt.fst(
                () -> Tls.cast(Method.class, clsPsi).map(m -> m.getContainingClass()), // class has own constructor
                () -> Tls.cast(PhpClass.class, clsPsi) // class does not have an own constructor
            ))
            // Laravel's Model takes array of initial column values in constructor
            .flt(cls -> order == 0)
            .fap(cls -> {
                L<PhpClass> supers = L(cls.getSupers());
                boolean isModel = supers.any(sup -> sup.getFQN()
                    .equals("\\Illuminate\\Database\\Eloquent\\Model"));
                if (!isModel) {
                    return list();
                } else {
                    Set<String> inherited = new HashSet<>(supers.fap(s -> It(s.getOwnFields()).map(f -> f.getName())).arr());
                    return makeAssoc(newEx, It(cls.getOwnFields())
                        .flt(fld -> !fld.getModifier().isPrivate())
                        .flt(fld -> !inherited.contains(fld.getName()))
                        .map(fld -> T2(fld.getName(), fld))).mt().types;
                }
            })
            .wap(Mt::new);
    }

    private static It<? extends Function> getImplementations(Function meth)
    {
        return It.cnc(
            Tls.cast(Method.class, meth)
                .fap(m -> MethCallRes.findOverridingMethods(m)).map(a -> a),
            list(meth)
        );
    }

    private It<DeepType> findKeysUsedInArrayMap(Function meth, ParameterList argList, PhpExpression arrCtor)
    {
        return opt(meth.getName())
            .flt(n -> n.equals("array_map"))
            .map(n -> L(argList.getParameters()))
            .flt(args -> args.indexOf(arrCtor) == 1)
            .fop(args -> args.gat(0))
            .fop(func -> Tls.cast(PhpExpressionImpl.class, func)
                .map(expr -> expr.getFirstChild())
                .fop(toCast(Function.class))) // TODO: support in a var
            .fap(func -> {
                DeepType arrt = new DeepType(argList, PhpType.ARRAY);
                arrt.addKey(KeyType.unknown(argList)).addType(Tls.onDemand(() -> resolveArgUsedKeys(func, 0, fakeCtx.subCtxSingleArgArr(arrCtor)).wap(Mt::new)));
                return list(arrt);
            });
    }

    private It<DeepType> findKeysUsedInPdoExec(Function meth, ParameterList argList, PhpExpression arrCtor)
    {
        return Tls.cast(Method.class, meth)
            .flt(m -> "\\PDOStatement".equals(opt(m.getContainingClass()).map(cls -> cls.getFQN()).def("")))
            .flt(m -> "execute".equals(m.getName()))
            .flt(m -> L(argList.getParameters()).indexOf(arrCtor) == 0)
            .fop(m -> opt(argList.getParent()))
            .fop(toCast(MethodReference.class))
            .fop(methRef -> opt(methRef.getClassReference()))
            .fap(clsRef -> fakeCtx.findExprType(clsRef))
            .map(pdostt -> makeAssoc(pdostt.definition, It(pdostt.pdoBindVars)
                .map(varName -> T2(varName, pdostt.definition))));
    }

    private It<DeepType> findKeysUsedOnExpr(PhpExpression arrCtor)
    {
        return opt(arrCtor.getParent())
            .fop(toCast(ParameterList.class))
            .fap(argList -> {
                int order = L(argList.getParameters()).indexOf(arrCtor);
                Opt<PsiElement> callOpt = opt(argList.getParent());
                return resolveFunc(argList)
                    .fap(meth -> It.cnc(
                        getImplementations(meth).fap(ipl -> {
                            FuncCtx nextCtx = callOpt.fop(call -> Opt.fst(
                                () -> Tls.cast(MethodReference.class, call).map(casted -> fakeCtx.subCtxDirect(casted)),
                                () -> Tls.cast(NewExpression.class, call).map(casted -> fakeCtx.subCtxDirect(casted))
                            )).def(new FuncCtx(fakeCtx.getSearch()));
                            return resolveArgUsedKeys(ipl, order, nextCtx);
                        }),
                        opt(meth.getName())
                            .flt(n -> n.equals("array_merge") || n.equals("array_replace"))
                            .fap(n -> resolveReplaceKeys(argList, order).types),
                        findKeysUsedInArrayMap(meth, argList, arrCtor),
                        findKeysUsedInPdoExec(meth, argList, arrCtor),
                        opt(argList.getParent())
                            .fop(toCast(NewExpressionImpl.class))
                            .fap(newEx -> findClsMagicCtorUsedKeys(newEx, order).types)
                    ));
            });
    }

    private It<DeepType> findKeysUsedOnVar(PhpNamedElement caretVar)
    {
        if (depthLeft < 1) {
            return It.non();
        }
        return findVarReferences(caretVar)
            .flt(ref -> ref.getTextOffset() > caretVar.getTextOffset())
            .fop(toCast(Variable.class))
            .fap(refVar -> findKeysUsedOnExpr(refVar));
    }

    private Mt resolveOuterArray(PhpPsiElementImpl val) {
        return opt(val.getParent())
            .fop(par -> {
                Opt<String> key;
                if (par instanceof ArrayHashElement) {
                    key = opt(((ArrayHashElement) par).getKey())
                        .fop(toCast(StringLiteralExpression.class))
                        .map(lit -> lit.getContents());
                    par = par.getParent();
                } else {
                    int order = It(par.getChildren())
                        .fop(toCast(PhpPsiElementImpl.class))
                        .arr().indexOf(par);
                    key = order > -1 ? opt(order + "") : opt(null);
                }
                Opt<String> keyf = key;
                return opt(par)
                    .fop(toCast(ArrayCreationExpression.class))
                    .map(outerArr -> resolve(outerArr))
                    .map(outerMt -> outerMt.getKey(keyf.def(null)));
            })
            .def(Mt.INVALID_PSI);
    }

    public Mt resolve(ArrayCreationExpression arrCtor)
    {
        SearchContext fakeSearch = new SearchContext(arrCtor.getProject());
        FuncCtx fakeCtx = new FuncCtx(fakeSearch);

        return list(
            findKeysUsedOnExpr(arrCtor),
            opt(arrCtor.getParent())
                .fop(toCast(BinaryExpression.class))
                .flt(sum -> arrCtor.isEquivalentTo(sum.getRightOperand()))
                .map(sum -> sum.getLeftOperand())
                .fop(toCast(PhpExpression.class))
                .fap(exp -> fakeCtx.findExprType(exp)),
            opt(arrCtor.getParent())
                .fop(toCast(AssignmentExpression.class))
                .flt(ass -> arrCtor.isEquivalentTo(ass.getValue()))
                .map(ass -> ass.getVariable())
                .fop(toCast(Variable.class))
                .fap(var -> It.cnc(
                    new VarRes(fakeCtx).getDocType(var),
                    findKeysUsedOnVar(var)
                )),
            // assoc array in an assoc array
            opt(arrCtor.getParent())
                .fop(toCast(PhpPsiElementImpl.class))
                .fap(val -> resolveOuterArray(val).types)
        ).fap(a -> a).wap(Mt::new);
    }
}
