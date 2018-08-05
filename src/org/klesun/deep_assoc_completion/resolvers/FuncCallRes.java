package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringEscapeUtils;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.ScopeFinder;
import org.klesun.deep_assoc_completion.helpers.ArgOrder;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import javax.annotation.Nullable;

public class FuncCallRes extends Lang
{
    private FuncCtx ctx;

    public FuncCallRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private MultiType findPsiExprType(PsiElement psi)
    {
        return Tls.cast(PhpExpression.class, psi)
            .map(casted -> ctx.findExprType(casted))
            .def(new MultiType(L()));
    }

    private static L<VariableImpl> findVarRefsInFunc(GroupStatement meth, String varName)
    {
        return Tls.findChildren(
            meth, VariableImpl.class,
            subPsi -> !(subPsi instanceof FunctionImpl)
        ).flt(varUsage -> varName.equals(varUsage.getName()));
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

    private L<DeepType> findBuiltInFuncCallType(FunctionReferenceImpl call)
    {
        return opt(call.getName()).map(name -> {
            L<DeepType> result = L();
            FuncCtx callCtx = ctx.subCtxDirect(call);
            PsiElement[] params = call.getParameters();
            L<PsiElement> lParams = L(params);
            if (name.equals("array_map")) {
                DeepType mapRetType = new DeepType(call);
                MultiType arrMt = callCtx.getArgMt(1);
                FuncCtx subCtx = L(params).gat(1)
                    .fop(array -> Tls.cast(PhpExpression.class, array))
                    .uni(argArr -> ctx.subCtxSingleArgArr(argArr),
                        () -> new FuncCtx(ctx.getSearch())
                    );
                S<MultiType> getElMt = () -> callCtx.getArgMt(0).types
                    .fap(t -> t.getReturnTypes(subCtx))
                    .wap(MultiType::new);
                arrMt.types.fch(t -> {
                    DeepType mapped = new DeepType(t.definition, PhpType.ARRAY);
                    t.keys.forEach((k, v) -> mapped.addKey(k, v.definition)
                        .addType(getElMt, call.getType().elementType()));
                    result.add(mapped);
                });
                if (arrMt.hasNumberIndexes()) {
                    mapRetType.listElTypes.add(getElMt);
                } else if (
                    arrMt.getKeyNames().size() == 0 ||
                    arrMt.types.any(t -> t.anyKeyElTypes.size() > 0)
                ) {
                    mapRetType.anyKeyElTypes.add(getElMt);
                }
                result.add(mapRetType);
            } else if (name.equals("array_filter") || name.equals("array_reverse")
                    || name.equals("array_splice") || name.equals("array_slice")
                    || name.equals("array_values")
            ) {
                // array type unchanged
                result.addAll(callCtx.getArgMt(0).types);
            } else if (name.equals("array_combine")) {
                DeepType combine = new DeepType(call, PhpType.ARRAY);
                Dict<L<DeepType>> keyToTypes = callCtx.getArgMt(0).getEl().types
                    .gop(t -> opt(t.stringValue));
                S<MultiType> getElMt = () -> callCtx.getArgMt(1).getEl();
                PhpType ideaElType = L(params).gat(1)
                    .fop(toCast(PhpExpression.class))
                    .map(e -> e.getType()).def(PhpType.MIXED);
                keyToTypes.fch((types,keyName) -> combine
                    .addKey(keyName, types.map(t -> t.definition).fst().def(call))
                        .addType(getElMt, ideaElType));
                combine.anyKeyElTypes.add(getElMt);
                result.add(combine);
            } else if (name.equals("array_pop") || name.equals("array_shift")
                    || name.equals("current") || name.equals("end") || name.equals("next")
                    || name.equals("prev") || name.equals("reset")
            ) {
                result.addAll(callCtx.getArgMt(0).getEl().types);
            } else if (name.equals("array_merge")) {
                for (int i = 0; i < params.length; ++i) {
                    result.addAll(callCtx.getArg(i).fap(mt -> mt.types));
                }
            } else if (name.equals("array_column")) {
                MultiType elType = callCtx.getArgMt(0).getEl();
                @Nullable String keyName = callCtx.getArgMt(1).getStringValue();
                DeepType type = new DeepType(call);
                type.listElTypes.add(() -> elType.getKey(keyName));
                result.add(type);
            } else if (name.equals("array_chunk")) {
                result.add(callCtx.getArgMt(0).getInArray(call));
            } else if (name.equals("array_intersect_key") || name.equals("array_diff_key")
                    || name.equals("array_intersect_assoc") || name.equals("array_diff_assoc")
            ) {
                // do something more clever?
                result.addAll(callCtx.getArgMt(0).types);
            } else if (name.equals("compact")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                Tls.findParent(call, GroupStatement.class, a -> true)
                    .thn(scope -> Tls.range(0, params.length)
                        .fch(i -> callCtx.getArg(i)
                            .map(keyt -> keyt.getStringValue())
                            .thn(varName -> {
                                L<VariableImpl> refs = findVarRefsInFunc(scope, varName)
                                    .flt(ref -> ScopeFinder.didPossiblyHappen(ref, call))
                                    ;
                                if (refs.size() > 0) {
                                    PhpType briefType = new PhpType();
                                    refs.map(var -> Tls.getIdeaType(var)).fch(briefType::add);
                                    arrt.addKey(varName, call)
                                        .addType(() -> refs
                                            .fap(ref -> ctx.findExprType(ref).types)
                                            .wap(MultiType::new), briefType);
                                }
                            })));
                result.add(arrt);
            } else if (name.equals("implode")) {
                String delim = callCtx.getArgMt(0).getStringValues().gat(0).def(" ");
                L<String> parts = callCtx.getArgMt(1).types
                    .fap(t -> L(t.keys.values()))
                    .fap(kv -> kv.getTypes())
                    .fop(t -> opt(t.stringValue));
                String joined = Tls.implode(delim, parts);
                // PHP string is not java string, of course, but pretty close
                String unescaped = StringEscapeUtils.unescapeJava(joined);
                DeepType type = new DeepType(call, PhpType.STRING, unescaped);
                result.add(type);
            } else if (name.equals("array_keys")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.listElTypes.add(() -> callCtx.getArgMt(0).getKeyNames()
                    .map(keyName -> new DeepType(call, PhpType.STRING, keyName))
                    .wap(types -> new MultiType(types)));
                result.add(arrt);
            } else if (name.equals("func_get_args")) {
                result.addAll(ctx.getArg(new ArgOrder(0, true)).fap(a -> a.types));
            } else if (name.equals("func_get_arg")) {
                callCtx.getArg(0)
                    .fop(mt -> opt(mt.getStringValue()))
                    .flt(str -> Tls.isNum(str))
                    .map(str -> Integer.parseInt(str))
                    .fop(order -> ctx.getArg(order))
                    .thn(mt -> result.addAll(mt.types));
            } else if (name.equals("call_user_func_array")) {
                FuncCtx newCtx = lParams.gat(1)
                    .fop(toCast(PhpExpression.class))
                    .map(args -> ctx.subCtxIndirect(args))
                    .def(new FuncCtx(ctx.getSearch()));
                callCtx.getArgMt(0).types
                    .fap(t -> t.getReturnTypes(newCtx))
                    .fch(t -> result.add(t));
            } else if (name.equals("explode")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.listElTypes.add(() -> new MultiType(list(new DeepType(call, PhpType.STRING))));
                result.add(arrt);
            } else {
                // try to get type info from standard_2.php
                opt(call.resolve())
                    .fop(toCast(Function.class))
                    .thn(func -> result.add(new DeepType(call, getDocType(func))))
                    ;
            }
            return result;
        }).def(list());
    }

    public MultiType resolve(FunctionReferenceImpl funcCall)
    {
        FuncCtx funcCtx = ctx.subCtxDirect(funcCall);
        return new MultiType(list(
            findBuiltInFuncCallType(funcCall),
            opt(funcCall.getFirstChild())
                .fop(toCast(PhpExpression.class))
                .map(funcVar -> ctx.findExprType(funcVar))
                .map(mt -> mt.types.fap(t -> t.getReturnTypes(funcCtx)))
                .def(L()),
            opt(funcCall.resolve())
                .fop(Tls.toCast(FunctionImpl.class))
                .map(func -> new ClosRes(ctx).resolve(func).getReturnTypes(funcCtx))
                .def(L())
        ).fap(a -> a));
    }
}
