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
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.resolvers.FuncCallRes;
import org.klesun.deep_assoc_completion.resolvers.VarRes;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.MemoizingIterable;
import org.klesun.lang.Tls;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.klesun.lang.Lang.*;

// string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
// should suggest possible values of 'type'
public class VarNamePvdr extends CompletionProvider<CompletionParameters> implements GotoDeclarationHandler
{
    private static LookupElementBuilder makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(DeepKeysPvdr.getIcon())
            .withTypeText(type);
    }

    private static It<LookupElement> makeOptions(It<DeepType> tit)
    {
        return tit.fap(t -> t.keys)
            .fap(k -> k.keyType.getNames()).unq()
            .map(strVal -> makeLookupBase("$" + strVal, "string"))
            .map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, -1000 - i));
    }

    private static boolean isGlobalContext(Variable caretVar)
    {
        return caretVar.getParent() instanceof Global || !Tls.findParent(caretVar, Function.class, a -> true).has();
    }

    private static It<DeepType> resolveGlobalsMagicVar(IExprCtx funcCtx, Variable caretVar)
    {
        return funcCtx.getProject()
            .flt(proj -> isGlobalContext(caretVar))
            .map(proj -> PhpIndex.getInstance(proj))
            // does not include $GLOBAL usages in for loops and functions sadly
            .fap(idx -> It(idx.getVariablesByName("GLOBALS")))
            .fap(glob -> new VarRes(funcCtx).resolve(glob));
    }

    private It<DeepType> resolve(VariableImpl caretVar, boolean isAutoPopup, Editor editor)
    {
        SearchContext search = new SearchContext(caretVar.getProject())
            .setDepth(DeepKeysPvdr.getMaxDepth(isAutoPopup, editor.getProject()));
        if (isAutoPopup) {
            search.overrideMaxExpr = som(200);
        }
        FuncCtx funcCtx = new FuncCtx(search);
        IExprCtx exprCtx = new ExprCtx(funcCtx, caretVar, 0);

        return It.cnc(
            resolveGlobalsMagicVar(exprCtx, caretVar)
        );
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        // run remaining so that our completions were in the bottom of the list since
        // they are global hence least probably to be needed in a given context
        result.runRemainingContributors(parameters, true);
        It<DeepType> tit = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .fop(toCast(VariableImpl.class))
            .fap(lit -> resolve(lit, parameters.isAutoPopup(), parameters.getEditor()));

        makeOptions(tit).fch(lookupElement -> result.addElement(lookupElement));
        long elapsed = System.nanoTime() - startTime;
        double seconds = elapsed / 1000000000.0;
        if (seconds > 0.1) {
            System.out.println("resolved str values in " + seconds + " seconds");
        }
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
            .fop(toCast(VariableImpl.class))
            .fap(lit -> resolve(lit, false, editor)
                .fap(globt -> globt.keys)
                .flt(k -> k.keyType.getNames().any(n -> n.equals(lit.getName())))
                .map(t -> t.definition))
                .arr();

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
