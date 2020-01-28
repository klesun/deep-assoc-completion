package org.klesun.deep_assoc_completion.entry;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpCallbackFunctionUtil;
import com.jetbrains.php.lang.PhpCallbackReferenceBase;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;

/**
 * provides method references in such expressions:
 * [SomeClass::class, 'doSomething']
 * [self::class, 'doSomething']
 * [static::class, 'doSomething']
 * [$this, 'doSomething']
 * [$someObj, 'doSomething']
 *
 * phpstorm's contributor only links them when array is passed to the
 * function, but not when you have array of callbacks for example
 */
public class ArrFuncRefCbtr extends PsiReferenceContributor
{
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

        registrar.registerReferenceProvider(
            ((PsiElementPattern.Capture)PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE))
                .withElementType(PhpElementTypes.STRING)
                .withSuperParent(2, ArrayCreationExpression.class)
                ,
            new ArrFuncRefPvdr(),
            0.0D
        );
    }

    private static class ArrFuncRefPvdr extends PsiReferenceProvider {
        private static boolean hasMethod(PhpExpression clsRef, StringLiteralExpression methLit) {
            String methName = methLit.getContents();
            return ArrCtorRes.resolveIdeaTypeCls(clsRef.getType(), clsRef.getProject())
                .any(clsPsi -> L(clsPsi.getMethods()).any(m -> methName.equals(m.getName())));
        }

        @NotNull
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            PsiReference[] results = Tls.cast(StringLiteralExpression.class, element)
                .map(lit -> lit.getParent()) // array value
                .map(val -> val.getParent())
                .fop(toCast(ArrayCreationExpression.class))
                .map(arr -> L(arr.getChildren()))
                .flt(valsPsis -> {
                    return valsPsis.size() == 2
                        && element.isEquivalentTo(valsPsis.get(1).getFirstChild())
                        && !valsPsis.any(v -> v instanceof ArrayHashElement);
                })
                // phpstorm wraps each value, probably due to assoc/ordered array syntax ambiguation
                .fop(valsPsis -> valsPsis.fal(psi -> opt(psi.getFirstChild())))
                .fop(vals -> {
                    Opt<PhpExpression> clsRefOpt = vals.gat(0)
                        .cst(PhpExpression.class);
                    Opt<StringLiteralExpression> methLitOpt = vals.gat(1)
                        .cst(StringLiteralExpression.class);
                    return T2.all(clsRefOpt, methLitOpt);
                })
                .fop(t -> t.nme((clsRef, methLit) -> {
                    if (hasMethod(clsRef, methLit)) {
                        PhpCallbackReferenceBase ref = PhpCallbackReferenceBase
                            .createMemberReference(clsRef, methLit, true);
                        return opt(ref);
                    } else {
                        return non();
                    }
                }))
                .map(ref -> new PsiReference[]{ref})
                .def(PsiReference.EMPTY_ARRAY);
            return results;
        }
    }
}
