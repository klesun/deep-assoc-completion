package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayHashElementImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpPsiElementImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.completion_providers.*;

/**
 * my intention was to make string values appear _after_ Symfony plugin
 * options, but they are still shown first for some reason...
 */
public class DeepAssocLastCbtr extends CompletionContributor
{
    public DeepAssocLastCbtr()
    {
        // array_column($arr, '')
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(2, ParameterList.class)
                ,
            new ArrayColumnPvdr()
        );
        // array_key_exists('', $arr)
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(2, ParameterList.class)
                ,
            new ArrayKeyExistsPvdr()
        );
        // [Something::class, '']
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(3, ArrayCreationExpression.class)
                ,
            new ArrFuncRefNamePvdr()
        );
        // typing associative key when creating array
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(3, ArrayHashElementImpl.class)
                ,
            new UsedKeysPvdr()
        );
        // indexed value (that may become associative key as user continues typing)
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(2, PhpPsiElementImpl.class)
                .withSuperParent(3, ArrayCreationExpression.class)
                ,
            new UsedKeysPvdr()
        );
        // string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
        // or in_array('', $types) or in_array($type, ['AIR', ''])
        // should suggest possible values of 'type'
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                ,
            new StrValsPvdr()
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
