package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.icons.DeepIcons;
import org.klesun.lang.*;

import javax.swing.*;
import java.net.URL;
import java.util.*;

import static org.klesun.lang.Lang.*;

/**
 * $arr[''];
 */
public class AssocKeyPvdr extends CompletionProvider<CompletionParameters>
{
    final private static int BRIEF_VALUE_MAX_LEN = 50;

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

    private static LookupElementBuilder makePaddedLookup(String keyName, String ideaType, String briefVal)
    {
        ideaType = !ideaType.equals("") ? ideaType : "?";

        // (keyName + briefVal) length must be constant for all keys, or you'll
        // get nasty broken position of type when you highlight an option
        briefVal = briefVal.trim().equals("") ? "" : " = " + briefVal;
        briefVal = briefVal + "                                                                ";
        briefVal = Tls.substr(briefVal, 0, BRIEF_VALUE_MAX_LEN - keyName.length());
        return LookupElementBuilder.create(keyName)
            .withBoldness(!Tls.isNum(keyName))
            .withInsertHandler(GuiUtil.toRemoveIntStrQuotes())
            .withTailText(briefVal, true)
            .withIcon(getIcon())
            .withTypeText(ideaType, false);
    }

    /**
     * unlike built-in LookupElement, this one can be changed after being
     * displayed (if more detailed type info was calculated in background)
     */
    static class MutableLookup extends LookupElement
    {
        public LookupElementBuilder lookupData;
        private boolean includeQuotes;
        private InsertHandler<LookupElement> onInsert = GuiUtil.toRemoveIntStrQuotes();

        public MutableLookup(LookupElementBuilder lookupData, boolean includeQuotes) {
            this.lookupData = lookupData;
            this.includeQuotes = includeQuotes;
        }
        public String getKeyName() {
            return lookupData.getLookupString();
        }
        @NotNull public String getLookupString() {
            return includeQuotes && !Tls.isNum(lookupData.getLookupString())
                ? "'" + lookupData.getLookupString() + "'"
                : lookupData.getLookupString();
        }
        public void renderElement(LookupElementPresentation presentation) {
            lookupData.renderElement(presentation);
            if (includeQuotes && !Tls.isNum(lookupData.getLookupString())) {
                presentation.setItemText("'" + lookupData.getLookupString() + "'");
            }
        }
        public void handleInsert(InsertionContext ctx)
        {
            onInsert.handleInsert(ctx, this);
        }
    }

    public static It<DeepType> resolveAtPsi(PsiElement caretPsi, IExprCtx funcCtx)
    {
        return opt(caretPsi.getParent())
            .map(litRaw -> litRaw.getParent())
            .fop(toCast(ArrayIndex.class))
            .map(index -> index.getParent())
            .fop(toCast(ArrayAccessExpression.class))
            .map(expr -> expr.getValue())
            .fop(toCast(PhpExpression.class))
            .fap(srcExpr -> funcCtx.findExprType(srcExpr));
    }

    public static LookupElementBuilder makeFullLookup(Mt arrMt, String keyName, Set<String> comments)
    {
        Mt keyMt = It(arrMt.types).fap(t -> Mt.getKeySt(t, keyName)).wap(Mt::new);
        String comment = Tls.implode(" ", comments);
        String briefValue = keyMt.getBriefValueText(BRIEF_VALUE_MAX_LEN);
        if (!comment.trim().equals("")) {
            briefValue = Tls.substr(briefValue, 0, 12) + " " + comment;
        }
        String ideaTypeStr = keyMt.getIdeaTypes().flt(it -> !it.isEmpty()).lmt(2).str("|");
        return makePaddedLookup(keyName, ideaTypeStr, briefValue);
    }

    private static void printExprTree(ExprCtx root, SearchCtx search, int depth)
    {
        String indent = Tls.range(0, depth).rdc((sum,i) -> sum + " ", "");
        int typeCnt = root.typeCnt.def(0);
        if (search.currentExpr.equals(som(root))) {
            System.out.println("======================================= current expression =======================================");
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

    private T2<Dict<MutableLookup>, Map<String, Set<String>>> addNameOnly(Mt arrMt, CompletionResultSet result, boolean includeQuotes, C<String> onFirst)
    {
        Set<String> keyNames = new LinkedHashSet<>();
        Map<String, Set<String>> keyToComments = new HashMap<>();
        Mutable<Boolean> isFirst = new Mutable<>(true);
        Dict<MutableLookup> nameToMutLookup = new Dict<>(new LinkedHashMap<>());
        arrMt.types.fap(t -> t.keys).fch((k,i) -> {
            k.keyType.getTypes.get().itr().fch((kt,j) -> {
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
                    LookupElementBuilder justName = makePaddedLookup(keyName, "resolving...", "");
                    MutableLookup mutLookup = new MutableLookup(justName, includeQuotes);
                    int basePriority = Tls.isNum(keyName) ? 2000 : 2500;
                    result.addElement(PrioritizedLookupElement.withPriority(mutLookup, basePriority - keyNames.size()));
                    nameToMutLookup.put(keyName, mutLookup);

                    String briefTypeRaw = Mt.getKeyBriefTypeSt(k.getBriefTypes()).filterUnknown().filterMixed().toStringResolved();
                    mutLookup.lookupData = makePaddedLookup(keyName, briefTypeRaw, "");
                }
                String comment = Tls.implode(" ", k.comments);
                if (!comment.trim().equals("")) {
                    keyNamesToAdd.fch(name -> {
                        opt(nameToMutLookup.get(name)).thn(mutLookup -> {
                            mutLookup.lookupData = mutLookup.lookupData.withTailText(" " + Tls.substr(comment, 0, BRIEF_VALUE_MAX_LEN), true);
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
        int depth = getMaxDepth(parameters);
        SearchCtx search = new SearchCtx(parameters).setDepth(depth);
        FuncCtx funcCtx = new FuncCtx(search);
        search.isMain = true;
        Set<String> suggested = new HashSet<>();
        PsiElement caretPsi = parameters.getPosition(); // usually leaf element
        Opt<PsiElement> firstParent = opt(caretPsi.getParent());
        boolean includeQuotes = firstParent
            .fop(toCast(StringLiteralExpression.class)) // inside ['']
            .uni(l -> false, () -> true); // else just inside []

        long startTime = System.nanoTime();
        Mutable<Long> firstTime = new Mutable<>(-1L);

        ExprCtx exprCtx = new ExprCtx(funcCtx, caretPsi, 0);
        It<DeepType> arrTit;
        try {
            arrTit = resolveAtPsi(caretPsi, exprCtx);
        } catch (Throwable exc) {
            printExprTree(exprCtx, search, 0);
            throw exc;
        }
        System.out.println("gonna start iterating with " + search.getExpressionsResolved() + " expression already resolved");
        arrTit.has();
        System.out.println("checked if iterator has anything, took " + search.getExpressionsResolved() + " expressions");

        Mt arrMt = new Mt(arrTit);
        // preliminary keys without type - they may be at least 3 times faster in some cases
        T2<Dict<MutableLookup>, Map<String, Set<String>>> tuple = addNameOnly(arrMt, result, includeQuotes, (keyName) -> {
            System.out.println("resolved " + search.getExpressionsResolved() + " expressions for first key - " + keyName);
            firstTime.set(System.nanoTime() - startTime);
        });
        Dict<MutableLookup> nameToMutLookup = tuple.a;
        Map<String, Set<String>> keyToComments = tuple.b;

        long elapsed = System.nanoTime() - startTime;
        System.out.println("Resolved all key names in " + search.getExpressionsResolved() + " expressions");
        result.addLookupAdvertisement("Press _Ctrl + Space_ for more options. Resolved " + search.getExpressionsResolved() +
            " expressions in " + (elapsed / 1000000000.0) + " sec. First in " + (firstTime.get() / 1000000000.0));

        //printExprTree(exprCtx, 0);

        // I enabled auto-popup for it, but I want it to show
        // only my options, not 100500k built-in suggestions
        boolean isEmptySquareBracket = firstParent
            .fop(toCast(ConstantReference.class))
            .map(cst -> cst.getName())
            .map(n -> n.equals("")|| n.equals("IntellijIdeaRulezzz"))
            .def(false);

        nameToMutLookup.map(l -> l.getLookupString()).fch(el -> suggested.add(el));
        result.runRemainingContributors(parameters, otherSourceResult -> {
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

        nameToMutLookup
            .fch((mutLook, keyName) -> {
                search.overrideMaxExpr = som(search.getExpressionsResolved() + 25);
                Set<String> comments = opt(keyToComments.get(keyName)).def(new HashSet<>());
                LookupElementBuilder lookup = makeFullLookup(arrMt, keyName, comments);
                search.overrideMaxExpr = non();
                mutLook.lookupData = lookup;
            });
    }
}
