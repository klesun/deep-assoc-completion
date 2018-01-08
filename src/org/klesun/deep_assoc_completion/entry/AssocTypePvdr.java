package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.AssignmentExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ArrAccRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.Collection;
import java.util.Set;

public class AssocTypePvdr extends Lang implements PhpTypeProvider3
{
    public char getKey()
    {
        // i dunno what that means, copy pasted from:
        // https://github.com/Haehnchen/idea-php-symfony2-plugin/blob/master/src/fr/adrienbrault/idea/symfony2plugin/doctrine/ObjectRepositoryResultTypeProvider.java
        return '\u0152';
    }

    @Nullable
    public PhpType getType(PsiElement psi)
    {
        if (DumbService.isDumb(psi.getProject())) {
            // following code relies on complex reference resolutions
            // very much, so trying to resolve type during indexing
            // is pointless and causes Contract Violation exceptions
            // so let's exit with null
            return null;
        }

        long startTime = System.nanoTime();

        boolean isMethCall = PlatformPatterns.psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
            .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
            .accepts(psi);
        boolean isFieldAcc = PlatformPatterns.psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
            .withParent(PlatformPatterns.psiElement(PhpElementTypes.FIELD_REFERENCE))
            .accepts(psi);
        boolean isAssVal = Tls.cast(ArrayAccessExpressionImpl.class, psi)
            .map(acc -> acc.getParent())
            .fop(toCast(AssignmentExpressionImpl.class))
            .map(ass -> psi.isEquivalentTo(ass.getValue()))
            .def(false);

        // we will calculate type only for method or property access
        if (!isMethCall && !isFieldAcc && !isAssVal){
            return null;
        }

        SearchContext search = new SearchContext().setDepth(35);
        IFuncCtx funcCtx = new FuncCtx(search, L());


        @Nullable PhpType result = Tls.cast(ArrayAccessExpressionImpl.class, psi)
            .map(acc -> new ArrAccRes(funcCtx).resolve(acc))
            .map(mt -> mt.getIdeaType())
            .def(null);

        long elapsed = System.nanoTime() - startTime;
        double seconds = elapsed / 1000000000.0;
        if (search.getExpressionsResolved() > 0 && seconds > 0.1) {
            System.out.println("Resolved " + search.getExpressionsResolved() + " expressions in " + seconds + " seconds");
        }

        return result;
    }

    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int i, Project project)
    {
        return list();
    }
}
