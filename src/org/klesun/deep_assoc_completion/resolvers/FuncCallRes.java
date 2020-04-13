package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty;
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
import org.klesun.deep_assoc_completion.resolvers.builtins.MysqliRes;
import org.klesun.deep_assoc_completion.structures.*;
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

    private static It<DeepType> getDocType(Function func, IExprCtx ctx)
    {
        return MethCallRes.findFuncDocRetType(func, ctx);
    }

    private It<DeepType> array_map(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        Mt arrMt = callCtx.getArgMt(1);
        IExprCtx subCtx = L(call.getParameters()).gat(1)
            .fop(array -> Tls.cast(PhpExpression.class, array))
            .uni(argArr -> ctx.subCtxSingleArgArr(argArr, 0),
                () -> ctx.subCtxEmpty()
            );
        S<Mt> getElMt = Tls.onDemand(() -> callCtx.getArgMt(0).types
            .fap(t -> t.getReturnTypes(subCtx))
            .wap(Mt::new));
        It<DeepType> eachTMapped = arrMt.types
            .map(t -> new Build(t.definition, PhpType.ARRAY)
                .keys(t.keys.map((v, i) -> new Key(v.keyType, v.definition)
                    .addType(getElMt, call.getType().elementType())))
                .get());
        MemIt<Key> srcKeys = arrMt.types.fap(t -> t.keys).mem();
        It<DeepType> ktg = srcKeys.fap(k -> k.keyType.getTypes());

        Key keyEntry = new Key(KeyType.mt(ktg, call), call)
            .addType(getElMt, PhpType.MIXED);
        It<DeepType> mapRetTit = new Build(call, PhpType.ARRAY)
            .keys(som(keyEntry)).itr();

        return It.cnc(mapRetTit, eachTMapped);
    }

    private It<DeepType> array_reduce(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        IExprCtx subCtx = L(call.getParameters()).gat(0)
            .fop(array -> Tls.cast(PhpExpression.class, array))
            .uni(argArr -> ctx.subCtxSingleArgArr(argArr, 1),
                () -> ctx.subCtxEmpty()
            );
        return callCtx.getArg(1)
            .fap(ft -> ft.types)
            .fap(t -> t.getReturnTypes(subCtx));
    }

    private DeepType array_combine(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        S<Mt> getElMt = Tls.onDemand(() -> callCtx.getArgMt(1).getEl());
        PhpType ideaElType = L(call.getParameters()).gat(1)
            .fop(toCast(PhpExpression.class))
            .map(e -> e.getType()).def(PhpType.MIXED);

        MemIt<DeepType> kit = callCtx.getArgMt(0).getEl().types;
        Key keyEntry = new Key(KeyType.mt(kit, call))
            .addType(getElMt, ideaElType);

        return new Build(call, PhpType.ARRAY)
            .keys(som(keyEntry)).get();
    }

    private It<DeepType> array_column(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        Mt srcArrMt = callCtx.getArgMt(0);
        KeyType kt = callCtx.getArg(2)
            .map(groupKmt -> KeyType.mt(groupKmt.types, call))
            .map(groupKt -> srcArrMt.getEl().getKey(groupKt))
            .map(kmt -> KeyType.mt(kmt.types, call))
            .def(KeyType.integer(call));
        S<Mt> getElMt = () -> callCtx.getArgMt(0).getEl();

        Key keyRec = new Key(kt).addType(() -> {
            if (L(call.getParameters()).any(p -> p.getText().equals("null"))) {
                return getElMt.get();
            } else {
                Mt elType = getElMt.get();
                String keyName = callCtx.getArgMt(1).getStringValue();
                It<Key> allProps = FieldRes.getPublicProps(
                    elType, call.getProject(), ctx.subCtxEmpty()
                );
                return new Mt(It.cnc(
                    elType.getKey(keyName).types,
                    Mt.getPropOfName(allProps, keyName)
                ));
            }
        });
        return new Build(call, PhpType.ARRAY)
            .keys(som(keyRec)).itr();
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
        Mt sourceMt = callCtx.getArgMt(0);
        Mt newValueMt = sourceMt.getKeyNames()
            .map(name -> new DeepType(call, PhpType.STRING, name)).wap(Mt::new);
        MemIt<String> newKeys = sourceMt.getEl().types.map(t -> t.stringValue).mem();

        It<Key> keys = It.non();
        /** @performance-hole */
        if (!newKeys.has() || newKeys.any(k -> k == null)) {
            Key keyEntry = new Key(KeyType.unknown(call), call)
                .addType(() -> newValueMt, PhpType.MIXED);
            keys = keys.cct(som(keyEntry));
        }
        for (String key: newKeys) {
            PhpType briefType = It(newValueMt.types).map(t -> t.briefType).fst().def(PhpType.EMPTY);
            Key keyEntry = new Key(key, call)
                .addType(() -> newValueMt, briefType);
            keys = keys.cct(som(keyEntry));
        }
        return new Build(call, PhpType.ARRAY).keys(keys).get();
    }

    private DeepType compact(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        It<Key> keyEntries = Tls.findParent(call, GroupStatement.class, a -> true)
            .fap(scope -> Tls.range(0, call.getParameters().length)
                .fap(i -> callCtx.getArg(i)
                    .map(keyt -> keyt.getStringValue())
                    .fap(varName -> {
                        L<Variable> refs = findVarRefsInFunc(scope, varName)
                            .flt(ref -> ScopeFinder.didPossiblyHappen(ref, call)).arr()
                            ;
                        if (refs.size() > 0) {
                            PhpType briefType = new PhpType();
                            refs.map(var -> Tls.getIdeaType(var)).fch(briefType::add);
                            return som(new Key(varName, call)
                                .addType(() -> refs
                                    .fap(ref -> ctx.findExprType(ref))
                                    .wap(Mt::new), briefType));
                        } else {
                            return non();
                        }
                    })));

        return new Build(call, PhpType.ARRAY).keys(keyEntries).get();
    }

    private DeepType implode(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        String delim = callCtx.getArgMt(0).getStringValues().fst().def(" ");
        It<String> parts = callCtx.getArgMt(1).types
            .fap(t -> t.keys)
            .fap(kv -> kv.getValueTypes())
            .fop(t -> opt(t.stringValue));
        String joined = Tls.implode(delim, parts);
        // PHP string is not java string, of course, but pretty close
        String unescaped = StringEscapeUtils.unescapeJava(joined);
        DeepType type = new DeepType(call, PhpType.STRING, unescaped);
        return type;
    }

    private DeepType getVars(FunctionReferenceImpl call, It<PhpClass> clses, boolean isStatic)
    {
        It<Key> keyEntries = clses.fap(cls -> cls.getFields())
            .flt(fld -> !fld.isConstant())
            .flt(fld -> !(fld instanceof PhpDocProperty))
            .flt(fld -> isStatic == fld.getModifier().isStatic())
            .map(fld -> new Key(fld.getName(), fld)
                .addType(() -> FieldRes.declToExplTypes(fld, ctx.subCtxEmpty())
                    .wap(ts -> new Mt(ts)), fld.getType()));

        return new Build(call, PhpType.ARRAY).keys(keyEntries).get();
    }

    private DeepType get_object_vars(IExprCtx callExprCtx, FunctionReferenceImpl call)
    {
        It<PhpClass> clses = callExprCtx.func().hasArgs()
            ? callExprCtx.func().getArg(0)
                .fap(mt -> ArrCtorRes.resolveMtInstCls(mt, call.getProject()))
            : Tls.findParent(callExprCtx.getRealPsi(call), PhpClass.class, a -> true).itr();
        return getVars(call, clses, false);
    }

    private DeepType get_class_vars(IExprCtx callExprCtx, FunctionReferenceImpl call)
    {
        It<PhpClass> clses = callExprCtx.func().getArg(0)
            .fap(mt -> ArrCtorRes.resolveMtClsRefCls(mt, call.getProject()));
        return getVars(call, clses, true);
    }

    private It<DeepType> get_class(IExprCtx callExprCtx, FunctionReferenceImpl call)
    {
        return callExprCtx.func().getArg(0)
            .fap(mt -> ArrCtorRes.resolveMtInstCls(mt, call.getProject()))
            .map(clsPsi -> DeepType.makeClsRef(call, clsPsi.getType()));
    }

    public static DeepType makeAssoc(PsiElement psi, Iterable<T2<String, PhpType>> keys)
    {
        return Mkt.assoc(psi, It(keys)
            .map(t -> t.nme((keyName, ideaType) ->
                T2(keyName, new DeepType(psi, ideaType).mt()))));
    }

    private Iterable<DeepType> findBuiltInFuncCallType(FunctionReferenceImpl call)
    {
        String name = opt(call.getName()).def("");
        IExprCtx callExprCtx = ctx.subCtxDirect(call);
        IFuncCtx callCtx = callExprCtx.func();
        PsiElement[] params = call.getParameters();
        L<PsiElement> lParams = L(params);

        if (name.equals("array_map")) {
            return array_map(callCtx, call);
        } else if (name.equals("array_reduce")) {
            return array_reduce(callCtx, call);
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
            return array_column(callCtx, call);
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
            Key keyEntry = new Key(KeyType.integer(call))
                .addType(Tls.onDemand(() -> callCtx.getArgMt(0).types
                    .fap(t -> t.keys)
                    .fap(k -> k.keyType.getTypes())
                    .fap(kt -> opt(kt.stringValue)
                        .map(keyName -> new DeepType(kt.definition, PhpType.STRING, keyName)))
                    .wap(types -> new Mt(types))));
            return new Build(call, PhpType.ARRAY)
                .keys(som(keyEntry)).itr();
        } else if (name.equals("key")) {
            return callCtx.getArgMt(0)
                .types.fap(t -> t.keys)
                .fap(k -> k.keyType.getTypes());
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
            Mt valMt = new DeepType(call, PhpType.STRING).mt();
            Key keyEntry = new Key(KeyType.integer(call))
                .addType(Granted(valMt), PhpType.STRING);
            return new Build(call, PhpType.ARRAY)
                .keys(som(keyEntry)).itr();
        } else if (name.equals("get_called_class")) {
            return ctx.getSelfType().map(idea -> DeepType.makeClsRef(call, idea));
        } else if (name.equals("get_object_vars")) {
            return list(get_object_vars(callExprCtx, call));
        } else if (name.equals("get_class_vars")) {
            return list(get_class_vars(callExprCtx, call));
        } else if (name.equals("get_class")) {
            return get_class(callExprCtx, call);
        }

        It<DeepType> generalFuncTit = It.frs(
            () -> new ReturnTypeDefs(ctx.subCtxEmpty()).getReturnType(call, callCtx),
            () -> opt(call.resolve())
                // try to get type info from standard_2.php
                .fop(toCast(Function.class))
                .fap(func -> getDocType(func, callExprCtx))
        );

        It<DeepType> mysqliTit = new MysqliRes(ctx)
            .resolveProceduralCall(name, params);

        return It.cnc(generalFuncTit, mysqliTit);
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
            It(funcCall.multiResolve(false))
                .fap(res -> opt(res.getElement()))
                .fap(psi -> Tls.cast(FunctionImpl.class, psi))
                .fap(func -> new ClosRes(ctx)
                    .resolve(func).getReturnTypes(funcCtx))
        );
    }
}
