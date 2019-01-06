package org.klesun.deep_assoc_completion.helpers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import org.klesun.lang.Tls;

public class GuiUtil {
    public static InsertHandler<LookupElement> toRemoveIntStrQuotes()
    {
        return (ctx, lookup) -> {
            if (Tls.isNum(lookup.getLookupString())) {
                toAlwaysRemoveQuotes().handleInsert(ctx, lookup);
            }
        };
    }
    public static InsertHandler<LookupElement> toAlwaysRemoveQuotes()
    {
        return (ctx, lookup) -> {
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
        };
    }
}
