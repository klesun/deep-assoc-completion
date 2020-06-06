package org.klesun.deep_assoc_completion.helpers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import org.klesun.lang.Tls;

public class GuiUtil {
    private static void removeQuotes(InsertionContext ctx, LookupElement lookup)
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
}
