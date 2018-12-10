package org.klesun.deep_assoc_completion;

import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.helpers.ExprCtx;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.IExprCtx;
import org.klesun.deep_assoc_completion.resolvers.*;
import org.klesun.lang.*;

import static org.klesun.lang.Lang.*;

/**
 * Provides mechanism to determine expression type.
 * Unlike original jetbrain's type resolver, this
 * includes associative array key information
 */
public class DeepTypeResolver
{
    private static It<DeepType> resolveClsConst(ClassConstantReference cst, IExprCtx ctx)
    {
        if ("class".equals(cst.getName())) {
            return opt(cst.getClassReference())
                .fap(cls -> new MiscRes(ctx).resolveClassReference(cst, cls))
                .map(ideaType -> DeepType.makeClsRef(cst, ideaType));
        } else {
            return It(cst.multiResolve(false))
                .map(ref -> ref.getElement())
                .fop(toCast(ClassConstImpl.class))
                .map(a -> a.getDefaultValue())
                .fop(toCast(PhpExpression.class))
                .fap(exp -> ctx.subCtxEmpty().findExprType(exp));
        }
    }

    public static It<DeepType> resolveIn(PhpExpression expr, IExprCtx ctx)
    {
        It<DeepType> tit = It.frs(() -> It.non()
            , () -> Tls.cast(VariableImpl.class, expr)
                .fap(v -> new VarRes(ctx).resolve(v))
            , () -> Tls.cast(ArrayCreationExpressionImpl.class, expr)
                .fap(arr -> new ArrCtorRes(ctx).resolve(arr).mt().types)
            , () -> Tls.cast(FunctionReferenceImpl.class, expr)
                .fap(call -> new FuncCallRes(ctx).resolve(call))
            , () -> Tls.cast(ArrayAccessExpressionImpl.class, expr)
                .fap(keyAccess -> new ArrAccRes(ctx).resolve(keyAccess))
            , () -> Tls.cast(StringLiteralExpressionImpl.class, expr)
                .fap(lit -> list(new DeepType(lit)))
            , () -> Tls.cast(StringLiteralExpressionImpl.class, expr)
                .fap(lit -> list(new DeepType(lit)))
            , () -> Tls.cast(ConstantReferenceImpl.class, expr)
                .flt(cst -> cst.getText().equals("__DIR__"))
                .map(ref -> ctx.getFakeFileSource().def(ref))
                .fap(psi -> opt(psi.getContainingFile()))
                .fap(psi -> opt(psi.getOriginalFile()))
                .fap(psiFile -> opt(psiFile.getContainingDirectory()))
                .fap(psiDir -> opt(psiDir.getVirtualFile()))
                .map(dir -> dir.getPath() + "/")
                .fap(dirPath -> som(new DeepType(expr, PhpType.STRING, dirPath)))
            , () -> Tls.cast(ConstantReferenceImpl.class, expr)
                .flt(cst -> // they are defined through themselves in Core_d.php
                    !cst.getText().toLowerCase().equals("null") &&
                    !cst.getText().toLowerCase().equals("true") &&
                    !cst.getText().toLowerCase().equals("false"))
                .fap(cst -> It(cst.multiResolve(false))
                    .map(ref -> ref.getElement())
                    .fop(toCast(PhpDefineImpl.class))
                    .fop(def -> opt(def.getValue()))
                    .fop(toCast(PhpExpression.class))
                    .fap(exp -> ctx.findExprType(exp)))
            , () -> Tls.cast(AssignmentExpression.class, expr)
                .map(ass -> ass.getValue())
                .fop(toCast(PhpExpression.class))
                .fap(val -> ctx.findExprType(val))
            , () -> Tls.cast(ParenthesizedExpression.class, expr)
                .map(par -> par.getArgument())
                .fop(toCast(PhpExpression.class))
                .fap(val -> ctx.findExprType(val))
            , () -> Tls.cast(ClassReferenceImpl.class, expr)
                .fap(ref -> {
                    It<DeepType> clsTit = It.frs(
                        () -> opt(expr.getFirstChild())
                            // in `new $someVar()` it is Variable PSI, in `new SomeCls()` it is Leaf PSI
                            .fop(toCast(PhpExpression.class))
                            .fap(clsVar -> ctx.findExprType(clsVar)),
                        () -> som(DeepType.makeClsRef(expr, expr.getType()))
                    );
                    if (FuncCtx.isWhitelistedStaticThis(expr) &&
                        list("self", "static").contains(expr.getText())
                    ) {
                        // deprecated PHP feature - to access non-static fields/methods from non-static context via self::
                        clsTit = It.cnc(clsTit, ctx.getThisType());
                    }
                    return clsTit;
                })
            , () -> Tls.cast(ClassConstantReferenceImpl.class, expr)
                .fap(cst -> resolveClsConst(cst, ctx))
            , () -> Tls.cast(PhpExpressionImpl.class, expr)
                .map(v -> v.getFirstChild())
                .fop(toCast(FunctionImpl.class))
                .fap(lambda -> list(new ClosRes(ctx).resolve(lambda)))
            , () -> Tls.cast(PhpExpressionImpl.class, expr)
                .fap(casted -> opt(casted.getText())
                    .flt(text -> Tls.regex("^\\d+$", text).has())
                    .fap(num -> list(DeepType.makeInt(casted, num))))
            // leave rest to MiscRes
            , () -> new MiscRes(ctx).resolve(expr)
            , () -> Tls.cast(MethodReferenceImpl.class, expr)
                .fap(call -> new MethCallRes(ctx).resolveCall(call))
        );
        if (tit.has()) {
            return tit;
        }

        return Opt.fst(
            () -> opt(null) // for coma formattingx
            // I can't understand why moving this to the list above causes option chain test to fail
            , () -> Tls.cast(FieldReferenceImpl.class, expr)
                .map(fieldRef -> new FieldRes(ctx).resolve(fieldRef))

            , () -> Tls.cast(PhpExpression.class, expr)
                .map(t -> list(new DeepType(t)))
        ).fap(a -> a);
    }
}
