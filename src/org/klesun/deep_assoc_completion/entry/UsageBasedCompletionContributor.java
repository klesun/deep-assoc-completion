package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocToken;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpPsiElementImpl;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.completion_providers.*;

import static org.klesun.lang.Lang.opt;
import static org.klesun.lang.Lang.toCast;

/**
 * for function argument keys completion - had to move it to separate contribute because apparently
 * runRemainingContributors() discards all following extends, so you can't have same PSI covered by
 * multiple providers
 */
public class UsageBasedCompletionContributor extends CompletionContributor
{
    public UsageBasedCompletionContributor()
    {
        // json_encode($data, JSON_PRETTY_PRINT | );
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, ConstantReference.class) // because IntellijIdeaRulezzz
                ,
            new ArgCstPvdr()
        );

        //  \/
        // [''] + zhopa = ['zhopa' => ]
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(2, PhpPsiElementImpl.class)
                .withSuperParent(3, ArrayCreationExpression.class)
            ,
            new ArrCtorIncompleteAssocKeyPvdr()
        );
        // string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
        // or in_array('', $types) or in_array($type, ['AIR', ''])
        // should suggest possible values of 'type'
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                ,
            new UsedStrValsPvdr()
        );
    }

    /**
     * Allow autoPopup to appear after custom symbol
     */
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        if (typeChar == '\'' || typeChar == '\"' || typeChar == '>') {
            return true;
        } else if (typeChar == '[') {
            return opt(position.getParent())
                .fop(toCast(PhpExpression.class))
                .map(par -> par.getLastChild())
                .flt(lst -> lst.isEquivalentTo(position))
                .uni(var -> true, () -> false);
        } else if (ArgCstPvdr.invokeAutoPopup(position, typeChar)) {
            return true;
        } else {
            return false;
        }
    }
}
