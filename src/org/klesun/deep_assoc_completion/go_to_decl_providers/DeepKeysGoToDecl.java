package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
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
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.MethCallRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.*;

import java.util.*;

/**
 * go to declaration functionality for associative array keys
 */
public class DeepKeysGoToDecl extends Lang implements GotoDeclarationHandler
{
    private Opt<PsiElement> lastCaretPsi = non();
    private boolean lastFinished = true;

    private static PsiElement truncateOnLineBreak(PsiElement psi)
    {
        PsiElement truncated = psi.getFirstChild();
        while (psi.getText().contains("\n") && truncated != null) {
            psi = truncated;
            truncated = psi.getFirstChild();
        }
        return psi;
    }

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
                        .fop(arrt -> opt(arrt.keys.get(key)))
                        .map(k -> k.definition);
                }));
    }

    private static It<PsiElement> resolveMethCall(PsiElement psiElement, FuncCtx ctx)
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
                String fileText = prefix + doc.getText() + ";";
                PsiFile file = PsiFileFactory.getInstance(doc.getProject())
                    .createFileFromText(PhpLanguage.INSTANCE, fileText);
                int offset = mouseOffset - doc.getTextOffset() + prefix.length();

                FuncCtx ctx = new FuncCtx(new SearchContext(psiElement.getProject()));
                ctx.fakeFileSource = opt(doc);

                return opt(file.findElementAt(offset))
                    .fap(psi -> It.cnc(
                        resolveAssocKey(psi, ctx),
                        resolveMethCall(psi, ctx)
                    ));
            });
    }

    private static It<DeepType> resolveDocResult(PsiElement psiElement)
    {
        SearchContext search = new SearchContext(psiElement.getProject())
            .setDepth(DeepKeysPvdr.getMaxDepth(false, psiElement.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return opt(psiElement)
            .fop(v -> Tls.findParent(v, PhpDocParamTag.class, psi -> true))
            .fap(tag -> new DocParamRes(funcCtx).resolve(tag));
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement nullPsi, int mouseOffset, Editor editor)
    {
        Boolean prevFinished = lastFinished;
        lastFinished = false;
        Boolean isSecondAttempt = lastCaretPsi.map(last -> last.equals(nullPsi)).def(false);
        It<PsiElement> psiit = opt(nullPsi)
            .fap(psiElement -> {
                SearchContext search = new SearchContext(psiElement.getProject())
                    .setDepth(DeepKeysPvdr.getMaxDepth(false, psiElement.getProject()));
                FuncCtx funcCtx = new FuncCtx(search);
                It<PsiElement> it = It.frs(
                    () -> It.cnc(
                        resolveAssocKey(psiElement, funcCtx),
                        resolveDocAt(psiElement, mouseOffset)
                    ),
                    () -> resolveDocResult(psiElement)
                        .map(t -> t.definition)
                );
                return it.map(psi -> truncateOnLineBreak(psi))
                    .end((itpsi) -> !isSecondAttempt || !prevFinished)
                    .unq();
            });
        L<PsiElement> arr = psiit.arr();
        lastCaretPsi = opt(nullPsi);
        lastFinished = true;
        return arr.toArray(new PsiElement[arr.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        // dunno what this does
        return null;
    }
}
