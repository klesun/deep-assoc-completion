package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Opt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.klesun.lang.Lang.*;

public class ArrayKeyExistsPvdr extends CompletionProvider<CompletionParameters> implements GotoDeclarationHandler
{
    private static LookupElement makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
                .bold()
                .withIcon(DeepKeysPvdr.getIcon())
                .withTypeText(type);
    }

    private static LookupElement makeLookup(DeepType.Key keyEntry)
    {
        String type = keyEntry.getTypes().gat(0).map(t -> t.briefType.toString()).def("unknown");
        return makeLookupBase(keyEntry.name, type);
    }

    private static L<LookupElement> makeOptions(MultiType mt)
    {
        L<LookupElement> result = L();
        Set<String> suggested = new HashSet<>();
        for (DeepType type: mt.types) {
            for (DeepType.Key keyEntry: type.keys.values()) {
                String key = keyEntry.name;
                if (suggested.contains(key)) continue;
                suggested.add(key);
                result.add(makeLookup(keyEntry));
            }
            L(type.getListElemTypes()).gat(0).flt(t -> type.keys.size() == 0).thn(t -> {
                for (int k = 0; k < 5; ++k) {
                    result.add(makeLookupBase(k + "", t.briefType.toString()));
                }
            });
        }
        return result;
    }

    private static Opt<MultiType> resolveArrayKeyExists(StringLiteralExpression literal, FuncCtx funcCtx)
    {
        return opt(literal.getParent())
            .map(argList -> argList.getParent())
            .fop(toCast(FunctionReferenceImpl.class))
            .flt(call -> list("array_key_exists", "key_exists").contains(call.getName()))
            .fop(call -> L(call.getParameters()).gat(1))
            .fop(toCast(PhpExpression.class))
            .map(arr -> funcCtx.findExprType(arr));
    }

    private MultiType resolve(StringLiteralExpression lit, boolean isAutoPopup, Editor editor)
    {
        SearchContext search = new SearchContext(lit.getProject())
            .setDepth(DeepKeysPvdr.getMaxDepth(isAutoPopup, editor.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return Opt.fst(
            () -> resolveArrayKeyExists(lit, funcCtx)
        ).def(MultiType.INVALID_PSI);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        MultiType mt = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .fop(toCast(StringLiteralExpression.class))
            .map(lit -> resolve(lit, parameters.isAutoPopup(), parameters.getEditor()))
            .def(MultiType.INVALID_PSI);

        makeOptions(mt).fch(result::addElement);
        long elapsed = System.nanoTime() - startTime;
        result.addLookupAdvertisement("Resolved in " + (elapsed / 1000000000.0) + " seconds");
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
            .map(lit -> resolve(lit, false, editor)
                .types.fap(t -> L(t.keys.values()))
                .flt(k -> lit.getContents().equals(k.name))
                .map(t -> t.definition))
            .def(list());

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
