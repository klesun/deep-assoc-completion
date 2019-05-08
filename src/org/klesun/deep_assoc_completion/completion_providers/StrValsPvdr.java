package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.GuiUtil;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.UsageResolver;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.Opt;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.klesun.lang.Lang.*;

// string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
// should suggest possible values of 'type'
public class StrValsPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElementBuilder makeLookupBase(String keyName, String type)
    {
        LookupElementBuilder lookup = LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(type);
        if ("int".equals(type)) {
            lookup = lookup.withInsertHandler(GuiUtil.toRemoveIntStrQuotes());
        }
        return lookup;
    }

    private static It<LookupElement> makeOptions(It<DeepType> tit)
    {
        return tit.unq(t -> t.stringValue).fap((t, i) -> It.cnc(
            opt(t.stringValue)
                .flt(strVal -> !t.cstName.has() || !t.isNumber)
                .map(strVal -> makeLookupBase(strVal, t.briefType + ""))
                .map((lookup) -> PrioritizedLookupElement.withPriority(lookup, 100 - i)),
            t.cstName
                .map(cstName -> makeLookupBase(cstName, t.briefType + "")
                    .withTailText(opt(t.stringValue).map(strVal -> " = " + strVal).def(""), true)
                    .withInsertHandler(GuiUtil.toAlwaysRemoveQuotes()))
                .map((lookup) -> PrioritizedLookupElement.withPriority(lookup, 100 - i + 50))
        ));
    }

    /** @return - the other operand */
    public static Opt<PhpExpression> assertEqOperand(PhpExpression lit)
    {
        return opt(lit)
            .map(literal -> literal.getParent()) // BinaryExpressionImpl
            .fop(toCast(BinaryExpressionImpl.class))
            .fap(bin -> opt(bin.getOperation())
                .flt(op -> op.getText().equals("==") || op.getText().equals("===")
                        || op.getText().equals("!=") || op.getText().equals("!=="))
                .fap(op -> list(bin.getLeftOperand(), bin.getRightOperand())))
            .cst(PhpExpression.class)
            .flt(op -> !Objects.equals(op, lit))
            .fst();
    }

    /** $type === '' */
    private static It<DeepType> resolveEqExpr(StringLiteralExpression lit, IExprCtx funcCtx)
    {
        return assertEqOperand(lit).fap(exp -> funcCtx.findExprType(exp));
    }

    private static It<DeepType> resolveUsedValues(StringLiteralExpression lit, IExprCtx funcCtx)
    {
        return new UsageResolver(funcCtx).findExprTypeFromUsage(lit);
    }

    /** in_array($type, ['']) */
    private static It<DeepType> resolveInArrayHaystack(StringLiteralExpression lit, IExprCtx funcCtx)
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
    private static It<DeepType> resolveInArrayNeedle(StringLiteralExpression lit, IExprCtx funcCtx)
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
    private static It<DeepType> resolveArrayIntersect(StringLiteralExpression lit, IExprCtx funcCtx)
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

    public static It<DeepType> resolve(StringLiteralExpression lit, boolean isAutoPopup)
    {
        SearchCtx search = new SearchCtx(lit.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(isAutoPopup, lit.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);
        IExprCtx exprCtx = new ExprCtx(funcCtx, lit, 0);

        return It.cnc(
            resolveEqExpr(lit, exprCtx),
            resolveUsedValues(lit, exprCtx),
            resolveInArrayHaystack(lit, exprCtx),
            resolveInArrayNeedle(lit, exprCtx),
            resolveArrayIntersect(lit, exprCtx)
        );
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        Set<String> alreadySuggested = new HashSet<>();
        // Symfony string completion options are usually more
        // relevant, so should make sure they are shown instantly
        result.runRemainingContributors(parameters, otherSourceResult -> {
            result.passResult(otherSourceResult);
            alreadySuggested.add(otherSourceResult.getLookupElement().getLookupString());
        });


        long startTime = System.nanoTime();
        It<DeepType> tit = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .fop(toCast(StringLiteralExpression.class))
            .fap(lit -> resolve(lit, parameters.isAutoPopup()));

        makeOptions(tit)
            .flt(our -> !alreadySuggested.contains(our.getLookupString()))
            .fch(result::addElement);
        long elapsed = System.nanoTime() - startTime;
        double seconds = elapsed / 1000000000.0;
        if (seconds > 0.1) {
            System.out.println("resolved str values in " + seconds + " seconds");
        }
    }
}
