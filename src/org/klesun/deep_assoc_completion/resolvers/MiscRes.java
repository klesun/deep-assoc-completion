package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.TernaryExpressionImpl;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public Opt<List<DeepType>> resolve(PsiElement expr)
    {
        return Opt.fst(list(opt(null)
            , Tls.cast(TernaryExpressionImpl.class, expr)
                .map(tern -> Stream.concat(
                    findPsiExprType(tern.getTrueVariant()).types.stream(),
                    findPsiExprType(tern.getFalseVariant()).types.stream()
                ).collect(Collectors.toList()))
            , Tls.cast(BinaryExpressionImpl.class, expr)
                // found this dealing with null coalescing, but
                // i suppose this rule will apply for all operators
                .map(bin -> Stream.concat(
                    findPsiExprType(bin.getLeftOperand()).types.stream(),
                    findPsiExprType(bin.getRightOperand()).types.stream()
                ).collect(Collectors.toList()))
            , Tls.cast(PhpExpression.class, expr)
                .map(t -> list(new DeepType(t)))
//            , Tls.cast(ConstantReferenceImpl.class, expr)
//                .map(cnst -> list(new DeepType(cnst)))
        ));
    }
}
