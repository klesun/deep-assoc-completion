package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import javax.swing.*;
import java.util.*;

import static org.klesun.lang.Lang.*;

/**
 * contains the completion logic
 */
public class DeepKeysPvdr extends CompletionProvider<CompletionParameters>
{
    final private static int BRIEF_TYPE_MAX_LEN = 50;

    final public static ImageIcon icon = new ImageIcon(DeepKeysPvdr.class.getResource("../icons/deep_16_ruby2.png"));

    public static int getMaxDepth(boolean isAutoPopup)
    {
        return isAutoPopup ? 25 : 40;
    }

    private static InsertHandler<LookupElement> makeInsertHandler()
    {
        return (ctx, lookup) -> {
            int from = ctx.getStartOffset();
            int to = ctx.getTailOffset();
            if (Tls.isNum(lookup.getLookupString()) && from != to) {
                if (ctx.getEditor().getDocument().getText(TextRange.create(from - 1, from)).equals("'") &&
                    ctx.getEditor().getDocument().getText(TextRange.create(to, to + 1)).equals("'")
                ) {
                    ctx.getEditor().getDocument().deleteString(to, to + 1);
                    ctx.getEditor().getDocument().deleteString(from - 1, from);
                }
            }
        };
    }

    private static LookupElement makePaddedLookup(String keyName, String ideaType, String briefVal)
    {
        ideaType = !ideaType.equals("") ? ideaType : "?";

        // (keyName + briefVal) length must be constant for all keys, or you'll
        // get nasty broken position of type when you highlight an option
        briefVal = briefVal.trim().equals("") ? "" : " = " + briefVal;
        briefVal = briefVal + "                                                                ";
        briefVal = Tls.substr(briefVal, 0, BRIEF_TYPE_MAX_LEN - keyName.length());
        return LookupElementBuilder.create(keyName)
            .bold()
            .withInsertHandler(makeInsertHandler())
            .withTailText(briefVal, true)
            .withIcon(icon)
            .withTypeText(ideaType, false);
    }

    /**
     * unlike built-in LookupElement, this one can be changed after being
     * displayed (if more detailed type info was calculated in background)
     */
    static class MutableLookup extends LookupElement
    {
        public LookupElement lookupData;
        private boolean includeQuotes;
        private InsertHandler<LookupElement> onInsert = makeInsertHandler();

        public MutableLookup(LookupElement lookupData, boolean includeQuotes) {
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

    public static MultiType resolveAtPsi(PsiElement caretPsi, SearchContext search)
    {
        FuncCtx funcCtx = new FuncCtx(search);
        return opt(caretPsi.getParent())
            .map(litRaw -> litRaw.getParent())
            .fop(toCast(ArrayIndex.class))
            .map(index -> index.getParent())
            .fop(toCast(ArrayAccessExpression.class))
            .map(expr -> expr.getValue())
            .fop(toCast(PhpExpression.class))
            .map(srcExpr -> funcCtx.findExprType(srcExpr))
            .def(MultiType.INVALID_PSI);
    }

    public static LookupElement makeFullLookup(MultiType mt, String keyName)
    {
        MultiType keyMt = mt.getKey(keyName);
        String briefValue = keyMt.getBriefValueText(BRIEF_TYPE_MAX_LEN);
        String ideaTypeStr = keyMt.getIdeaType().filterUnknown().toStringResolved();
        return makePaddedLookup(keyName, ideaTypeStr, briefValue);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        int depth = getMaxDepth(parameters.isAutoPopup());
        SearchContext search = new SearchContext().setDepth(depth);
        Set<String> suggested = new HashSet<>();
        PsiElement caretPsi = parameters.getPosition(); // usually leaf element
        Opt<PsiElement> firstParent = opt(caretPsi.getParent());
        boolean includeQuotes = firstParent
            .fop(toCast(StringLiteralExpression.class)) // inside ['']
            .uni(l -> false, () -> true); // else just inside []

        long startTime = System.nanoTime();
        MultiType mt = resolveAtPsi(caretPsi, search);

        L<String> keyNames = mt.getKeyNames();
        L<MutableLookup> lookups = L();
        // preliminary keys without type - they may be at least 3 times faster in some cases
        keyNames.fch((keyName, i) -> {
            LookupElement justName = makePaddedLookup(keyName, "resolving...", "");
            MutableLookup mutLookup = new MutableLookup(justName, includeQuotes);
            result.addElement(PrioritizedLookupElement.withPriority(mutLookup, 2000 - i));
            lookups.add(mutLookup);

            String briefTypeRaw = mt.getKeyBriefType(keyName).filterUnknown().toStringResolved();
            mutLookup.lookupData = makePaddedLookup(keyName, briefTypeRaw, "");
        });
        L<DeepType> indexTypes = mt.types.fap(t -> t.getElemTypes());
        if (indexTypes.size() > 0) {
            String typeText = new MultiType(indexTypes).getBriefValueText(BRIEF_TYPE_MAX_LEN);
            String ideaType = new MultiType(indexTypes).getIdeaType().filterUnknown().toStringResolved();
            if (mt.hasNumberIndexes()) {
                for (int k = 0; k < 5; ++k) {
                    result.addElement(makePaddedLookup(k + "", ideaType, typeText));
                }
            } else {
                // string key, but key name unknown
                result.addElement(makePaddedLookup("", ideaType, typeText));
            }
        }

        // I enabled auto-popup for it, but I want it to show
        // only my options, not 100500k built-in suggestions
        boolean isEmptySquareBracket = firstParent
            .fop(toCast(ConstantReference.class))
            .map(cst -> cst.getName())
            .map(n -> n.equals("")|| n.equals("IntellijIdeaRulezzz"))
            .def(false);

        suggested.addAll(lookups.map(l -> l.getLookupString()));
        result.runRemainingContributors(parameters, otherSourceResult -> {
            // remove dupe built-in suggestions
            LookupElement lookup = otherSourceResult.getLookupElement();
            if (!suggested.contains(lookup.getLookupString()) &&
                !isEmptySquareBracket // no auto-popup is needed here
            ) {
                result.addElement(lookup);
            }
        });

        // following code calculates deeper type info for
        // completion options and updates them in the dialog

        Dict<LookupElement> nameToNewLookup = keyNames.key(keyName -> keyName)
            .map(keyName -> makeFullLookup(mt, keyName));

        lookups.fch(l -> nameToNewLookup.gat(l.getKeyName()).thn(newL -> l.lookupData = newL));

        long elapsed = System.nanoTime() - startTime;
        result.addLookupAdvertisement("Resolved " + search.getExpressionsResolved() + " expressions in " + (elapsed / 1000000000.0) + " seconds");

        if (search.getExpressionsResolved() > 100) {
            System.out.println("Resolved " + search.getExpressionsResolved() + " expressions in " + (elapsed / 1000000000.0) + " seconds");
        }
    }
}
