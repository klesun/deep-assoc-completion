package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.MethCallRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.awt.event.MouseEvent;
import java.util.*;

/**
 * go to declaration functionality for associative array keys
 */
public class DeepKeysGoToDecl extends Lang implements GotoDeclarationHandler
{
    private static PsiElement truncateOnLineBreak(PsiElement psi)
    {
        PsiElement truncated = psi.getFirstChild();
        while (psi.getText().contains("\n") && truncated != null) {
            psi = truncated;
            truncated = psi.getFirstChild();
        }
        return psi;
    }

    // just treating a symptom. i dunno why duplicates appear - they should not
    private static void removeDuplicates(L<PsiElement> psiTargets)
    {
        Set<PsiElement> fingerprints = new HashSet<>();
        int size = psiTargets.size();
        for (int k = size - 1; k >= 0; --k) {
            if (fingerprints.contains(psiTargets.get(k))) {
                psiTargets.remove(k);
            }
            fingerprints.add(psiTargets.get(k));
        }
    }

    private static L<PsiElement> resolveAssocKey(PsiElement psiElement)
    {
        SearchContext search = new SearchContext()
            .setDepth(DeepKeysPvdr.getMaxDepth(false));
        FuncCtx funcCtx = new FuncCtx(search);

        return opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(PhpExpression.class))
            .fap(literal -> Lang.opt(literal.getParent())
                .fop(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fop(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fop(toCast(PhpExpression.class))
                .map(srcExpr -> funcCtx.findExprType(srcExpr))
                .fap(arrMt -> {
                    String key = funcCtx.findExprType(literal).getStringValue();
                    return arrMt.types
                        .fop(arrt -> opt(arrt.keys.get(key)))
                        .map(k -> k.definition);
                }));
    }

    private static L<PsiElement> resolveMethCall(PsiElement psiElement)
    {
        return opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(MethodReference.class))
            .fap(call -> {
                String cls = opt(call.getClassReference()).map(c -> c.getName()).def("");
                String mth = opt(call.getName()).def("");
                return L(MethCallRes.resolveMethodsNoNs(cls, mth, call.getProject()));
            })
            .map(a -> a);
    }

    private static L<PsiElement> resolveDocAt(PsiElement psiElement, int mouseOffset)
    {
        String prefix = "<?php\n$arg ";
        return opt(psiElement)
            .map(psi -> psi.getParent())
            .flt(doc -> Tls.regex("^\\s*=\\s*(.+)$", doc.getText()).has())
            .fap(doc -> {
                String fileText = prefix + doc.getText() + ";";
                PsiFile file = PsiFileFactory.getInstance(doc.getProject())
                    .createFileFromText(PhpLanguage.INSTANCE, fileText);
                int offset = mouseOffset - doc.getTextOffset() + prefix.length();
                return opt(file.findElementAt(offset))
                    .fap(psi -> list(
                        resolveAssocKey(psi),
                        resolveMethCall(psi)
                    ).fap(a -> a));
            });
    }

    private static Opt<MultiType> resolveDocResult(PsiElement psiElement)
    {
        SearchContext search = new SearchContext()
            .setDepth(DeepKeysPvdr.getMaxDepth(false));
        FuncCtx funcCtx = new FuncCtx(search);

        return opt(psiElement)
            .fop(v -> Tls.findParent(v, PhpDocParamTag.class, psi -> true))
            .fop(tag -> new DocParamRes(funcCtx).resolve(tag));
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int mouseOffset, Editor editor)
    {
        L<PsiElement> psiTargets = L();
        resolveAssocKey(psiElement)
            .fch(psi -> psiTargets.add(psi));
        resolveDocAt(psiElement, mouseOffset)
            .fch(psi -> psiTargets.add(psi));
        if (psiTargets.size() == 0) {
            resolveDocResult(psiElement)
                .map(mt -> mt.types)
                .thn(types -> types.forEach(t -> psiTargets.add(t.definition)));
        }
        removeDuplicates(psiTargets);
        return psiTargets
            .map(psi -> truncateOnLineBreak(psi))
            .toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        // dunno what this does
        return null;
    }
}
