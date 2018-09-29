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
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.klesun.lang.Lang.*;

// string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
// should suggest possible values of 'type'
public class EqStrValsPvdr extends CompletionProvider<CompletionParameters> implements GotoDeclarationHandler
{
    private static LookupElement makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
                .bold()
                .withIcon(DeepKeysPvdr.getIcon())
                .withTypeText(type);
    }

    private static It<LookupElement> makeOptions(It<DeepType> tit)
    {
        return tit.fop(t -> opt(t.stringValue)).unq().map(strVal -> makeLookupBase(strVal, "string"));
    }

    /** $type === '' */
    private static It<DeepType> resolveEqExpr(StringLiteralExpression lit, FuncCtx funcCtx)
    {
        return opt(lit)
            .map(literal -> literal.getParent()) // BinaryExpressionImpl
            .fop(toCast(BinaryExpressionImpl.class))
            .fap(bin -> opt(bin.getOperation())
                .flt(op -> op.getText().equals("==") || op.getText().equals("===")
                        || op.getText().equals("!=") || op.getText().equals("!=="))
                .map(op -> bin.getLeftOperand())
                .fop(toCast(PhpExpression.class))
                .fap(exp -> funcCtx.findExprType(exp)))
            ;
    }

    /** in_array($type, ['']) */
    private static It<DeepType> resolveInArrayHaystack(StringLiteralExpression lit, FuncCtx funcCtx)
    {
        return opt(lit)
            .map(literal -> literal.getParent()) // array value
            .map(literal -> literal.getParent())
            .fop(toCast(ArrayCreationExpression.class))
            .fap(arr -> opt(arr.getParent())
                .fop(toCast(ParameterList.class))
                .flt(lst -> L(lst.getParameters()).gat(1)
                    .flt(arg -> arg.isEquivalentTo(arr)).has()
                )
                .flt(lst -> opt(lst.getParent())
                    .fop(toCast(FunctionReference.class))
                    .map(fun -> fun.getName())
                    .flt(nme -> nme.equals("in_array")).has())
                .fop(lst -> L(lst.getParameters()).gat(0))
                .fop(toCast(PhpExpression.class))
                .fap(str -> funcCtx.findExprType(str)));
    }

    /** in_array('', $types) */
    private static It<DeepType> resolveInArrayNeedle(StringLiteralExpression lit, FuncCtx funcCtx)
    {
        return opt(lit.getParent())
            .fop(toCast(ParameterList.class))
            .flt(lst -> L(lst.getParameters()).gat(0)
                .flt(arg -> arg.isEquivalentTo(lit)).has())
            .flt(lst -> opt(lst.getParent())
                .fop(toCast(FunctionReference.class))
                .map(fun -> fun.getName())
                .flt(nme -> nme.equals("in_array")).has())
            .fop(lst -> L(lst.getParameters()).gat(1))
            .fop(toCast(PhpExpression.class))
            .fap(str -> funcCtx.findExprType(str))
            .fap(t -> Mt.getElSt(t));
    }

    // array_intersect($cmdTypes, ['redisplayPnr', 'itinerary', 'airItinerary', 'storeKeepPnr', 'changeArea', ''])
    private static It<DeepType> resolveArrayIntersect(StringLiteralExpression lit, FuncCtx funcCtx)
    {
        return opt(lit)
            .map(literal -> literal.getParent()) // array value
            .map(literal -> literal.getParent())
            .fop(toCast(ArrayCreationExpression.class))
            .fap(arr -> opt(arr.getParent())
                .fop(toCast(ParameterList.class))
                .flt(lst -> opt(lst.getParent())
                    .fop(toCast(FunctionReference.class))
                    .map(fun -> fun.getName())
                    .flt(nme -> nme.equals("array_intersect")).has())
                .fap(lst -> L(lst.getParameters()))
                .flt(par -> !arr.isEquivalentTo(par))
                .fop(toCast(PhpExpression.class))
                .fap(str -> funcCtx.findExprType(str))
                .fap(t -> Mt.getElSt(t)));
    }

    private It<DeepType> resolve(StringLiteralExpression lit, boolean isAutoPopup, Editor editor)
    {
        SearchContext search = new SearchContext(lit.getProject())
            .setDepth(DeepKeysPvdr.getMaxDepth(isAutoPopup, editor.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return It.cnc(
            resolveEqExpr(lit, funcCtx),
            resolveInArrayHaystack(lit, funcCtx),
            resolveInArrayNeedle(lit, funcCtx),
            resolveArrayIntersect(lit, funcCtx)
        );
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        It<DeepType> tit = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .fop(toCast(StringLiteralExpression.class))
            .fap(lit -> resolve(lit, parameters.isAutoPopup(), parameters.getEditor()));

        makeOptions(tit).fch(result::addElement);
        long elapsed = System.nanoTime() - startTime;
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
            .fap(lit -> resolve(lit, false, editor)
                .flt(t -> lit.getContents().equals(t.stringValue))
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
