package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
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
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.MethCallRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import javax.swing.text.html.Option;
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

    private static L<PsiElement> resolveAssocKey(PsiElement psiElement, FuncCtx funcCtx)
    {
        return opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(PhpExpression.class))
            .fap(literal -> Lang.opt(literal.getParent())
                .fop(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fop(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fop(toCast(PhpExpression.class))
                .fap(srcExpr -> {
                    String key = funcCtx.findExprType(literal).getStringValue();
                    MultiType arrMt = funcCtx.findExprType(srcExpr);
                    return arrMt.types
                        .fop(arrt -> opt(arrt.keys.get(key)))
                        .map(k -> k.definition);
                }));
    }

    private static L<PsiElement> resolveMethCall(PsiElement psiElement, FuncCtx ctx)
    {
        return opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(MethodReference.class))
            .fap(call -> L(MethCallRes.resolveMethodsNoNs(call, ctx)))
            .map(a -> a);
    }

    private static L<PsiElement> resolveDocAt(PsiElement psiElement, int mouseOffset)
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
                    .fap(psi -> list(
                        resolveAssocKey(psi, ctx),
                        resolveMethCall(psi, ctx)
                    ).fap(a -> a));
            });
    }

    private static Opt<MultiType> resolveDocResult(PsiElement psiElement)
    {
        SearchContext search = new SearchContext(psiElement.getProject())
            .setDepth(DeepKeysPvdr.getMaxDepth(false, psiElement.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return opt(psiElement)
            .fop(v -> Tls.findParent(v, PhpDocParamTag.class, psi -> true))
            .fop(tag -> new DocParamRes(funcCtx).resolve(tag));
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement nullPsi, int mouseOffset, Editor editor)
    {
        L<PsiElement> result = opt(nullPsi)
            .fap(psiElement -> {
                L<PsiElement> psiTargets = L();
                SearchContext search = new SearchContext(psiElement.getProject())
                    .setDepth(DeepKeysPvdr.getMaxDepth(false, psiElement.getProject()));
                FuncCtx funcCtx = new FuncCtx(search);

                resolveAssocKey(psiElement, funcCtx)
                    .fch(psi -> psiTargets.add(psi));
                resolveDocAt(psiElement, mouseOffset)
                    .fch(psi -> psiTargets.add(psi));
                if (psiTargets.size() == 0) {
                    resolveDocResult(psiElement)
                        .map(mt -> mt.types)
                        .thn(types -> types.forEach(t -> psiTargets.add(t.definition)));
                }
                removeDuplicates(psiTargets);
                return psiTargets.map(psi -> truncateOnLineBreak(psi));
            });
        return result.toArray(new PsiElement[result.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        // dunno what this does
        return null;
    }
}
