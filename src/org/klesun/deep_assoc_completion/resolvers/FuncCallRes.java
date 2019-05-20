package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringEscapeUtils;
import org.klesun.deep_assoc_completion.built_in_typedefs.ReturnTypeDefs;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.ScopeFinder;
import org.klesun.deep_assoc_completion.structures.ArgOrder;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.deep_assoc_completion.structures.Mkt;
import org.klesun.lang.*;

public class FuncCallRes extends Lang
{
    private IExprCtx ctx;

    public FuncCallRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    public static It<Variable> findUsedVars(PsiElement meth)
    {
        return Tls.findChildren(
            meth, Variable.class,
            subPsi -> !(subPsi instanceof Function)
        );
    }

    private static It<Variable> findVarRefsInFunc(GroupStatement meth, String varName)
    {
        return findUsedVars(meth).flt(varUsage -> varName.equals(varUsage.getName()));
    }

    private static PhpType getDocType(Function func)
    {
        //return func.getDocType();
        return opt(func.getDocComment())
            .map(doc -> doc.getReturnTag())
            .map(ret -> ret.getType())
            .def(PhpType.EMPTY)
            ;
    }

    private It<DeepType> array_map(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        DeepType mapRetType = new DeepType(call);
        Mt arrMt = callCtx.getArgMt(1);
        IExprCtx subCtx = L(call.getParameters()).gat(1)
            .fop(array -> Tls.cast(PhpExpression.class, array))
            .uni(argArr -> ctx.subCtxSingleArgArr(argArr),
                () -> ctx.subCtxEmpty()
            );
        S<Mt> getElMt = Tls.onDemand(() -> callCtx.getArgMt(0).types
            .fap(t -> t.getReturnTypes(subCtx))
            .wap(Mt::new));
        It<DeepType> eachTMapped = arrMt.types.map(t -> {
            DeepType mapped = new DeepType(t.definition, PhpType.ARRAY);
            t.keys.fch((v, i) -> mapped.addKey(v.keyType, v.definition)
                .addType(getElMt, call.getType().elementType()));
            return mapped;
        });
        MemIt<DeepType.Key> srcKeys = arrMt.types.fap(t -> t.keys).mem();
        It<DeepType> ktg = srcKeys.fap(k -> k.keyType.getTypes());
        mapRetType.addKey(KeyType.mt(ktg, call), call).addType(getElMt, PhpType.MIXED);
        return It.cnc(list(mapRetType), eachTMapped);
    }

    private DeepType array_combine(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        DeepType combine = new DeepType(call, PhpType.ARRAY);
        S<Mt> getElMt = Tls.onDemand(() -> callCtx.getArgMt(1).getEl());
        PhpType ideaElType = L(call.getParameters()).gat(1)
            .fop(toCast(PhpExpression.class))
            .map(e -> e.getType()).def(PhpType.MIXED);
        combine.addKey(KeyType.mt(callCtx.getArgMt(0).getEl().types, call))
            .addType(getElMt, ideaElType);
        return combine;
    }

    private DeepType array_fill_keys(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        return makeAssoc(call, callCtx.getArg(0)
            .fap(mt -> mt.getEl().getStringValues())
            .map(keyName -> T2(keyName, PhpType.MIXED))
            .arr());
    }

    private DeepType array_flip(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        DeepType flip = new DeepType(call, PhpType.ARRAY);
        Mt sourceMt = callCtx.getArgMt(0);
        Mt newValueMt = sourceMt.getKeyNames()
            .map(name -> new DeepType(call, PhpType.STRING, name)).wap(Mt::new);
        L<String> newKeys = sourceMt.getEl().types.map(t -> t.stringValue).arr();
        if (!newKeys.has() || newKeys.any(k -> k == null)) {
            flip.addKey(KeyType.unknown(call), call).addType(() -> newValueMt, PhpType.MIXED);
        }
        for (String key: newKeys) {
            flip.addKey(key, call).addType(() -> newValueMt, It(newValueMt.types).map(t -> t.briefType).fst().def(PhpType.EMPTY));
        }
        return flip;
    }

    private DeepType compact(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        DeepType arrt = new DeepType(call, PhpType.ARRAY);
        Tls.findParent(call, GroupStatement.class, a -> true)
            .thn(scope -> Tls.range(0, call.getParameters().length)
                .fch(i -> callCtx.getArg(i)
                    .map(keyt -> keyt.getStringValue())
                    .thn(varName -> {
                        L<Variable> refs = findVarRefsInFunc(scope, varName)
                            .flt(ref -> ScopeFinder.didPossiblyHappen(ref, call)).arr()
                            ;
                        if (refs.size() > 0) {
                            PhpType briefType = new PhpType();
                            refs.map(var -> Tls.getIdeaType(var)).fch(briefType::add);
                            arrt.addKey(varName, call)
                                .addType(() -> refs
                                    .fap(ref -> ctx.findExprType(ref))
                                    .wap(Mt::new), briefType);
                        }
                    })));
        return arrt;
    }

    private DeepType implode(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        String delim = callCtx.getArgMt(0).getStringValues().fst().def(" ");
        It<String> parts = callCtx.getArgMt(1).types
            .fap(t -> t.keys)
            .fap(kv -> kv.getTypes())
            .fop(t -> opt(t.stringValue));
        String joined = Tls.implode(delim, parts);
        // PHP string is not java string, of course, but pretty close
        String unescaped = StringEscapeUtils.unescapeJava(joined);
        DeepType type = new DeepType(call, PhpType.STRING, unescaped);
        return type;
    }

    private DeepType get_object_vars(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        It<PhpClass> clses = callCtx.hasArgs()
            ? callCtx.getArg(0)
                .fap(mt -> ArrCtorRes.resolveMtInstCls(mt, call.getProject()))
            : Tls.findParent(call, PhpClass.class, a -> true).itr();
        DeepType type = new DeepType(call, PhpType.ARRAY);
        clses.fap(cls -> cls.getFields())
            .flt(fld -> !fld.getModifier().isStatic())
            .fch(fld -> type.addKey(fld.getName(), fld)
                .addType(() -> opt(fld.getDefaultValue())
                    .cst(PhpExpression.class)
                    .fap(expr -> ctx.subCtxEmpty().findExprType(expr))
                    .wap(ts -> new Mt(ts)), fld.getType()));
        return type;
    }

    public static DeepType makeAssoc(PsiElement psi, Iterable<T2<String, PhpType>> keys)
    {
        return Mkt.assoc(psi, It(keys)
            .map(t -> t.nme((keyName, ideaType) ->
                T2(keyName, new DeepType(psi, ideaType).mt()))));
    }

    private Iterable<DeepType> findBuiltInFuncCallType(FunctionReferenceImpl call)
    {
        return opt(call.getName()).map(name -> {
            IFuncCtx callCtx = ctx.subCtxDirect(call).func();
            PsiElement[] params = call.getParameters();
            L<PsiElement> lParams = L(params);
            if (name.equals("array_map")) {
                return array_map(callCtx, call);
            } else if (name.equals("array_filter") || name.equals("array_reverse")
                    || name.equals("array_splice") || name.equals("array_slice")
                    || name.equals("array_values") || name.equals("array_pad")
                    || name.equals("array_unique")
            ) {
                // array type unchanged
                return callCtx.getArgMt(0).types;
            } else if (name.equals("array_combine")) {
                return list(array_combine(callCtx, call));
            } else if (name.equals("array_fill_keys")) {
                return list(array_fill_keys(callCtx, call));
            } else if (name.equals("array_flip")) {
                return list(array_flip(callCtx, call));
            } else if (name.equals("array_pop") || name.equals("array_shift")
                    || name.equals("current") || name.equals("end") || name.equals("next")
                    || name.equals("prev") || name.equals("reset")
            ) {
                return callCtx.getArgMt(0).getEl().types;
            } else if (name.equals("array_merge") || name.equals("array_replace")
                    || name.equals("array_replace_recursive")
            ) {
                return Tls.range(0, params.length)
                    .fop(i -> callCtx.getArg(i))
                    .fap(mt -> mt.types).map(a -> a);
            } else if (name.equals("array_column")) {
                DeepType type = new DeepType(call);
                type.addKey(KeyType.integer(call)).addType(() -> {
                    Mt elType = callCtx.getArgMt(0).getEl();
                    String keyName = callCtx.getArgMt(1).getStringValue();
                    It<DeepType.Key> allProps = FieldRes.getPublicProps(
                        elType, call.getProject(), ctx.subCtxEmpty()
                    );
                    return new Mt(It.cnc(
                        elType.getKey(keyName).types,
                        Mt.getPropOfName(allProps, keyName)
                    ));
                });
                return list(type);
            } else if (name.equals("array_chunk")) {
                return callCtx.getArgMt(0).getInArray(call).mt().types;
            } else if (name.equals("array_intersect_key") || name.equals("array_diff_key")
                    || name.equals("array_intersect_assoc") || name.equals("array_diff_assoc")
                    || name.equals("array_diff") || name.equals("strval")
            ) {
                // do something more clever?
                return callCtx.getArgMt(0).types;
            } else if (name.equals("compact")) {
                return list(compact(callCtx, call));
            } else if (name.equals("implode")) {
                return list(implode(callCtx, call));
            } else if (name.equals("array_keys")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey(KeyType.integer(call)).addType(Tls.onDemand(() -> callCtx.getArgMt(0).types
                    .fap(t -> t.keys)
                    .fap(k -> k.keyType.getTypes())
                    .fap(kt -> opt(kt.stringValue)
                        .map(keyName -> new DeepType(kt.definition, PhpType.STRING, keyName)))
                    .wap(types -> new Mt(types))));
                return list(arrt);
            } else if (name.equals("func_get_args")) {
                return ctx.func().getArg(new ArgOrder(0, true)).itr().fap(a -> a.types);
            } else if (name.equals("func_get_arg")) {
                return callCtx.getArg(0).itr()
                    .fop(mt -> opt(mt.getStringValue()))
                    .flt(str -> Tls.isNum(str))
                    .map(str -> Integer.parseInt(str))
                    .fop(order -> ctx.func().getArg(order))
                    .fap(mt -> mt.types).map(a -> a);
            } else if (name.equals("call_user_func_array")) {
                IExprCtx newCtx = lParams.gat(1)
                    .fop(toCast(PhpExpression.class))
                    .map(args -> ctx.subCtxIndirect(args))
                    .def(ctx.subCtxEmpty());
                return callCtx.getArgMt(0).types
                    .fap(t -> t.getReturnTypes(newCtx)).map(a -> a);
            } else if (name.equals("explode")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey(KeyType.integer(call))
                    .addType(() -> new Mt(list(new DeepType(call, PhpType.STRING))), PhpType.STRING);
                return list(arrt);
            } else if (name.equals("get_called_class")) {
                return ctx.getSelfType().map(idea -> DeepType.makeClsRef(call, idea));
            } else if (name.equals("get_object_vars")) {
                return list(get_object_vars(callCtx, call));
            } else {
                return It.frs(
                    () -> new ReturnTypeDefs(ctx.subCtxEmpty()).getReturnType(call, callCtx),
                    () -> opt(call.resolve())
                        // try to get type info from standard_2.php
                        .fop(toCast(Function.class))
                        .map(func -> new DeepType(call, getDocType(func)))
                );
            }
        }).def(list());
    }

    public static It<String> getCallFqn(FunctionReferenceImpl funcCall)
    {
        return opt(funcCall.getFQN()).itr()
            .cct(opt(funcCall.getFirstChild()).map(i -> i.getText()));
    }

    public It<DeepType> resolve(FunctionReferenceImpl funcCall)
    {
        IExprCtx funcCtx = ctx.subCtxDirect(funcCall);
        return It.cnc(
            findBuiltInFuncCallType(funcCall),
            getCallFqn(funcCall).fap(fqn -> MethCallRes.findFqnMetaDefRetType(fqn, ctx)),
            opt(funcCall.getFirstChild())
                .fop(toCast(PhpExpression.class))
                .fap(funcVar -> ctx.findExprType(funcVar))
                .fap(t -> t.getReturnTypes(funcCtx)),
            opt(funcCall.resolve())
                .fop(Tls.toCast(FunctionImpl.class))
                .fap(func -> new ClosRes(ctx)
                    .resolve(func).getReturnTypes(funcCtx))
        );
    }
}
