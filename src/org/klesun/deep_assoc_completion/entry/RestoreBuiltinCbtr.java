package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * because my plugin triggers auto-popup in [], built-in auto-popup
 * does not trigger for variable completion in [$] - fixing that
 */
public class RestoreBuiltinCbtr extends CompletionContributor
{
    public RestoreBuiltinCbtr()
    {
    }

    /**
     * Allow autoPopup to appear after custom symbol
     */
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        if (typeChar == '$') {
            return true;
        } else {
            return false;
        }
    }
}
