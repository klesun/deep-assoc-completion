package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.completion_providers.ArrFuncRefNamePvdr;

/**
 * my intention was to make string values appear _after_ Symfony plugin
 * options, but they are still shown first for some reason...
 */
public class DeepAssocLastCbtr extends CompletionContributor
{
    public DeepAssocLastCbtr()
    {
        // [Something::class, '']
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(3, ArrayCreationExpression.class)
                ,
            new ArrFuncRefNamePvdr()
        );
    }

    /**
     * Allow autoPopup to appear after custom symbol
     */
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        if (typeChar == '\'' || typeChar == '\"' || typeChar == '>') {
            return true;
        } else {
            return false;
        }
    }
}
