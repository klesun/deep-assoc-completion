package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionArgumentsIndex;
import org.klesun.deep_assoc_completion.built_in_typedefs.ArgTypeDefs;
import org.klesun.deep_assoc_completion.completion_providers.StrValsPvdr;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.*;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.lang.Lang.*;

/**
 * takes associative array that caret points at and returns
 * all key names that will be accessed on this array later
 */
public class UsageResolver
{
    final private IExprCtx fakeCtx;
    final private int depthLeft;

    public UsageResolver(IExprCtx fakeCtx, int depthLeft)
    {
        this.fakeCtx = fakeCtx;
        this.depthLeft = depthLeft;
    }

    public UsageResolver(IExprCtx fakeCtx)
    {
        this(fakeCtx, fakeCtx.getSearch().project.map(proj ->
            DeepSettings.inst(proj).usageBasedCompletionDepthLimit).def(3));
    }

    private It<DeepType> resolveReplaceKeys(ParameterList argList, int order)
    {
        return It(argList.getParameters())
            .flt((psi, i) -> i < order)
            .fop(toCast(PhpExpression.class))
            .fap(exp -> fakeCtx.findExprType(exp));
    }

    private It<DeepType> resolveObjMethNames(PsiElement objPsi)
    {
        It<DeepType> objTit = Tls.cast(PhpExpression.class, objPsi)
            .fap(expr -> fakeCtx.findExprType(expr));
        Mt mt = new Mt(objTit);
        It<PhpClass> clses = ArrCtorRes.resolveMtCls(mt, objPsi.getProject());
        return clses.fap(cls -> cls.getMethods())
            .map(m -> new DeepType(m, PhpType.STRING, m.getName()));
    }

    private static It<ArrayIndex> findUsedIndexes(Function meth, String varName)
    {
        return opt(meth.getLastChild())
            .fap(chd -> Tls.findChildren(
                chd,
                ArrayAccessExpressionImpl.class,
                subPsi -> !(subPsi instanceof FunctionImpl)
            ))
            .fop(acc -> opt(acc.getValue())
                .fop(toCast(VariableImpl.class))
                .flt(varUsage -> varName.equals(varUsage.getName()))
                .map(varUsage -> acc.getIndex()));
    }

    public It<DeepType> findArgTypeFromUsage(Function meth, int argOrder, IExprCtx nextCtx)
    {
        return L(meth.getParameters()).gat(argOrder)
            .fop(toCast(ParameterImpl.class))
            .fap(arg -> It.cnc(
                findUsedIndexes(meth, arg.getName())
                    .map(idx -> idx.getValue())
                    .cst(PhpExpression.class)
                    .fap(lit -> nextCtx.limitResolveDepth(15, lit)
                        .unq(t -> t.stringValue)
                        .fap(t -> opt(t.stringValue)
                            .map(name -> {
                                DeepType assoct = new DeepType(arg, PhpType.ARRAY);
                                S<Mt> getType = () -> Tls.findParent(lit, ArrayAccessExpression.class, a -> true)
                                    .fap(acc -> new UsageResolver(nextCtx, depthLeft - 1).findExprTypeFromUsage(acc)).wap(Mt::new);
                                assoct.addKey(name, t.definition)
                                    .addType(getType, PhpType.UNSET);
                                return assoct;
                            }))
                    ),
                opt(arg.getDocComment())
                    .map(doc -> doc.getParamTagByName(arg.getName()))
                    .fap(doc -> new DocParamRes(nextCtx).resolve(doc)),
                opt(arg.getDefaultValue())
                    .cst(PhpExpression.class)
                    .fap(xpr -> nextCtx.subCtxEmpty().findExprType(xpr)),
                new UsageResolver(nextCtx, depthLeft - 1).findVarTypeFromUsage(arg)
            ));
    }

    // not sure this method belongs here... and name should be changed
    public It<DeepType> resolveArgCallArrKeys(Function meth, int funcVarArgOrder, int caretArgOrder)
    {
        return L(meth.getParameters()).gat(funcVarArgOrder)
            .fop(toCast(ParameterImpl.class))
            .fap(arg -> findVarReferences(arg))
            .fop(var -> opt(var.getParent()))
            // TODO: include not just direct calls,
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
                    .fop(call -> It(call.multiResolve(false))
                        .fap(res -> opt(res.getElement())).fst()),
                () -> Tls.cast(NewExpressionImpl.class, par)
                    .map(newEx -> newEx.getClassReference())
                    .map(ref -> ref.resolve())
            )  .fop(toCast(Function.class)));
    }

    private static It<Variable> findVarReferences(PhpNamedElement caretVar)
    {
        return Tls.findParent(caretVar, Function.class, a -> true)
            .fap(meth -> opt(meth.getLastChild()))
            .fap(chd -> FuncCallRes.findUsedVars(chd))
            .flt(varUsage -> caretVar.getName().equals(varUsage.getName()));
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
                .flt(m -> m.isAbstract())
                .fap(m -> MethCallRes.findOverridingMethods(m)).map(a -> a),
            list(meth)
        );
    }

    private It<DeepType> findKeysUsedInArrayMap(Function meth, ParameterList argList, int caretArgOrder)
    {
        return opt(meth.getName())
            .flt(n -> n.equals("array_map"))
            .map(n -> L(argList.getParameters()))
            .flt(args -> caretArgOrder == 1)
            .fop(args -> args.gat(0))
            .fop(func -> Tls.cast(PhpExpressionImpl.class, func)
                .map(expr -> expr.getFirstChild())
                .fop(toCast(Function.class))) // TODO: support in a var
            .fap(func -> {
                DeepType arrt = new DeepType(argList, PhpType.ARRAY);
                DeepType.Key k = arrt.addKey(KeyType.unknown(argList));
                L(argList.getParameters()).gat(caretArgOrder)
                    .cst(PhpExpression.class)
                    .thn(arrCtor -> k.addType(Tls.onDemand(() -> {
                        IExprCtx subCtx = fakeCtx.subCtxSingleArgArr(arrCtor);
                        return findArgTypeFromUsage(func, 0, subCtx).wap(Mt::new);
                    })));
                return list(arrt);
            });
    }

    private It<DeepType> findKeysUsedInPdoExec(Method meth, ParameterList argList, int caretArgOrder)
    {
        return som(meth)
            .flt(m -> "\\PDOStatement".equals(opt(m.getContainingClass()).map(cls -> cls.getFQN()).def("")))
            .flt(m -> "execute".equals(m.getName()))
            .flt(m -> caretArgOrder == 0)
            .fop(m -> opt(argList.getParent()))
            .fop(toCast(MethodReference.class))
            .fop(methRef -> opt(methRef.getClassReference()))
            .fap(clsRef -> fakeCtx.findExprType(clsRef))
            .map(pdostt -> makeAssoc(pdostt.definition, It(pdostt.pdoBindVars)
                .map(varName -> T2(varName, pdostt.definition))));
    }

    private It<DeepType> findKeysUsedInModelGet(Method func, ParameterList argList, int caretArgOrder)
    {
        return som(func)
            .flt(m -> caretArgOrder == 0)
            .fap(meth -> opt(argList.getParent())
                .cst(MethodReference.class)
                .fap(methCall -> (new MethCallRes(fakeCtx)).getModelRowType(methCall, meth)));
    }

    private It<DeepType> findBuiltInArgType(Function builtInFunc, int argOrder, ParameterList argList)
    {
        return Tls.cast(Method.class, builtInFunc)
            .uni(meth -> It.cnc(
                findKeysUsedInPdoExec(meth, argList, argOrder),
                findKeysUsedInModelGet(meth, argList, argOrder)
            ), () -> It.cnc(
                opt(builtInFunc.getName())
                    .flt(n -> n.equals("array_merge") || n.equals("array_replace"))
                    .fap(n -> resolveReplaceKeys(argList, argOrder)),
                opt(builtInFunc.getName())
                    .flt(n -> n.equals("method_exists"))
                    .flt(n -> argOrder == 1)
                    .fap(n -> resolveObjMethNames(argList.getParameters()[0])),
                new ArgTypeDefs(fakeCtx.subCtxEmpty()).getArgType(builtInFunc, argOrder),
                findKeysUsedInArrayMap(builtInFunc, argList, argOrder)
            ));
    }

    private static It<DeepType> findFqnMetaArgType(String fqn, int argOrder, IExprCtx ctx)
    {
        return MethCallRes.findFqnMetaType(fqn, ctx,
            PhpExpectedFunctionArgumentsIndex.KEY,
            arg -> arg.getArgumentIndex() == argOrder
        );
    }

    public static It<DeepType> findMetaArgType(Function func, int argOrder, IExprCtx ctx)
    {
        String fqn = func.getFQN();
        return findFqnMetaArgType(fqn, argOrder, ctx);
    }

    private It<DeepType> findCallableMetaArgType(Function func, int argOrder)
    {
        return Tls.cast(Method.class, func).uni(
            meth -> MethCallRes.findOverriddenMethods(meth).fap(m -> findMetaArgType(m, argOrder, fakeCtx)),
            () -> findMetaArgType(func, argOrder, fakeCtx)
        );
    }

    private void putToCache(PhpExpression expr, MemIt<DeepType> result)
    {
        fakeCtx.getSearch().exprToUsageResult.remove(expr);
        fakeCtx.getSearch().exprToUsageResult.put(expr, result);
    }

    private Opt<MemIt<DeepType>> takeFromCache(PhpExpression expr)
    {
        return opt(fakeCtx.getSearch().exprToUsageResult.get(expr));
    }

    private static It<String> makeMethFqns(PhpExpression clsRef, String name)
    {
        return It(ArrCtorRes.ideaTypeToFqn(clsRef.getType()))
            .map(clsFqn -> clsFqn + "." + name);
    }

    public static It<String> getCallFqn(PsiElement call)
    {
        return It.cnc(
            Tls.cast(MethodReference.class, call)
                .fap(casted -> opt(casted.getName())
                    .fap(name -> opt(casted.getClassReference())
                        .fap(clsRef -> makeMethFqns(clsRef, name)))),
            Tls.cast(NewExpression.class, call)
                .fap(casted -> opt(casted.getClassReference())
                    .fap(clsRef -> makeMethFqns(clsRef, "__construct"))),
            Tls.cast(FunctionReferenceImpl.class, call)
                .fop(casted -> opt(casted.getFQN()))
        );
    }

    // if arg is assoc array - will return type with keys accessed on it
    // if arg is string - will return type of values it can take
    public It<DeepType> findExprTypeFromUsage(PhpExpression caretExpr)
    {
        Opt<MemIt<DeepType>> fromCache = takeFromCache(caretExpr);
        if (fromCache.has()) {
            return fromCache.unw().itr();
        }
        putToCache(caretExpr, new MemIt<>(It.non()));

        // assoc array in an assoc array
        It<DeepType> asAssocKey = opt(caretExpr.getParent())
            .fop(toCast(PhpPsiElementImpl.class))
            .fap(val -> resolveOuterArray(val).types);

        It<DeepType> asPlusArr = opt(caretExpr.getParent())
            .fop(toCast(BinaryExpression.class))
            .flt(bin -> opt(bin.getOperation()).any(op -> op.getText().equals("+")))
            .flt(sum -> caretExpr.isEquivalentTo(sum.getRightOperand()))
            .map(sum -> sum.getLeftOperand())
            .fop(toCast(PhpExpression.class))
            .fap(exp -> fakeCtx.findExprType(exp));

        Opt<DeepType> asEqStrVal = StrValsPvdr.assertEqOperand(caretExpr)
            .cst(StringLiteralExpression.class)
            .map(lit -> new DeepType(lit, PhpType.STRING, lit.getContents()));

        It<DeepType> asFuncArg = opt(caretExpr.getParent())
            .fop(toCast(ParameterList.class))
            .fap(argList -> {
                int order = L(argList.getParameters()).indexOf(caretExpr);
                Opt<PsiElement> callOpt = opt(argList.getParent());
                It<DeepType> asRealFuncArg = resolveFunc(argList)
                    .fap(meth -> It.cnc(
                        getImplementations(meth).fap(ipl -> {
                            IExprCtx nextCtx = callOpt.fop(call -> Opt.fst(
                                () -> Tls.cast(MethodReference.class, call).map(casted -> fakeCtx.subCtxDirect(casted)),
                                () -> Tls.cast(NewExpression.class, call).map(casted -> fakeCtx.subCtxDirect(casted))
                            )).def(fakeCtx.subCtxEmpty());
                            return findArgTypeFromUsage(ipl, order, nextCtx);
                        }),
                        findBuiltInArgType(meth, order, argList),
                        findCallableMetaArgType(meth, order) // for meta info on methods in parent classes
                    ));

                It<DeepType> asMagicCtorArg = opt(argList.getParent())
                    .fop(toCast(NewExpressionImpl.class))
                    .fap(newEx -> findClsMagicCtorUsedKeys(newEx, order).types);

                // for meta info on methods of this particular class, including magic and inherited ones
                It<DeepType> asFqnMeta = callOpt
                    .fap(psi -> getCallFqn(psi))
                    .fap(fqn -> findFqnMetaArgType(fqn, order, fakeCtx));

                return It.cnc(asFqnMeta, asRealFuncArg, asMagicCtorArg);
            });

        MemIt<DeepType> result = It.cnc(asAssocKey, asEqStrVal, asPlusArr, asFuncArg).mem();
        putToCache(caretExpr, result);
        return result.itr();
    }

    private It<DeepType> findVarTypeFromUsage(PhpNamedElement caretVar)
    {
        if (depthLeft < 1) {
            return It.non();
        }
        return findVarReferences(caretVar)
            .flt(ref -> ref.getTextOffset() > caretVar.getTextOffset())
            .fop(toCast(Variable.class))
            .fap(refVar -> It.cnc(
                findExprTypeFromUsage(refVar),
                // $this->$magicProp
                opt(refVar.getParent())
                    .cst(FieldReference.class)
                    .flt(fld -> !caretVar.equals(fld.getClassReference()))
                    .fap(fld -> opt(fld.getClassReference()))
                    .fap(fld -> fakeCtx.findExprType(fld))
                    // TODO: add declared field names here too
                    .fap(objt -> objt.props.vls())
                    .fap(prop -> prop.keyType.getTypes()),
                // $this->props[$varName]
                opt(refVar.getParent())
                    .cst(ArrayIndex.class)
                    .fap(idx -> opt(idx.getParent()))
                    .cst(ArrayAccessExpression.class)
                    .fap(acc -> opt(acc.getValue()))
                    .cst(PhpExpression.class)
                    .fap(value -> fakeCtx.findExprType(value))
                    .fap(objt -> objt.keys)
                    .fap(prop -> prop.keyType.getTypes())
            ));
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
                        .arr().indexOf(val);
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
        SearchCtx fakeSearch = new SearchCtx(arrCtor.getProject());
        FuncCtx funcCtx = new FuncCtx(fakeSearch);
        IExprCtx fakeCtx = new ExprCtx(funcCtx, arrCtor, 0);

        return It.cnc(
            findExprTypeFromUsage(arrCtor),
            opt(arrCtor.getParent())
                .fop(toCast(AssignmentExpression.class))
                .flt(ass -> arrCtor.isEquivalentTo(ass.getValue()))
                .map(ass -> ass.getVariable())
                .fop(toCast(Variable.class))
                .fap(var -> It.cnc(
                    new VarRes(fakeCtx).getDocType(var),
                    findVarTypeFromUsage(var)
                ))
        ).wap(Mt::new);
    }
}
