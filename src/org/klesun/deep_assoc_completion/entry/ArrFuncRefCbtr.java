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
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.toCast;

/**
 * provides method references in such expressions:
 * [SomeClass::class, 'doSomething']
 * [self::class, 'doSomething']
 * [static::class, 'doSomething']
 * [$this, 'doSomething']
 * [$someObj, 'doSomething']
 *
 * phpstorm's contributor only links them when array is passed to
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

        private static PsiReference getCallbackReference(@NotNull ArrayCreationExpression parameter, @NotNull PsiElement element) {
            // mostly copied from PhpReferenceContributor.CallbackReferenceProvider
            PhpCallbackFunctionUtil.PhpCallbackInfoHolder callback = PhpCallbackFunctionUtil.createCallback(parameter);
            if (callback instanceof PhpCallbackFunctionUtil.PhpMemberCallbackInfoHolder) {
                PsiElement classRef = ((PhpCallbackFunctionUtil.PhpMemberCallbackInfoHolder)callback).getClassElement();
                if (callback.getCallbackElement() == element) {
                    return PhpCallbackReferenceBase.createMemberReference(classRef, callback.getCallbackElement(), true);
                }
            }
            return null;
        }

        @NotNull
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            return Tls.cast(StringLiteralExpression.class, element)
                .map(lit -> lit.getParent()) // array value
                .map(val -> val.getParent())
                .fop(toCast(ArrayCreationExpression.class))
                .flt(arr -> {
                    L<PsiElement> vals = L(arr.getChildren());
                    return !vals.any(v -> v instanceof ArrayHashElement)
                        && vals.size() == 2
                        && element.isEquivalentTo(vals.get(1).getFirstChild())
                        && !(vals.get(1) instanceof StringLiteralExpression);
                })
                .map(arr -> getCallbackReference(arr, element))
                .map(ref -> new PsiReference[]{ref})
                .def(PsiReference.EMPTY_ARRAY);
        }
    }
}
