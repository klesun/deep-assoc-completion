package org.klesun.deep_keys.entry;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import org.klesun.deep_keys.DeepKeysPvdr;

/**
 * provides associative array keys autocomplete
 * using declaration inside the initial function
 * that created this array
 */
public class DeepKeysCbtr extends CompletionContributor
{
    public DeepKeysCbtr()
    {
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(2, ArrayIndex.class),
            new DeepKeysPvdr()
        );
    }
}
