package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.Opt;

import static org.klesun.lang.Lang.*;

/**
 * provides completion of method names in `[Some\ClassName::class, '']`
 */
public class ArrFuncRefPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookup(Method method)
    {
        return LookupElementBuilder.create(method.getName())
            .bold()
            .withIcon(PhpIcons.METHOD)
            .withTypeText(method.getLocalType(false).toString());
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        L<Method> methods = opt(parameters.getPosition().getParent())
            .fap(literal -> opt(literal.getParent())
                .map(arrVal -> arrVal.getParent())
                .fap(toCast(ArrayCreationExpressionImpl.class))
                .map(arrCtor -> L(arrCtor.getChildren()))
                .flt(params -> params.size() == 2)
                .flt(params -> literal.isEquivalentTo(params.get(1).getFirstChild()))
                .fap(params -> params.gat(0))
                .fap(clsPsi -> Opt.fst(list(
                    ArrCtorRes.resolveClass(clsPsi)
                        .map(cls -> L(cls.getMethods())
                            .flt(meth -> meth.isStatic())),
                    ArrCtorRes.resolveInstance(clsPsi)
                        .map(cls -> L(cls.getMethods())
                            .flt(meth -> meth.getMethodType(false) != Method.MethodType.CONSTRUCTOR)
                            .flt(meth -> !meth.isStatic())))
                )))
            .def(L());

        methods.map(m -> makeLookup(m)).fch(result::addElement);
        result.addLookupAdvertisement("Press <Page Down> few times to skip built-in suggestions");

        long elapsed = System.nanoTime() - startTime;
        System.out.println("Resolved in " + (elapsed / 1000000000.0) + " seconds");
    }
}
