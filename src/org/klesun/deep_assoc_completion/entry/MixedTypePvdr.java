package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

import static org.klesun.lang.Lang.list;

public class MixedTypePvdr implements PhpTypeProvider4
{
    public char getKey()
    {
        // i dunno what that means, copy pasted from:
        // https://github.com/Haehnchen/idea-php-symfony2-plugin/blob/master/src/fr/adrienbrault/idea/symfony2plugin/doctrine/ObjectRepositoryResultTypeProvider.java
        return '\u0159';
    }

    @Nullable
    public PhpType getType(PsiElement psi)
    {
        boolean isArrAcc = PlatformPatterns.psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
            .accepts(psi);

        if (isArrAcc) {
            // when you store some object in an associative array, phpstorm
            // sometimes can't resolve it and reports methods as undefined
            return PhpType.MIXED;
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public PhpType complete(String expression, Project project) {
        return null;
    }

    // 2016.2.2
    public Collection<? extends PhpNamedElement> getBySignature(String s, Project project) {
        return list();
    }

    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int i, Project project)
    {
        return list();
    }
}
