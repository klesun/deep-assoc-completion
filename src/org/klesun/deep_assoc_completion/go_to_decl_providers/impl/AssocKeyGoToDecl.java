package org.klesun.deep_assoc_completion.go_to_decl_providers.impl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.completion_providers.AssocKeyPvdr;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.resolvers.MethCallRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.*;

import java.util.*;

/**
 * go to declaration functionality for associative array keys
 */
public class AssocKeyGoToDecl extends Lang
{
    private static It<PsiElement> resolveAssocKey(PsiElement psiElement, FuncCtx funcCtx)
    {
        return opt(psiElement)
            .fap(psi -> opt(psi.getParent()))
            .fop(toCast(PhpExpression.class))
            .fap(literal -> Lang.opt(literal.getParent())
                .fop(Lang.toCast(ArrayIndex.class))
                .fap(index -> opt(index.getParent()))
                .fop(Lang.toCast(ArrayAccessExpressionImpl.class))
                .fap(expr -> opt(expr.getValue()))
                .fop(toCast(PhpExpression.class))
                .fap(srcExpr -> {
                    String key = funcCtx.findExprType(literal).wap(Mt::getStringValueSt);
                    return funcCtx.findExprType(srcExpr)
                        .fap(arrt -> arrt.keys)
                        .fap(k -> k.keyType.getTypes())
                        .flt(t -> Objects.equals(t.stringValue, key))
                        .map(t -> t.definition)
                        .unq();
                }));
    }

    private static It<PsiElement> resolveMethCall(PsiElement psiElement, IExprCtx ctx)
    {
        return opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(MethodReference.class))
            .fap(call -> MethCallRes.resolveMethodsNoNs(call, ctx))
            .map(a -> a);
    }

    private static It<PsiElement> resolveDocAt(PsiElement psiElement, int mouseOffset)
    {
        String prefix = "<?php\n$arg ";
        return opt(psiElement)
            .map(psi -> psi.getParent())
            .flt(doc -> Tls.regex("^\\s*=\\s*(.+)$", doc.getText()).has() ||
                        doc.getParent() instanceof PhpDocReturnTag)
            .fap(doc -> {
                String fileText = DocParamRes.EXPR_PREFIX + doc.getText() + DocParamRes.EXPR_POSTFIX;
                PsiFile file = PsiFileFactory.getInstance(doc.getProject())
                    .createFileFromText(PhpLanguage.INSTANCE, fileText);
                int offset = mouseOffset - doc.getTextOffset() + prefix.length();

                FuncCtx ctx = new FuncCtx(new SearchCtx(psiElement.getProject()));
                ctx.fakeFileSource = opt(doc);
                IExprCtx exprCtx = new ExprCtx(ctx, psiElement, 0);

                return opt(file.findElementAt(offset))
                    .fap(psi -> It.cnc(
                        resolveAssocKey(psi, ctx),
                        resolveMethCall(psi, exprCtx)
                    ));
            });
    }

    private static It<DeepType> resolveDocResult(PsiElement psiElement)
    {
        SearchCtx search = new SearchCtx(psiElement.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(false, psiElement.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);
        IExprCtx exprCtx = new ExprCtx(funcCtx, psiElement, 0);

        return opt(psiElement)
            .fop(v -> Tls.findParent(v, PhpDocParamTag.class, psi -> true))
            .fap(tag -> new DocParamRes(exprCtx).resolve(tag));
    }

    public static It<PsiElement> resolveDeclPsis(@NotNull PsiElement psiElement, int mouseOffset, FuncCtx funcCtx)
    {
        return It.frs(
            () -> It.cnc(
                resolveAssocKey(psiElement, funcCtx)
                    .flt(declPsi -> !ScopeFinder.isPartOf(psiElement, declPsi)),
                resolveDocAt(psiElement, mouseOffset)
            ),
            () -> resolveDocResult(psiElement)
                .map(t -> t.definition)
        );
    }
}
