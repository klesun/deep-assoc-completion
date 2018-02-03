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
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

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

    private L<DeepType> findBuiltInFuncCallType(FunctionReferenceImpl call)
    {
        return opt(call.getName()).map(name -> {
            L<DeepType> result = L();
            PsiElement[] params = call.getParameters();
            if (name.equals("array_map")) {
                DeepType mapRetType = new DeepType(call);
                if (params.length > 1) {
                    PsiElement callback = params[0];
                    PsiElement array = params[1];
                    MultiType arrMt = Tls.cast(PhpExpression.class, array)
                        .map(exp -> ctx.findExprType(exp))
                        .def(MultiType.INVALID_PSI);
                    FuncCtx subCtx = Tls.cast(PhpExpression.class, array)
                        .uni(argArr -> ctx.subCtxArgArr(argArr),
                            () -> new FuncCtx(ctx.getSearch())
                        );
                    findPsiExprType(callback).types
                        .map(t -> t.getReturnTypes(subCtx))
                        .fch(rts -> {
                            if (arrMt.hasNumberIndexes()) {
                                mapRetType.listElTypes.addAll(rts);
                            } else {
                                mapRetType.anyKeyElTypes.addAll(rts);
                            }
                        });
                }
                result.add(mapRetType);
            } else if (name.equals("array_filter") || name.equals("array_reverse")
                    || name.equals("array_splice") || name.equals("array_slice")
                    || name.equals("array_values")
            ) {
                // array type unchanged
                L(params).gat(0).map(p -> findPsiExprType(p).types).thn(result::addAll);
            } else if (name.equals("array_combine")) {
                DeepType combine = new DeepType(call, PhpType.ARRAY);
                L<String> keyNames = L(params).gat(0)
                    .map(p -> findPsiExprType(p))
                    .map(mt -> mt.getEl())
                    .fap(mt -> mt.getStringValues());
                MultiType elMt = L(params).gat(1)
                    .map(p -> findPsiExprType(p))
                    .def(MultiType.INVALID_PSI)
                    .getEl();
                keyNames.fch(keyName -> combine
                    .addKey(keyName, call)
                        .addType(() -> elMt, elMt.getIdeaType()));
                combine.anyKeyElTypes.addAll(elMt.types);
                result.add(combine);
            } else if (name.equals("array_pop") || name.equals("array_shift")) {
                L(params).gat(0).map(p -> findPsiExprType(p).getEl().types).thn(result::addAll);
            } else if (name.equals("array_merge")) {
                for (PsiElement paramPsi: params) {
                    result.addAll(findPsiExprType(paramPsi).types);
                }
            } else if (name.equals("array_column")) {
                if (params.length > 1) {
                    MultiType elType = findPsiExprType(params[0]).getEl();
                    Tls.cast(PhpExpression.class, params[1])
                        .map(keyNamePsi -> ctx.findExprType(keyNamePsi))
                        .map(mt -> opt(mt.getStringValue()))
                        .map(keyName -> elType.getKey(keyName.def(null)).types)
                        .flt(itypes -> itypes.size() > 0)
                        .map(itypes -> {
                            DeepType type = new DeepType(call);
                            type.listElTypes.addAll(itypes);
                            return list(type);
                        })
                        .thn(result::addAll);
                }
            } else if (name.equals("array_chunk")) {
                L(params).gat(0)
                    .map(this::findPsiExprType)
                    .map(mt -> mt.getInArray(call))
                    .thn(result::add);
            } else if (name.equals("array_intersect_key") || name.equals("array_diff_key")
                    || name.equals("array_intersect_assoc") || name.equals("array_diff_assoc")
            ) {
                // do something more clever?
                L(params).gat(0).map(p -> findPsiExprType(p).types).thn(result::addAll);
            } else if (name.equals("compact")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                Tls.findParent(call, GroupStatement.class, a -> true)
                    .thn(scope -> L(params)
                        .fch(argPsi -> opt(findPsiExprType(argPsi))
                            .map(keyt -> keyt.getStringValue())
                            .thn(varName -> {
                                L<VariableImpl> refs = findVarRefsInFunc(scope, varName)
                                    .flt(ref -> ScopeFinder.didPossiblyHappen(ref, call))
                                    ;
                                if (refs.size() > 0) {
                                    PhpType briefType = new PhpType();
                                    refs.map(var -> var.getType()).fch(briefType::add);
                                    arrt.addKey(varName, argPsi)
                                        .addType(() -> refs
                                            .fap(ref -> ctx.findExprType(ref).types)
                                            .wap(MultiType::new), briefType);
                                }
                            })));
                result.add(arrt);
            } else if (name.equals("implode")) {
                if (params.length > 1) {
                    String delim = opt(params[0])
                        .fop(toCast(PhpExpression.class))
                        .map(exp -> ctx.findExprType(exp))
                        .fop(mt -> mt.getStringValues().gat(0))
                        .def(" ");
                    L<String> parts = opt(params[1])
                        .fop(toCast(PhpExpression.class))
                        .map(exp -> ctx.findExprType(exp))
                        .fap(t -> t.types)
                        .fap(t -> L(t.keys.values()))
                        .fap(kv -> kv.getTypes())
                        .fop(t -> opt(t.stringValue));
                    String joined = Tls.implode(delim, parts);
                    // PHP string is not java string, of course, but pretty close
                    String unescaped = StringEscapeUtils.unescapeJava(joined);
                    DeepType type = new DeepType(call, PhpType.STRING, unescaped);
                    result.add(type);
                }
            } else if (name.equals("array_keys")) {
                if (params.length > 1) {
                    L<DeepType> keyTypes = opt(params[0])
                        .fop(toCast(PhpExpression.class))
                        .map(exp -> ctx.findExprType(exp))
                        .fap(mt -> mt.getKeyNames())
                        .map(keyName -> new DeepType(call, PhpType.STRING, keyName));
                    result.addAll(keyTypes);
                }
            } else {
                // try to get type info from standard_2.php
                opt(call.resolve())
                    .fop(toCast(Function.class))
                    .thn(func -> result.add(new DeepType(func, func.getDocType())))
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
