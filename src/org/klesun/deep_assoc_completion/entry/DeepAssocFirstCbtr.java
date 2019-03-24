package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocToken;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.completion_providers.*;

import static org.klesun.lang.Lang.opt;
import static org.klesun.lang.Lang.toCast;

/**
 * provides associative array keys autocomplete
 * using declaration inside the initial function
 * that created this array
 */
public class DeepAssocFirstCbtr extends CompletionContributor
{
    public DeepAssocFirstCbtr()
    {
        // $arr[''];
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(2, ArrayIndex.class),
            new AssocKeyPvdr()
        );
        // $arr[];
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, ConstantReference.class) // because IntellijIdeaRulezzz
                .withSuperParent(2, ArrayIndex.class)
                ,
            new AssocKeyPvdr()
        );
        // @param $a = SomeClass::
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(2, PhpDocTag.class),
            new DocFqnPvdr()
        );
        /** @param $params = [
         *     'weekday' => self::we, // should suggest "weekday"
         *     'youkai' => new YakumoR, // should suggest "YakumoRan"
         * ] */
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(0, PhpDocToken.class)
                .withSuperParent(1, PhpDocComment.class)
                ,
            new CompletionProvider<CompletionParameters>() {
                protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                    new DocFqnPvdr().addCompletionsMultiline(completionParameters, processingContext, completionResultSet);
                }
            }
        );
        // $GLOBALS['myVar'] = 123;
        // $myV;
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, VariableImpl.class)
                ,
            new VarNamePvdr()
        );
        // json_encode($data, JSON_PRETTY_PRINT | );
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, ConstantReference.class) // because IntellijIdeaRulezzz
                ,
            new ArgCstPvdr()
        );
        // for cases when only Deep Resolve can say what class it is
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, MemberReference.class)
                ,
            new ObjMemberPvdr()
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
