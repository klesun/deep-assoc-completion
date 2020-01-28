package org.klesun.deep_assoc_completion.entry;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpCallbackFunctionUtil;
import static com.jetbrains.php.lang.PhpCallbackReferenceBase.PhpClassMemberCallbackReference;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ClassReferenceImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.Collection;

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
            It<T2<Boolean, String>> clsFqns = It.frs(
                () -> It(ArrCtorRes.ideaTypeToFqn(clsRef.getType())).map(fqn -> T2(false, fqn)),
                () -> Tls.cast(ClassConstantReference.class, clsRef)
                    .flt(cstRef -> clsRef.getText().endsWith("::class"))
                    .fap(cstRef -> opt(cstRef.getClassReference()))
                    .cst(ClassReferenceImpl.class)
                    .fap(clsPart -> opt(clsPart.getFQN()))
                    .map(fqn -> T2(true, fqn))
            );
            return clsFqns
                .fap(t -> t.nme((isStatic, clsPath) -> {
                    PhpIndex idx = PhpIndex.getInstance(clsRef.getProject());
                    Collection<PhpClass> clsPsis = idx.getAnyByFQN(clsPath);
                    return It(clsPsis)
                        .fap(clsPsi -> clsPsi.getMethods())
                        .flt(meth -> {
                            boolean isPossible = meth.isStatic() || !isStatic;
                            return methName.equals(meth.getName())
                                && isPossible;
                        });
                }))
                .has();
        }

        @NotNull
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            PsiReference[] results = Tls.cast(StringLiteralExpression.class, element)
                .map(lit -> lit.getParent()) // array value
                .map(val -> val.getParent())
                .fop(toCast(ArrayCreationExpression.class))
                .map(arr -> L(arr.getChildren()))
                .flt(valsPsis -> valsPsis.size() == 2 &&
                    element.isEquivalentTo(valsPsis.get(1).getFirstChild()) &&
                    !valsPsis.any(v -> v instanceof ArrayHashElement))
                // phpstorm wraps each value, probably due to assoc/ordered array syntax ambiguation
                .fop(valsPsis -> valsPsis.fal(psi -> opt(psi.getFirstChild())))
                .flt(vals ->  !(vals.get(0) instanceof StringLiteralExpression))
                .fop(vals -> {
                    Opt<PhpExpression> clsRefOpt = vals.gat(0)
                        .cst(PhpExpression.class);
                    Opt<StringLiteralExpression> methLitOpt = vals.gat(1)
                        .cst(StringLiteralExpression.class);
                    return T2.all(clsRefOpt, methLitOpt);
                })
                .fop(t -> t.nme((clsRef, methLit) -> {
                    if (hasMethod(clsRef, methLit)) {
                        PhpClassMemberCallbackReference ref = new PhpClassMemberCallbackReference(clsRef, methLit, true);
                        return som(ref);
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
