package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.go_to_decl_providers.DeepObjMemberGoToDecl;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
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
        Project project = psi.getProject();
        DeepSettings settings = DeepSettings.inst(project);
        if (!settings.bgTypePvdrEnabled) {
            return null;
        }

        if (DumbService.isDumb(psi.getProject())) {
            // following code relies on complex reference resolutions
            // very much, so trying to resolve type during indexing
            // is pointless and causes Contract Violation exceptions
            // so let's exit with null
            return null;
        }

        long startTime = System.nanoTime();

        boolean isMethCall = PlatformPatterns.psiElement()
            .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
            .accepts(psi);
        boolean isFieldAcc = PlatformPatterns.psiElement()
            .withParent(PlatformPatterns.psiElement(PhpElementTypes.FIELD_REFERENCE))
            .accepts(psi);

        // we will calculate type only for method or property access
        if (!isMethCall && !isFieldAcc){
            return null;
        }

        // Type Provider is called at random points of time breaking
        // my recursive formatting in STDOUT, so always setDebug(false)
        SearchContext search = new SearchContext()
            .setDepth(settings.bgTypePvdrDepthLimit)
            .setTimeout(settings.bgTypePvdrTimeout / 1000)
            .setMaxExpressions(1000).setDebug(false);
        FuncCtx funcCtx = new FuncCtx(search);

        @Nullable PhpType result = null;
        try {
            result = Tls.cast(PhpExpression.class, psi)
                .map(exp -> funcCtx.findExprType(exp))
                .map(mt -> mt.getIdeaType())
                .def(null);
        } catch (Throwable exc) {
            // throwing some exceptions further would cause class to become undefined
            L<Class> allowedExceptions = list(ControlFlowException.class);
            if (allowedExceptions.any(excCls -> exc.getClass().isAssignableFrom(excCls))) {
                throw exc;
            } else {
                String msg = "Unexpected exception in deep-assoc-completion plugin - " + exc.getClass() + " while resolving " + psi.getText() + " " + psi.getClass() + " " + opt(psi.getContainingFile()).map(f -> f.getName()).def("(no file)") + " :" + psi.getTextOffset();
                com.intellij.openapi.diagnostic.Logger.getInstance(getClass()).debug(msg, exc);
                System.out.println(msg + "\n" + Tls.getStackTrace(exc));
                // it would also be nice to email such cases to me somehow...
                return null;
            }
        }

        long elapsed = System.nanoTime() - startTime;
        double seconds = elapsed / 1000000000.0;
        if (search.getExpressionsResolved() > 0 && seconds > 0.5) {
            System.out.println("Resolved " + search.getExpressionsResolved() + " expressions in " + seconds + " seconds");
        }

        return result;
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
