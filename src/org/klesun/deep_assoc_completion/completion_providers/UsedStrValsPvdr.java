package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.ForeachImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpPsiElementImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpType.PhpTypeBuilder;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.GuiUtil;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.UsageBasedTypeResolver;
import org.klesun.deep_assoc_completion.structures.*;
import org.klesun.lang.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.klesun.deep_assoc_completion.completion_providers.AssocKeyPvdr.BRIEF_VALUE_MAX_LEN;
import static org.klesun.deep_assoc_completion.completion_providers.AssocKeyPvdr.COMMENTED_MAX_LEN;
import static org.klesun.lang.Lang.*;

// string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
// should suggest possible values of 'type'
public class UsedStrValsPvdr extends CompletionProvider<CompletionParameters>
{
    /**
     * to show above built-in options, at same time it must be in the
     * order of resolution, but constant suggestions must always go first
     */
    private static class BasePriorityOption
    {
        final public LookupElement lookup;
        final public int basePriority;

        public BasePriorityOption(LookupElement lookup, int basePriority)
        {
            this.lookup = lookup;
            this.basePriority = basePriority;
        }
    }

    public static LookupElementBuilder makeLookupBase(String keyName, L<DeepType> valtarr, String comment)
    {
        int maxValLen = comment.length() > 0 ? COMMENTED_MAX_LEN : BRIEF_VALUE_MAX_LEN;

        String typeStr = valtarr.size() < 1 ? "from usage" :
            valtarr.fop(t -> opt(t.briefType)).wap(pstit -> {
                PhpTypeBuilder pstBuilder = PhpType.builder();
                pstit.forEach(pstBuilder::add);
                return pstBuilder.build().toString();
            });
        LookupElementBuilder lookup = LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(typeStr);
        if ("int".equals(typeStr)) {
            lookup = lookup.withInsertHandler(GuiUtil.toRemoveIntStrQuotes());
        }
        String valStr = new Mt(valtarr).getBriefValueText(maxValLen);
        valStr = Tls.substr(valStr, 0, BRIEF_VALUE_MAX_LEN);
        if (comment.length() > 0) {
            valStr = valStr.length() > 0
                ? " = " + Tls.substr(valStr, 0, 20) + " // " + comment
                : " " + comment;
        } else if (valStr.length() > 0) {
            valStr = " = " + valStr;
        }
        if (valStr.length() > 0) {
            lookup = lookup.withTailText(valStr, true);
        }
        return lookup;
    }

    private static void placeCaretAfterStrLit(InsertionContext ctx, LookupElement lookup)
    {
        ctx.getEditor().getCaretModel().moveToOffset(ctx.getSelectionEndOffset() + 1);
    }

    private static It<LookupElement> makeOptions(It<DeepType> assocTit)
    {
        return assocTit
            .fap((assoct, i) -> assoct.keys.fap(keyObj -> {
                L<DeepType> valtarr = keyObj.getGrantedValues();
                return keyObj.keyType.types.fap((t) -> It.cnc(
                    opt(t.stringValue)
                        .flt(strVal -> !t.cstName.has() || !t.isNumber)
                        .map(strVal -> makeLookupBase(strVal, valtarr, Tls.implode(" ", keyObj.comments).trim())
                            .withInsertHandler(UsedStrValsPvdr::placeCaretAfterStrLit))
                        .map((lookup) -> new BasePriorityOption(lookup, 10000)),
                    t.cstName
                        .map(cstName -> makeLookupBase(cstName, list(t), "")
                            .withTailText(opt(t.stringValue).map(strVal -> " = " + strVal).def(""), true)
                            .withInsertHandler(GuiUtil.toAlwaysRemoveQuotes()))
                        .map((lookup) -> new BasePriorityOption(lookup, 15000))
                ));
            }))
            .map((prio, i) -> PrioritizedLookupElement.withPriority(prio.lookup, prio.basePriority - i * 100))
            .unq(LookupElement::getLookupString);
    }

    /** @return - the other operand */
    public static Opt<PhpExpression> assertEqOperand(PhpExpression lit)
    {
        return opt(lit)
            .map(PsiElement::getParent) // BinaryExpressionImpl
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
        return assertEqOperand(lit).fap(funcCtx::findExprType);
    }

    private static It<DeepType> resolveKeyArrCtor(StringLiteralExpression lit, IExprCtx funcCtx)
    {
        return opt(lit.getParent())
            .cst(PhpPsiElementImpl.class)
            .fop(psi -> opt(psi.getParent()))
            .cst(ArrayHashElement.class)
            .flt(hash -> lit.equals(hash.getKey()))
            .fop(psi -> opt(psi.getParent()))
            .cst(ArrayCreationExpression.class)
            .fap(arrCtor -> new UsageBasedTypeResolver(funcCtx).resolve(arrCtor));
    }

    private static It<DeepType> resolveUsedValues(StringLiteralExpression lit, IExprCtx funcCtx)
    {
        return new UsageBasedTypeResolver(funcCtx).resolve(lit);
    }

    /*
     * moving all these built-in function resolutions inside UsageBasedTypeResolver.java
     * would be cooler, as it would also cover cases when they are called deeper
     * in some function, or when you put the value in a var
     */

    /** in_array($type, ['']) */
    private static It<DeepType> resolveInArrayHaystack(StringLiteralExpression lit, IExprCtx funcCtx)
    {
        return opt(lit)
            .map(PsiElement::getParent) // array value
            .map(PsiElement::getParent)
            .fop(toCast(ArrayCreationExpression.class))
            .fap(arr -> opt(arr.getParent())
                .fop(toCast(ParameterList.class))
                .flt(lst -> L(lst.getParameters()).gat(1)
                    .flt(arg -> arg.isEquivalentTo(arr)).has()
                )
                .flt(lst -> opt(lst.getParent())
                    .fop(toCast(FunctionReference.class))
                    .map(PhpReference::getName)
                    .flt(nme -> nme.equals("in_array")).has())
                .fop(lst -> L(lst.getParameters()).gat(0))
                .fop(toCast(PhpExpression.class))
                .fap(funcCtx::findExprType));
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
                .map(PhpReference::getName)
                .flt(nme -> nme.equals("in_array")).has())
            .fop(lst -> L(lst.getParameters()).gat(1))
            .fop(toCast(PhpExpression.class))
            .fap(funcCtx::findExprType)
            .fap(Mt::getElSt);
    }

    // array_intersect($cmdTypes, ['redisplayPnr', 'itinerary', 'airItinerary', 'storeKeepPnr', 'changeArea', ''])
    private static It<DeepType> resolveArrayIntersect(StringLiteralExpression lit, IExprCtx funcCtx)
    {
        return opt(lit)
            .map(PsiElement::getParent) // array value
            .map(PsiElement::getParent)
            .fop(toCast(ArrayCreationExpression.class))
            .fap(arr -> opt(arr.getParent())
                .fop(toCast(ParameterList.class))
                .flt(lst -> opt(lst.getParent())
                    .fop(toCast(FunctionReference.class))
                    .map(PhpReference::getName)
                    .flt(nme -> nme.equals("array_intersect")).has())
                .fap(lst -> L(lst.getParameters()))
                .flt(par -> !arr.isEquivalentTo(par))
                .fop(toCast(PhpExpression.class))
                .fap(funcCtx::findExprType)
                .fap(Mt::getElSt));
    }

    private static It<DeepType> resolveForeachListKey(StringLiteralExpression lit, IExprCtx funcCtx)
    {
        boolean isKeyInList = opt(lit.getNextSibling())
            .fop(sib -> sib instanceof PsiWhiteSpace ? opt(sib.getNextSibling()) : som(sib))
            .any(sib -> sib.getText().equals("=>"));
        if (!isKeyInList) {
            return It.non();
        }
        return opt(lit.getParent())
            .cst(ForeachImpl.class)
            .fap(fch -> opt(fch.getArray()))
            .cst(PhpExpression.class)
            .fap(funcCtx::findExprType)
            .fap(Mt::getElSt)
            .fap(elt -> elt.keys.fap(k -> k.keyType.types));
    }

    /** @return - association: requested string value type as key and related value, if applicable, as value */
    public static It<DeepType> resolve(StringLiteralExpression lit, boolean isAutoPopup)
    {
        SearchCtx search = new SearchCtx(lit.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(isAutoPopup, lit.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);
        IExprCtx exprCtx = new ExprCtx(funcCtx, lit, 0);

        return It.frs(
            () -> resolveKeyArrCtor(lit, exprCtx),
            () -> {
                It<DeepType> strts = It.cnc(
                    resolveEqExpr(lit, exprCtx),
                    resolveForeachListKey(lit, exprCtx),
                    resolveUsedValues(lit, exprCtx),
                    resolveInArrayHaystack(lit, exprCtx),
                    resolveInArrayNeedle(lit, exprCtx),
                    resolveArrayIntersect(lit, exprCtx)
                );
                return strts.map(strt -> new Build(lit, PhpType.UNSET).keys(list(
                    new Key(KeyType.mt(som(strt), strt.definition))
                )).get());
            }
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

        Opt<StringLiteralExpression> litOpt = opt(parameters.getPosition().getParent())
            .fop(toCast(StringLiteralExpression.class));
        if (!litOpt.has()) {
            return;
        }
        StringLiteralExpression lit = litOpt.unw();

        long startTime = System.nanoTime();
        It<DeepType> assocTit = resolve(lit, parameters.isAutoPopup());

        makeOptions(assocTit)
            .flt(our -> !alreadySuggested.contains(our.getLookupString()))
            .forEach(result::addElement);
        long elapsed = System.nanoTime() - startTime;
        double seconds = elapsed / 1000000000.0;
        if (seconds > 0.1) {
            System.out.println("resolved str values in " + seconds + " seconds");
        }
    }
}
