package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.KeyUsageResolver;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.klesun.lang.Lang.*;

/**
 * provides completion of keys in `$someVar = ['' => 123]`
 * by looking at what keys are used in the `$someVar`
 * made first of all for cases when function takes an associative array and it
 * is a pain to collect key names one-by-one from the function implementation
 */
public class UsedKeysPvdr extends CompletionProvider<CompletionParameters> implements GotoDeclarationHandler
{
    private static LookupElement makeLookup(DeepType.Key keyEntry)
    {
        String type = keyEntry.getTypes().fst().map(t -> t.briefType.toString()).def("from usage");
        return LookupElementBuilder.create(keyEntry.name)
            .bold()
            .withIcon(DeepKeysPvdr.getIcon())
            .withTypeText(type);
    }

    private static Opt<ArrayCreationExpression> assertArrCtorKey(PsiElement caretPsi)
    {
        return opt(caretPsi.getParent())
            .fop(literal -> opt(literal.getParent())
                .map(arrKey -> arrKey.getParent())
                .fop(parent -> Opt.fst(
                    // assoc key
                    () -> Tls.cast(ArrayHashElementImpl.class, parent)
                        .flt(hash -> literal.isEquivalentTo(hash.getKey()))
                        .map(hash -> hash.getParent())
                        .fop(toCast(ArrayCreationExpression.class)),
                    // indexed value (that may become associative key as user continues typing)
                    () -> Tls.cast(ArrayCreationExpression.class, parent)
                )));
    }

    private Mt resolve(ArrayCreationExpression lit, boolean isAutoPopup, Editor editor)
    {
        SearchContext search = new SearchContext(lit.getProject())
            .setDepth(DeepKeysPvdr.getMaxDepth(isAutoPopup, editor.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return new KeyUsageResolver(funcCtx, 3).resolve(lit);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        It<DeepType.Key> usedKeys = assertArrCtorKey(parameters.getPosition())
            .fap(arrCtor -> {
                Set<String> alreadyDeclared = It(arrCtor.getHashElements())
                    .fop(el -> opt(el.getKey()))
                    .fop(toCast(StringLiteralExpressionImpl.class))
                    .map(lit -> lit.getContents()).wap(tit -> new HashSet<>(tit.arr()));
                return resolve(arrCtor, parameters.isAutoPopup(), parameters.getEditor())
                    .types.fap(t -> t.keys.values())
                    .flt(k -> !alreadyDeclared.contains(k.name));
            });

        usedKeys.map(k -> makeLookup(k))
            // to preserve order
            .map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, 3000 - i))
            .fch(result::addElement);

        long elapsed = System.nanoTime() - startTime;
        result.addLookupAdvertisement("Resolved used keys in " + (elapsed / 1000000000.0) + " seconds");
    }

    // ================================
    //  GotoDeclarationHandler part follows
    // ================================

    // just treating a symptom. i dunno why duplicates appear - they should not
    private static void removeDuplicates(List<PsiElement> psiTargets)
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

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor) {

        L<PsiElement> psiTargets = opt(psiElement)
            .map(psi -> psi.getParent())
            .fop(toCast(StringLiteralExpressionImpl.class))
            .fap(lit -> opt(lit.getFirstChild())
                .fop(c -> assertArrCtorKey(c))
                .fap(arrCtor -> resolve(arrCtor, false, editor)
                    .types.fap(t -> t.keys.values())
                    .flt(k -> lit.getContents().equals(k.name))
                    .map(k -> k.definition))).arr();

        removeDuplicates(psiTargets);

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        // renames the "Declaration" action if returned value is not null
        return null;
    }
}
