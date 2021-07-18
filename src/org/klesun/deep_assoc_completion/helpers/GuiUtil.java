package org.klesun.deep_assoc_completion.helpers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.deep_assoc_completion.helpers.QuotesState.Kind;
import org.klesun.lang.Lang.Mutable;
import org.klesun.lang.Tls;

public class GuiUtil {
    public static void putCaretForward(InsertionContext ctx, QuotesState quotesState) {
        // place caret after closing bracket if possible
        int finalPos = ctx.getTailOffset();
        boolean isCaretAfterClosingQuote =
            quotesState.kind ==  Kind.HAD_LEFT ||
            quotesState.kind ==  Kind.HAD_NONE;
        if (!isCaretAfterClosingQuote) {
            String expectedEndChar = quotesState.quoteChar.toString();
            String actualEndChar = ctx.getEditor().getDocument()
                .getText(new TextRange(finalPos, finalPos + 1));
            if (!expectedEndChar.equals(actualEndChar)) {
                // do not attempt to update caret position, as this
                // completion did not end at the end of string literal
                // may happen if it was an unterminated quote in the middle of line, rather than end
                return;
            } else  {
                finalPos += 1; // ends before closing "'", place after the "'"
            }
        }

        String caretChar = ctx.getEditor().getDocument()
            .getText(new TextRange(finalPos, finalPos + 1));
        if (caretChar.equals("]")) {
            finalPos += 1; // place after the "]"
        }
        ctx.getEditor().getCaretModel().moveToOffset(finalPos);
    }

    public static void removeQuotes(InsertionContext ctx, LookupElement lookup)
    {
        int from = ctx.getStartOffset();
        int to = ctx.getTailOffset();
        if (from != to) {
            if (ctx.getEditor().getDocument().getText(TextRange.create(from - 1, from)).equals("'") &&
                ctx.getEditor().getDocument().getText(TextRange.create(to, to + 1)).equals("'")
            ) {
                ctx.getEditor().getDocument().deleteString(to, to + 1);
                ctx.getEditor().getDocument().deleteString(from - 1, from);
            }
        }
    }

    public static void removeIntStrQuotes(InsertionContext ctx, LookupElement lookup)
    {
        if (Tls.isNum(lookup.getLookupString())) {
            removeQuotes(ctx, lookup);
        }
    }

    public static InsertHandler<LookupElement> toRemoveIntStrQuotes()
    {
        return GuiUtil::removeIntStrQuotes;
    }
    public static InsertHandler<LookupElement> toAlwaysRemoveQuotes()
    {
        return GuiUtil::removeQuotes;
    }

    public static void runSafeRemainingContributors(
        @NotNull CompletionResultSet result,
        @NotNull CompletionParameters parameters,
        Consumer<? super CompletionResult> consumer
    ) {
        Project project = parameters.getPosition().getProject();
        if (!DeepSettings.inst(project).adjustOtherPluginOptions) {
            // we want to make it disablable to debug an issue that happens during runRemainingContributors
            return;
        }
        Mutable<Boolean> isOurs = new Mutable<>(false);
        try {
            result.runRemainingContributors(parameters, contributorResult -> {
                try {
                    consumer.consume(contributorResult);
                } catch (Throwable exc) {
                    isOurs.set(true);
                    throw exc;
                }
            });
        } catch (ProcessCanceledException exc) {
            // happens when user interrupts completion resolution, ignore
        } catch (Throwable exc) {
            if (isOurs.get()) {
                throw exc;
            } else {
                // ignore other plugins errors
                System.out.println("runRemainingContributors() ran by deep-assoc resulted in error:");
                exc.printStackTrace();
            }
        }
    }
}
