package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.NewExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.TernaryExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringEscapeUtils;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.*;

public class MiscRes extends Lang
{
    final private FuncCtx ctx;

    public MiscRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private MultiType findPsiExprType(PsiElement psi)
    {
        return Tls.cast(PhpExpression.class, psi)
            .map(casted -> ctx.findExprType(casted))
            .def(new MultiType(L()));
    }

    private Opt<MultiType> resolveNew(NewExpression newExp)
    {
        return Opt.fst(
            () -> opt(newExp.getClassReference())
                .flt(ref -> ref.getText().equals("static"))
                .fop(ref -> Opt.fst(
                    () -> ctx.clsIdeaType,
                    () -> Tls.findParent(ref, PhpClass.class, a -> true)
                        .map(cls -> cls.getType())
                )),
            () -> opt(newExp)
                .flt(exp -> opt(exp.getClassReference())
                    .fap(ref -> L(ref.multiResolve(false)))
                    .has())
                .map(exp -> exp.getType())
        ).map(it -> {
            FuncCtx ctorArgs = ctx.subCtxDirect(newExp);
            return DeepType.makeNew(newExp, ctorArgs, it).mt();
        });
    }

    public Opt<Iterable<DeepType>> resolve(PsiElement expr)
    {
        return Opt.fst(() -> opt(null)
            , () -> Tls.cast(TernaryExpressionImpl.class, expr)
                .map(tern -> It.cnc(
                    findPsiExprType(tern.getTrueVariant()).types,
                    findPsiExprType(tern.getFalseVariant()).types
                ))
            , () -> Tls.cast(BinaryExpressionImpl.class, expr)
                .fop(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("??") || op.getText().equals("?:"))
                    .map(op -> It.cnc(
                        findPsiExprType(bin.getLeftOperand()).types,
                        findPsiExprType(bin.getRightOperand()).types
                    )))
            , () -> Tls.cast(BinaryExpressionImpl.class, expr)
                .fop(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("-")
                        || op.getText().equals("*") || op.getText().equals("/")
                        || op.getText().equals("%") || op.getText().equals("**")
                    )
                    .map(op -> {
                        DeepType type = new DeepType(bin, PhpType.NUMBER);
                        type.isNumber = true;
                        return list(type);
                    }))
            , () -> Tls.cast(BinaryExpressionImpl.class, expr)
                .fop(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("+"))
                    .map(op -> {
                        MultiType lmt = findPsiExprType(bin.getLeftOperand());
                        MultiType rmt = findPsiExprType(bin.getRightOperand());
                        L<DeepType> types = L();
                        It<DeepType> tit = It.cnc(lmt.types, rmt.types);
                        if (tit.any(t -> t.isNumber())) {
                            return list(new DeepType(bin, PhpType.NUMBER));
                        } else {
                            return tit;
                        }
                    }))
            , () -> Tls.cast(BinaryExpressionImpl.class, expr)
                .fop(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("."))
                    .map(op -> {
                        MultiType lmt = findPsiExprType(bin.getLeftOperand());
                        MultiType rmt = findPsiExprType(bin.getRightOperand());
                        String ccted = opt(lmt.getStringValue()).def("") + opt(rmt.getStringValue()).def("");
                        String unescaped = StringEscapeUtils.unescapeJava(ccted); // PHP ~ java
                        DeepType type = new DeepType(bin, PhpType.STRING, unescaped);
                        return list(type);
                    }))
            , () -> Tls.cast(NewExpressionImpl.class, expr)
                .fop(newExp -> resolveNew(newExp))
                .map(mt -> mt.types)
            , () -> Tls.cast(PhpExpression.class, expr)
                .map(t -> list(new DeepType(t)))
//            , Tls.cast(ConstantReferenceImpl.class, expr)
//                .map(cnst -> list(new DeepType(cnst)))
        );
    }
}
