package org.klesun.deep_assoc_completion;

import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.AssignmentExpression;
import com.jetbrains.php.lang.psi.elements.ParenthesizedExpression;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.resolvers.*;
import org.klesun.lang.L;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

/**
 * Provides mechanism to determine expression type.
 * Unlike original jetbrain's type resolver, this
 * includes associative array key information
 */
public class DeepTypeResolver extends Lang
{
    /** @debug */
    public static Opt<Iterable<DeepType>> resolveIn(PsiElement expr, FuncCtx ctx)
    {
        return Opt.fst(
            () -> opt(null) // for coma formatting
            , () -> Tls.cast(VariableImpl.class, expr)
                .map(v -> new VarRes(ctx).resolve(v))
            , () -> Tls.cast(ArrayCreationExpressionImpl.class, expr)
                .map(arr -> new ArrCtorRes(ctx).resolve(arr).mt().types)
            , () -> Tls.cast(FunctionReferenceImpl.class, expr)
                .map(call -> new FuncCallRes(ctx).resolve(call))
            , () -> Tls.cast(MethodReferenceImpl.class, expr)
                .map(call -> new MethCallRes(ctx).resolveCall(call).types)
            , () -> Tls.cast(ArrayAccessExpressionImpl.class, expr)
                .map(keyAccess -> new ArrAccRes(ctx).resolve(keyAccess).types)
            , () -> Tls.cast(FieldReferenceImpl.class, expr)
                .map(fieldRef -> new FieldRes(ctx).resolve(fieldRef))
            , () -> Tls.cast(StringLiteralExpressionImpl.class, expr)
                .map(lit -> list(new DeepType(lit)))
            , () -> Tls.cast(ConstantReferenceImpl.class, expr)
                .flt(cst -> // they are defined through themselves in Core_d.php
                    !cst.getText().toLowerCase().equals("null") &&
                    !cst.getText().toLowerCase().equals("true") &&
                    !cst.getText().toLowerCase().equals("false"))
                .map(cst -> L(cst.multiResolve(false))
                    .map(ref -> ref.getElement())
                    .fop(toCast(PhpDefineImpl.class))
                    .fop(def -> opt(def.getValue()))
                    .fop(toCast(PhpExpression.class))
                    .fap(exp -> ctx.findExprType(exp).types))
            , () -> Tls.cast(AssignmentExpression.class, expr)
                .map(ass -> ass.getValue())
                .fop(toCast(PhpExpression.class))
                .map(val -> ctx.findExprType(val).types)
            , () -> Tls.cast(ParenthesizedExpression.class, expr)
                .map(par -> par.getArgument())
                .fop(toCast(PhpExpression.class))
                .map(val -> ctx.findExprType(val).types)
            , () -> Tls.cast(ClassConstantReferenceImpl.class, expr)
                .map(cst -> L(cst.multiResolve(false))
                    .map(ref -> ref.getElement())
                    .fop(toCast(ClassConstImpl.class))
                    .map(a -> a.getDefaultValue())
                    .fop(toCast(PhpExpression.class))
                    .fap(exp -> new FuncCtx(ctx.getSearch()).findExprType(exp).types))
            , () -> Tls.cast(PhpExpressionImpl.class, expr)
                .map(v -> v.getFirstChild())
                .fop(toCast(FunctionImpl.class))
                .map(lambda -> list(new ClosRes(ctx).resolve(lambda)))
            , () -> Tls.cast(PhpExpressionImpl.class, expr)
                .fop(casted -> opt(casted.getText())
                    .flt(text -> Tls.regex("^\\d+$", text).has())
                    .map(Integer::parseInt)
                    .map(num -> list(new DeepType(casted, num))))
            // leave rest to MiscRes
            , () -> new MiscRes(ctx).resolve(expr)
        );
    }
}
