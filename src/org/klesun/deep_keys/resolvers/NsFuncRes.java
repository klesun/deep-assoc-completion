package org.klesun.deep_keys.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

public class NsFuncRes extends Lang
{
    private IFuncCtx ctx;

    public NsFuncRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private MultiType findPsiExprType(PsiElement psi)
    {
        return Tls.cast(PhpExpression.class, psi)
            .map(casted -> ctx.findExprType(casted))
            .def(new MultiType(L()));
    }

    /** @return - opt<null> if call is not a built-in function */
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

                    // TODO: think of a way how to pass them to the function
                    S<MultiType> onDemand = Tls.onDemand(() ->
                        findPsiExprType(array).getEl());
                    L<S<MultiType>> argGetters = list(onDemand::get);
                    IFuncCtx funcCtx = ctx.subCtx(argGetters);

                    findPsiExprType(callback).types.map(t -> t.returnTypes)
                        .fch(rts -> mapRetType.indexTypes.addAll(rts));
                }
                result.add(mapRetType);
            } else if (name.equals("array_filter") || name.equals("array_reverse")
                    || name.equals("array_splice") || name.equals("array_slice")
                    || name.equals("array_values")
            ) {
                // array type unchanged
                L(params).gat(0).map(p -> findPsiExprType(p).types).thn(result::addAll);
            } else if (name.equals("array_combine")) {
                // if we store all literal values of a key, we could even recreate the
                // associative array here, but for now just merging all possible values together
                L(params).gat(1).map(p -> findPsiExprType(p).types).thn(result::addAll);
            } else if (name.equals("array_pop") || name.equals("array_shift")) {
                L(params).gat(0).map(p -> findPsiExprType(p).getEl().types).thn(result::addAll);
            } else if (name.equals("array_merge")) {
                for (PsiElement paramPsi: params) {
                    result.addAll(findPsiExprType(paramPsi).types);
                }
            } else if (name.equals("array_column")) {
                if (params.length > 1) {
                    MultiType elType = findPsiExprType(params[0]).getEl();
                    Tls.cast(StringLiteralExpression.class, params[1])
                        .map(lit -> lit.getContents())
                        .map(keyName -> elType.types
                            .fop(type -> getKey(type.keys, keyName))
                            .fap(keyRec -> keyRec.types))
                        .map(lTypes -> lTypes.s)
                        .flt(itypes -> itypes.size() > 0)
                        .map(itypes -> {
                            DeepType type = new DeepType(call);
                            type.indexTypes.addAll(itypes);
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
            }
            return result;
        }).def(list());
    }

    public MultiType resolve(FunctionReferenceImpl funcCall)
    {
        L<PsiElement> args = L(funcCall.getParameters());
        L<S<MultiType>> argGetters = args.map((psi) -> () -> findPsiExprType(psi));
        IFuncCtx funcCtx = ctx.subCtx(argGetters);
        return new MultiType(list(
            findBuiltInFuncCallType(funcCall),
            Opt.fst(list(opt(null)
                , opt(funcCall.resolve())
                    .fap(Tls.toCast(FunctionImpl.class))
                    .map(func -> new ClosRes(funcCtx).resolve(func).returnTypes)
                // idea is not able to resolve function passed as arg, but we are thanks to our context
                , opt(funcCall.getFirstChild())
                    .fap(toCast(PhpExpression.class))
                    .map(func -> ctx.findExprType(func).types.fap(t -> t.returnTypes))
            ))
            .def(list())
        ).fap(a -> a));
    }
}
