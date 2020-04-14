package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.*;

import static org.klesun.lang.Lang.*;

/**
 * Provides mechanism to determine expression type.
 * Unlike original jetbrain's type resolver, this
 * includes associative array key information
 *
 * unlike UsageBasedTypeResolver.java, this one resolves the type var
 * has based on what was explicitly assigned to it in code above
 */
public class DirectTypeResolver {
    final private IExprCtx ctx;

    public DirectTypeResolver(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private It<ClassConstImpl> getClsConstDecl(ClassConstantReference cst)
    {
        return It.frs(
            () -> Lang.It(cst.multiResolve(false))
                .map(ref -> ref.getElement())
                .cst(ClassConstImpl.class),
            () -> new FieldRes(ctx).getBriefDecls(cst)
                .cst(ClassConstImpl.class)
        );
    }

    private IIt<DeepType> resolveClsConst(ClassConstantReference cst)
    {
        if ("class".equals(cst.getName())) {
            return opt(cst.getClassReference())
                .fap(cls -> new MiscRes(ctx).resolveClassReference(cst, cls))
                .map(ideaType -> DeepType.makeClsRef(cst, ideaType));
        } else {
            return getClsConstDecl(cst)
                .map(a -> a.getDefaultValue())
                .fop(toCast(PhpExpression.class))
                .fap(exp -> ctx.subCtxEmpty().findExprType(exp))
                // modifying type is actually pretty bad since it could be
                // cached, but for constants we are neglecting that, because nah
                .btw(t -> opt(cst.getClassReference())
                    .cst(ClassReferenceImpl.class)
                    .map(ref -> ref.getFQN())
                    .thn(fqn -> t.cstName = opt(cst.getName())
                        .map(cstName -> fqn + "::" + cstName)));
        }
    }

    public static It<DeepType> resolveConst(Constant cst, IExprCtx ctx)
    {
        return opt(cst.getValue())
            .fop(toCast(PhpExpression.class))
            .fap(exp -> ctx.findExprType(exp))
            // modifying type is actually pretty bad since it could be
            // cached, but for constants we are neglecting that, because nah
            .btw(t -> t.cstName = opt(cst.getName()));
    }

    private static boolean isInFakeFile(PsiElement ref) {
        PsiFile file = ref.getContainingFile();
        PsiDirectory dir = file == null ? null :
            file.getContainingDirectory();
        return dir == null;
    }

    /* to reuse the collection instead of creating new iterator each time, when possible */
    private IResolvedIt<DeepType> resolveAsPlainType(PhpExpression expr)
    {
        return IResolvedIt.fst(L::non
            , () -> Tls.cast(ArrayCreationExpressionImpl.class, expr)
                .map(arr -> new ArrCtorRes(ctx).resolve(arr))
            , () -> Tls.cast(StringLiteralExpressionImpl.class, expr)
                .map(lit -> new DeepType(lit))
            , () -> Tls.cast(ConstantReferenceImpl.class, expr)
                .flt(cst -> // they are defined through themselves in Core_d.php
                    cst.getText().toLowerCase().equals("null") ||
                    cst.getText().toLowerCase().equals("true") ||
                    cst.getText().toLowerCase().equals("false"))
                .map(cst -> new DeepType(cst))
            , () -> Tls.cast(PhpExpressionImpl.class, expr)
                .fop(casted -> opt(casted.getText())
                    .flt(text -> Tls.regex("^\\d+$", text).has())
                    .map(num -> DeepType.makeInt(casted, num)))
            , () -> Tls.cast(PhpExpressionImpl.class, expr)
                .fop(casted -> opt(casted.getText())
                    .flt(text -> Tls.regex("^\\d+\\.\\d+$", text).has())
                    .map(num -> DeepType.makeFloat(casted, num)))
            , () -> Tls.cast(BinaryExpressionImpl.class, expr)
                .fop(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("-")
                        || op.getText().equals("*") || op.getText().equals("/")
                        || op.getText().equals("%") || op.getText().equals("**")
                    )
                    .map(op -> {
                        DeepType type = new DeepType(bin, PhpType.NUMBER);
                        type.isNumber = true;
                        return type;
                    }))
        );
    }

    public IIt<DeepType> resolve(PhpExpression expr)
    {
        IResolvedIt<DeepType> plainTypes = resolveAsPlainType(expr);
        if (plainTypes.has()) {
            // no lazy resolution for straightforward expressions like 'blablabla', 123, ['a' => 5, 'b' => 6]
            return plainTypes;
        }

        return It.frs(It::non
            , () -> Tls.cast(VariableImpl.class, expr)
                .fap(v -> new VarRes(ctx).resolve(v))
            , () -> Tls.cast(FunctionReferenceImpl.class, expr)
                .fap(call -> new FuncCallRes(ctx).resolve(call))
            , () -> Tls.cast(ArrayAccessExpressionImpl.class, expr)
                .fap(keyAccess -> new ArrAccRes(ctx).resolve(keyAccess))
            , () -> Tls.cast(ConstantReferenceImpl.class, expr)
                .flt(cst -> cst.getText().equals("__DIR__"))
                .fap(ref -> !isInFakeFile(ref) ? som(ref) : ctx.getFakeFileSource())
                .fap(psi -> opt(psi.getContainingFile()))
                .fap(psi -> opt(psi.getOriginalFile()))
                .fap(psiFile -> opt(psiFile.getContainingDirectory()))
                .fap(psiDir -> opt(psiDir.getVirtualFile()))
                .map(dir -> dir.getPath() + "/")
                .fap(dirPath -> som(new DeepType(expr, PhpType.STRING, dirPath)))
            , () -> Tls.cast(ConstantReferenceImpl.class, expr)
                .fap(cst -> It(cst.multiResolve(false))
                    .map(ref -> ref.getElement())
                    .cst(Constant.class)
                    .fap(def -> resolveConst(def, ctx)))
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
                .fap(cst -> resolveClsConst(cst))
            , () -> Tls.cast(PhpExpressionImpl.class, expr)
                .map(v -> v.getFirstChild())
                .fop(toCast(FunctionImpl.class))
                .fap(lambda -> list(new ClosRes(ctx).resolve(lambda)))
            // leave rest to MiscRes
            , () -> new MiscRes(ctx).resolve(expr)
            , () -> Tls.cast(MethodReferenceImpl.class, expr)
                .fap(call -> new MethCallRes(ctx).resolveCall(call))
            , () -> Tls.cast(FieldReferenceImpl.class, expr)
                .fap(fieldRef -> new FieldRes(ctx).resolve(fieldRef))
            , () -> som(new DeepType(expr))
        );
    }
}
