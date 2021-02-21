package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpPsiElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.icons.DeepIcons;
import org.klesun.deep_assoc_completion.structures.Key;
import org.klesun.lang.*;
import org.klesun.lib.PhpToolbox;

import javax.swing.*;
import java.net.URL;
import java.util.*;

import static org.klesun.deep_assoc_completion.helpers.GuiUtil.putCaretForward;
import static org.klesun.deep_assoc_completion.helpers.GuiUtil.runSafeRemainingContributors;
import static org.klesun.lang.Lang.*;

/**
 * $arr[''];
 */
public class AssocKeyPvdr extends CompletionProvider<CompletionParameters>
{
    final public static int BRIEF_VALUE_MAX_LEN = 65;
    final public static int COMMENTED_MAX_LEN = 110;

    private static ImageIcon icon = null;

    public static ImageIcon getIcon()
    {
        if (icon == null) {
            URL path = DeepIcons.class.getResource("deep_16_ruby2.png");
            icon = new ImageIcon(path);
        }
        return icon;
    }

    public static int getMaxDepth(boolean isAutoPopup, @Nullable Project project)
    {
        if (project != null) {
            DeepSettings settings = DeepSettings.inst(project);
            return isAutoPopup ? settings.implicitDepthLimit : settings.explicitDepthLimit;
        } else {
            return isAutoPopup ? 30 : 40;
        }
    }

    public static int getMaxDepth(CompletionParameters parameters)
    {
        return getMaxDepth(parameters.isAutoPopup(), parameters.getEditor().getProject());
    }

    private static String prepareTailText(int keyLength, String briefVal, Opt<String> commentOpt) {
        int maxValLen = commentOpt.has() ? COMMENTED_MAX_LEN : BRIEF_VALUE_MAX_LEN;

        // (keyName + briefVal) length must be constant for all keys, or you'll
        // get nasty broken position of type when you highlight an option
        briefVal = briefVal.trim().equals("") ? "" : " = " + briefVal;
        if (commentOpt.has()) {
            briefVal = Tls.substr(briefVal, 0, 20) + "… " + commentOpt.unw();
        }
        return Tls.substr(briefVal, 0, maxValLen);
    }

    private static LookupElementBuilder startLookupBuilder(String keyName) {
        return LookupElementBuilder.create(keyName)
            .withIcon(getIcon());
    }

    public static LookupElementBuilder makePaddedLookup(
        String keyName,
        String ideaType,
        String briefVal,
        Opt<String> commentOpt
    ) {
        ideaType = !ideaType.equals("") ? ideaType : "?";
        briefVal = prepareTailText(keyName.length(), briefVal, commentOpt);

        return startLookupBuilder(keyName)
            .withTailText(briefVal, true)
            .withTypeText(ideaType, false);
    }

    public static IIt<DeepType> resolveAtPsi(PsiElement caretPsi, IExprCtx funcCtx)
    {
        return opt(caretPsi.getParent())
            .map(litRaw -> litRaw.getParent())
            .fop(toCast(ArrayIndex.class))
            .map(index -> index.getParent())
            .fop(toCast(ArrayAccessExpression.class))
            .map(expr -> expr.getValue())
            .fop(toCast(PhpExpression.class)).arr()
            .rap(srcExpr -> funcCtx.findExprType(srcExpr));
    }

    /** see https://github.com/klesun/deep-assoc-completion/issues/131 */
    private static It<F<LookupElementBuilder, LookupElementBuilder>> assertMetaComment(String comment, ExprCtx ctx) {
        L<T2<String, F2<LookupElementBuilder, String, LookupElementBuilder>>> mappers = list(
            T2("type_text", (lookup, value) -> lookup.withTypeText(value)),
            T2("icon", (lookup, value) -> {
                try {
                    Icon icon = PhpToolbox.getLookupIconOnString(value);
                    if (icon != null) {
                        lookup = lookup.withIcon(icon);
                    }
                } catch (Throwable exc) {
                    exc.printStackTrace();
                }
                return lookup;
            }),
            //T2("target", (lookup, value) -> lookup.withTarget(value)),
            T2("tail_text", (lookup, value) -> lookup.withTailText(value))
        );
        return DocParamRes.parseExpression(comment, ctx.expr.getProject(), ctx)
            .fap(t -> mappers
                .fap(tup -> tup.nme((key, mapper) -> t.mt()
                    .getKey(key)
                    .getStringValues()
                    .flt(str -> !str.equals(""))
                    .map(value -> {
                        F<LookupElementBuilder, LookupElementBuilder> updater =
                            (lookup) -> mapper.apply(lookup, value);
                        return updater;
                    }))));
    }

    private static void printExprTree(ExprCtx root, SearchCtx search, int depth)
    {
        String indent = Tls.range(0, depth).rdc((sum,i) -> sum + " ", "");
        int typeCnt = root.typeCnt.def(0);
        if (search.currentExpr.equals(som(root))) {
            System.out.println("                                 ====== current expression =======================================");
        }
        if (root.parent.any(p -> p.expr == root.expr)) {
            // a fake expr ctx created for func call resolution
        } else {
            System.out.println(indent + SearchCtx.formatPsi(root.expr) + " " + typeCnt + " types " + (typeCnt > 100 ? "many yopta" : ""));
        }
        for (ExprCtx subCtx: root.children) {
            printExprTree(subCtx, search, depth + 1);
        }
    }

    private Opt<PsiElement> tryTakeValuePsi(PsiElement keyPsi)
    {
        return Opt.fst(Lang::non
            , () -> Tls.cast(ArrayHashElement.class, keyPsi)
                .fop(hashEl -> opt(hashEl.getValue()))
            , () -> Tls.cast(StringLiteralExpression.class, keyPsi)
                .fop(lit -> opt(lit.getParent()))
                .cst(ArrayIndex.class)
                .fop(lit -> opt(lit.getParent()))
                .cst(ArrayAccessExpression.class)
                .fop(acc -> opt(acc.getParent())
                    .cst(AssignmentExpression.class)
                    .flt(ass -> acc.equals(ass.getVariable())))
                .fop(ass -> opt(ass.getValue()))
            , () -> Tls.cast(StringLiteralExpression.class, keyPsi)
                .fop(lit -> opt(lit.getParent())
                    .cst(PhpPsiElementImpl.class)
                    .fop(el -> opt(el.getParent())
                    .cst(ArrayHashElement.class)
                    .flt(hashEl -> lit.equals(hashEl.getKey()))))
                .fop(hashEl -> opt(hashEl.getValue()))
        );
    }

    /**
     * a tradeoff after idea removed possibility to update tail text in completion option after it was
     * shown - show type at once if it is already resolved (direct keys definition, phpdoc, etc...)
     */
    private String getGrantedBriefValue(Key key)
    {
        IIt<String> granteds = key.getGrantedValues()
            .map(t -> t.getBriefVal(false).def("?"));
        if (granteds.has()) {
            Set<String> typeStringsSet = new LinkedHashSet<>(granteds.arr());
            return substr(It(typeStringsSet).str("|"), 0, BRIEF_VALUE_MAX_LEN);
        } else {
            return tryTakeValuePsi(key.definition)
                .map(valPsi -> valPsi.getText())
                .map(valStr -> valStr.replaceAll("\\s{2,}", ""))
                .map(valStr -> substr(valStr, 0, BRIEF_VALUE_MAX_LEN))
                .def("");
        }
    }

    private IIt<LookupKey> makeSuggestibleNames(DeepType kt, boolean isCaretInsideQuotes) {
        L<LookupKey> keyNamesToAdd = list();
        if (kt.stringValue != null) {
            String keyName = kt.stringValue;
            String lookupString = isCaretInsideQuotes || Tls.isNum(keyName) ? keyName : "'" + keyName + "'";
            LookupElementBuilder keyLookup = startLookupBuilder(lookupString)
                .withBoldness(true);
            keyNamesToAdd.add(new LookupKey(keyLookup, LookupKeyKind.NAME));
        } else {
            IIt<LookupKey> varNameLookups = opt(kt.definition)
                .fop(psi -> Opt.fst(Tls::non
                    , () -> Tls.cast(Variable.class, psi)
                    , () -> Tls.cast(ArrayHashElement.class, psi)
                        .fop(h -> opt(h.getKey()))
                        .cst(Variable.class)
                ))
                .map(PhpNamedElement::getName)
                .map(varName -> {
                    LookupElementBuilder varLookup = startLookupBuilder("$" + varName);
                    return new LookupKey(varLookup, LookupKeyKind.VAR);
                });
            varNameLookups.forEach(keyNamesToAdd::add);
            for (int n = 0; n < 3; ++n) {
                LookupElementBuilder keyLookup = startLookupBuilder(n + "");
                keyNamesToAdd.add(new LookupKey(keyLookup, LookupKeyKind.INDEX));
            }
        }
        return keyNamesToAdd.map(lk -> {
            LookupElementBuilder keyLookup = lk.lookup
                .withInsertHandler((ctx, lookup) -> {
                    putCaretForward(ctx, isCaretInsideQuotes);
                    if (lk.kind == LookupKeyKind.NAME) {
                        GuiUtil.removeIntStrQuotes(ctx, lookup);
                    } else { // var, index
                        GuiUtil.removeQuotes(ctx, lookup);
                    }
                });
            return new LookupKey(keyLookup, lk.kind);
        });
    }

    private T2<Set<String>, Boolean> addNameOnly(
        Mt arrMt,
        CompletionResultSet result,
        ExprCtx exprCtx,
        boolean isCaretInsideQuotes,
        C<String> onFirst
    ) {
        Set<String> suggested = new LinkedHashSet<>();
        Mutable<Boolean> isFirst = new Mutable<>(true);
        Mutable<Boolean> hadComments = new Mutable<>(false);

        arrMt.types.fap(t -> t.keys).fch((keyEntry, i) -> {
            keyEntry.keyType.getTypes().itr().fch((kt,j) -> {
                IIt<LookupKey> newKeyNamesToAdd = makeSuggestibleNames(kt, isCaretInsideQuotes)
                    .flt(lk -> !suggested.contains(lk.lookup.getLookupString()));

                String comment = Tls.implode(" ", keyEntry.comments).trim();
                Opt<String> commentOpt = comment.trim().equals("") ? non() : som(comment);
                L<F<LookupElementBuilder, LookupElementBuilder>> metCommentOpt = commentOpt
                    .fap(c -> assertMetaComment(c, exprCtx)).arr();
                if (metCommentOpt.has()) {
                    commentOpt = non();
                }
                if (commentOpt.has()) {
                    hadComments.set(true);
                }
                String briefTypeRaw = Mt.getKeyBriefTypeSt(keyEntry.getBriefTypes())
                    .filterUnknown().filterMixed().toStringResolved();

                for (LookupKey lookupKey: newKeyNamesToAdd) {
                    if (isFirst.get()) {
                        isFirst.set(false);
                        onFirst.accept(lookupKey.lookup.getLookupString());
                    }

                    String tailText = prepareTailText(
                        lookupKey.lookup.getLookupString().length(),
                        getGrantedBriefValue(keyEntry), commentOpt
                    );
                    LookupElementBuilder lookup = lookupKey.lookup
                        .withTailText(tailText, true)
                        .withTypeText(!briefTypeRaw.equals("") ? briefTypeRaw : "?", false);

                    for (F<LookupElementBuilder, LookupElementBuilder> updater: metCommentOpt) {
                        lookup = updater.apply(lookup);
                    }

                    int basePriority = lookupKey.kind == LookupKeyKind.NAME ? 2500 : 2000;
                    int priority = basePriority - suggested.size();
                    LookupElement prio = PrioritizedLookupElement
                        .withPriority(lookup, priority);

                    result.addElement(prio);
                    suggested.add(lookupKey.lookup.getLookupString());
                }
            });
        });
        return T2(suggested, hadComments.get());
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        if (DumbService.isDumb(parameters.getPosition().getProject())) {
            // following code relies on complex reference resolutions
            // very much, so trying to resolve type during indexing
            // is pointless and is likely to cause exceptions
            return;
        }
        PsiElement caretPsi = parameters.getPosition(); // usually leaf element
        Opt<PsiElement> firstParent = opt(caretPsi.getParent());
        boolean isCaretInsideQuotes = firstParent
            .fop(toCast(StringLiteralExpression.class)) // inside ['']
            .uni(l -> true, () -> false); // else just inside []

        long startTime = System.nanoTime();
        Mutable<Long> firstTime = new Mutable<>(-1L);

        int depth = getMaxDepth(parameters);
        SearchCtx search = new SearchCtx(parameters).setDepth(depth);
        FuncCtx funcCtx = new FuncCtx(search);
        search.isMain = true;
        ExprCtx exprCtx = new ExprCtx(funcCtx, caretPsi, 0);
        IIt<DeepType> arrTit;
        try {
            arrTit = resolveAtPsi(caretPsi, exprCtx);
        } catch (Throwable exc) {
            printExprTree(exprCtx, search, 0);
            throw exc;
        }
        arrTit.has();

        Mt arrMt = Mt.reuse(arrTit);
        // preliminary keys without type - they may be at least 3 times faster in some cases
        T2<Set<String>, Boolean> tuple = addNameOnly(arrMt, result, exprCtx, isCaretInsideQuotes, (lookupString) -> {
            //System.out.println("resolved " + search.getExpressionsResolved() + " expressions for first key - " + lookupString);
            firstTime.set(System.nanoTime() - startTime);
        });
        Set<String> suggested = tuple.a;
        boolean hadComments = tuple.b;

        long elapsed = System.nanoTime() - startTime;
        String prefix = "";
        String postfix = "";
        if (parameters.isAutoPopup()) {
            prefix = "Press _Ctrl + Space_ for more options. ";
        } else {
            // trying to make it take same space despite font being not monospace...
            postfix += "                                                      ";
        }
        // length of this message defines the width of popup dialog apparently
        result.addLookupAdvertisement(prefix + "Resolved " + search.getExpressionsResolved() +
            " expressions in " + (elapsed / 1000000000.0) + " sec. First in " + (firstTime.get() / 1000000000.0) + postfix);

        //printExprTree(exprCtx, search, 0);

        // I enabled auto-popup for it, but I want it to show
        // only my options, not 100500k built-in suggestions
        boolean isEmptySquareBracket = firstParent
            .fop(toCast(ConstantReference.class))
            .map(cst -> cst.getName())
            .map(n -> n.equals("")|| n.equals("IntellijIdeaRulezzz"))
            .def(false);

        runSafeRemainingContributors(result, parameters, otherSourceResult -> {
            // remove dupe built-in suggestions
            LookupElement lookup = otherSourceResult.getLookupElement();
            boolean wouldBeDisplayedOnItsOwn = !isEmptySquareBracket || !parameters.isAutoPopup();
            if (!suggested.contains(lookup.getLookupString()) &&
                wouldBeDisplayedOnItsOwn
            ) {
                result.addElement(lookup);
            }
        });

        if (hadComments) {
            // note, this character is not a simple space, it's U+2003 EM SPACE (mutton)
            result.addLookupAdvertisement(Tls.repeat(" ", 80 ));
        }
    }

    private static enum LookupKeyKind { NAME, INDEX, VAR };

    private static class LookupKey {

        final public LookupElementBuilder lookup;
        final public LookupKeyKind kind;

        LookupKey(LookupElementBuilder lookup, LookupKeyKind kind) {
            this.lookup = lookup;
            this.kind = kind;
        }
    }
}
