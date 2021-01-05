package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
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

import static org.klesun.deep_assoc_completion.helpers.GuiUtil.runSafeRemainingContributors;
import static org.klesun.lang.Lang.*;

/**
 * $arr[''];
 */
public class AssocKeyPvdr extends CompletionProvider<CompletionParameters>
{
    final public static int BRIEF_VALUE_MAX_LEN = 65;
    final public static int COMMENTED_MAX_LEN = 90;

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

    private static LookupElementBuilder makePaddedLookup(String keyName, String ideaType, String briefVal, int maxValLen)
    {
        ideaType = !ideaType.equals("") ? ideaType : "?";

        // (keyName + briefVal) length must be constant for all keys, or you'll
        // get nasty broken position of type when you highlight an option
        briefVal = briefVal.trim().equals("") ? "" : " = " + briefVal;
        briefVal = Tls.substr(briefVal, 0, maxValLen - keyName.length());
        return LookupElementBuilder.create(keyName)
            .withBoldness(!Tls.isNum(keyName))
            .withTailText(briefVal, true)
            .withIcon(getIcon())
            .withTypeText(ideaType, false);
    }

    /**
     * unlike built-in LookupElement, this one can be changed after being
     * displayed (if more detailed type info was calculated in background)
     *
     * upd.: mutations are no more updating UI sadly, so this class is pretty useless now
     */
    static class MutableLookup extends LookupElement
    {
        public LookupElementBuilder lookupData;
        private boolean isCaretInsideQuotes;

        public MutableLookup(LookupElementBuilder lookupData, boolean isCaretInsideQuotes) {
            this.lookupData = lookupData;
            this.isCaretInsideQuotes = isCaretInsideQuotes;
        }

        private boolean shouldAddQuotes() {
            return !isCaretInsideQuotes
                && !Tls.isNum(lookupData.getLookupString());
        }

        public String getKeyName() {
            return lookupData.getLookupString();
        }
        @NotNull public String getLookupString() {
            return shouldAddQuotes()
                ? "'" + lookupData.getLookupString() + "'"
                : lookupData.getLookupString();
        }
        public void renderElement(LookupElementPresentation presentation) {
            lookupData.renderElement(presentation);
            if (shouldAddQuotes()) {
                presentation.setItemText("'" + lookupData.getLookupString() + "'");
            }
        }
        public void handleInsert(InsertionContext ctx)
        {
            int endPos = ctx.getTailOffset();
            // place caret after closing bracket
            int offset = isCaretInsideQuotes ? 2 : 1;
            ctx.getEditor().getCaretModel().moveToOffset(endPos + offset);
            GuiUtil.removeIntStrQuotes(ctx, this);
        }
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

    public static LookupElementBuilder makeFullLookup(Mt arrMt, String keyName, Opt<String> commentOpt)
    {
        Mt keyMt = arrMt.types.fap(t -> Mt.getKeySt(t, keyName)).wap(Mt::mem);
        int maxValLen = commentOpt.has() ? COMMENTED_MAX_LEN : BRIEF_VALUE_MAX_LEN;
        String briefValue = keyMt.getBriefValueText(maxValLen);
        if (commentOpt.has()) {
            briefValue = Tls.substr(briefValue, 0, 20) + " // " + commentOpt.unw();
        }
        String ideaTypeStr = keyMt.getIdeaTypes().flt(it -> !it.isEmpty()).lmt(2).str("|");
        return makePaddedLookup(keyName, ideaTypeStr, briefValue, maxValLen);
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

    private T2<Dict<MutableLookup>, Map<String, Set<String>>> addNameOnly(
        Mt arrMt, CompletionResultSet result, boolean isCaretInsideQuotes, C<String> onFirst
    ) {
        Set<String> keyNames = new LinkedHashSet<>();
        Map<String, Set<String>> keyToComments = new HashMap<>();
        Mutable<Boolean> isFirst = new Mutable<>(true);
        Dict<MutableLookup> nameToMutLookup = new Dict<>(new LinkedHashMap<>());
        arrMt.types.fap(t -> t.keys).fch((k, i) -> {
            k.keyType.getTypes().itr().fch((kt,j) -> {
                L<String> keyNamesToAdd = list();
                if (kt.stringValue == null) {
                    for (int n = 0; n < 5; ++n) {
                        keyNamesToAdd.add(n + "");
                    }
                } else {
                    keyNamesToAdd.add(kt.stringValue);
                }
                L<String> newKeyNamesToAdd = keyNamesToAdd.flt(kn -> !keyNames.contains(kn)).arr();
                for (String keyName: newKeyNamesToAdd) {
                    if (isFirst.get()) {
                        isFirst.set(false);
                        onFirst.accept(keyName);
                    }
                    keyNames.add(keyName);
                    String briefTypeRaw = Mt.getKeyBriefTypeSt(k.getBriefTypes()).filterUnknown().filterMixed().toStringResolved();

                    String briefVal = getGrantedBriefValue(k);
                    LookupElementBuilder justName = makePaddedLookup(keyName, briefTypeRaw, briefVal, BRIEF_VALUE_MAX_LEN);
                    MutableLookup mutLookup = new MutableLookup(justName, isCaretInsideQuotes);
                    int basePriority = Tls.isNum(keyName) ? 2000 : 2500;
                    LookupElement prio = PrioritizedLookupElement
                        .withPriority(mutLookup, basePriority - keyNames.size());
                    result.addElement(prio);
                    nameToMutLookup.put(keyName, mutLookup);
                }
                String comment = Tls.implode(" ", k.comments);
                if (!comment.trim().equals("")) {
                    keyNamesToAdd.fch(name -> {
                        opt(nameToMutLookup.get(name)).thn(mutLookup -> {
                            mutLookup.lookupData = mutLookup.lookupData
                                .withTailText(" " + Tls.substr(comment, 0, BRIEF_VALUE_MAX_LEN), true);
                        });
                        if (!keyToComments.containsKey(name)) {
                            keyToComments.put(name, new LinkedHashSet<>());
                        }
                        keyToComments.get(name).addAll(k.comments);
                    });
                }
            });
        });
        return T2(nameToMutLookup, keyToComments);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        Set<String> suggested = new HashSet<>();
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
        T2<Dict<MutableLookup>, Map<String, Set<String>>> tuple = addNameOnly(arrMt, result, isCaretInsideQuotes, (keyName) -> {
            //System.out.println("resolved " + search.getExpressionsResolved() + " expressions for first key - " + keyName);
            firstTime.set(System.nanoTime() - startTime);
        });
        Dict<MutableLookup> nameToMutLookup = tuple.a;
        Map<String, Set<String>> keyToComments = tuple.b;

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

        nameToMutLookup.map(l -> l.getLookupString()).fch(el -> suggested.add(el));
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

        // following code calculates deeper type info for
        // completion options and updates them in the dialog

        Mutable<Boolean> hadComments = new Mutable<>(false);
        nameToMutLookup
            .fch((mutLook, keyName) -> {
                // TODO: move comments logic to instant popup data, as they won't work anymore if added separately

                search.overrideMaxExpr = som(search.getExpressionsResolved() + 25);

                Set<String> comments = opt(keyToComments.get(keyName)).def(new HashSet<>());
                String comment = Tls.implode(" ", comments);
                Opt<String> commentOpt = comment.trim().equals("") ? non() : som(comment);
                L<F<LookupElementBuilder, LookupElementBuilder>> metCommentOpt = commentOpt
                    .fap(c -> assertMetaComment(c, exprCtx)).arr();
                if (metCommentOpt.has()) {
                    commentOpt = non();
                }

                LookupElementBuilder lookup = makeFullLookup(arrMt, keyName, commentOpt);
                for (F<LookupElementBuilder, LookupElementBuilder> updater: metCommentOpt) {
                    lookup = updater.apply(lookup);
                }
                if (commentOpt.has()) {
                    hadComments.set(true);
                }
                search.overrideMaxExpr = non();
                mutLook.lookupData = lookup;
            });

        if (hadComments.get()) {
            // note, this character is not a simple space, it's U+2003 EM SPACE (mutton)
            result.addLookupAdvertisement(Tls.repeat(" ", 80 ));
        }
    }
}
