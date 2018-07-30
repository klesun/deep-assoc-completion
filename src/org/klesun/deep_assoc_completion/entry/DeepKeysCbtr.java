package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ArrayHashElementImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpPsiElementImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.completion_providers.*;

import static org.klesun.lang.Lang.opt;
import static org.klesun.lang.Lang.toCast;

/**
 * provides associative array keys autocomplete
 * using declaration inside the initial function
 * that created this array
 */
public class DeepKeysCbtr extends CompletionContributor
{
    public DeepKeysCbtr()
    {
        // $arr[''];
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(2, ArrayIndex.class),
            new DeepKeysPvdr()
        );
        // $arr[];
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, ConstantReference.class) // I dunno why
                .withSuperParent(2, ArrayIndex.class)
                ,
            new DeepKeysPvdr()
        );
        // @param $a = SomeClass::
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(2, PhpDocTag.class),
            new DocFqnPvdr()
        );
        // array_column($arr, '')
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(2, ParameterList.class)
                ,
            new ArrayColumnPvdr()
        );
        // [Something::class, '']
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(1, StringLiteralExpression.class)
                .withSuperParent(3, ArrayCreationExpression.class)
                ,
            new ArrFuncRefPvdr()
        );
        // associative key
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
            new EqStrValsPvdr()
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
        } else {
            return false;
        }
    }
}
