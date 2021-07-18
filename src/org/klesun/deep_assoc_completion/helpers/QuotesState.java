package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.opt;

public class QuotesState {
    public enum Kind { HAD_NONE, HAD_LEFT, HAD_BOTH }

    final public Kind kind;
    final public Character quoteChar;

    /** more correct would probably be to take it from user settings, but idgaf */
    final public static char FALLBACK_QUOTE = '\'';

    private QuotesState(Kind kind, Character quoteChar) {
        this.kind = kind;
        this.quoteChar = quoteChar;
    }

    public static QuotesState fromCaretPsi(PsiElement caretPsi) {
        Opt<PsiElement> firstParent = opt(caretPsi.getParent());
        return firstParent
            .cst(StringLiteralExpression.class) // inside ['']
            .uni(
                lit -> {
                    String litText = lit.getText().replaceAll("\\n[\\s\\S]*", "");
                    char quoteChar = litText.charAt(0);
                    return litText.endsWith(Character.toString(quoteChar))
                        ? new QuotesState(Kind.HAD_BOTH, quoteChar)
                        : new QuotesState(Kind.HAD_LEFT, quoteChar);
                },
                () -> new QuotesState(Kind.HAD_NONE, FALLBACK_QUOTE) // else just inside []
            );
    }

    public String completeQuoting(String keyName) {
        if (kind == Kind.HAD_LEFT) {
            return keyName + quoteChar; // close unterminated quote if any
        } else if (kind == Kind.HAD_NONE && !Tls.isNum(keyName)) {
            return FALLBACK_QUOTE + keyName + FALLBACK_QUOTE;
        } else {
            return keyName;
        }
    }
}
