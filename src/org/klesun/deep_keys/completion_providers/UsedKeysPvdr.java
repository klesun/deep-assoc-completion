package org.klesun.deep_keys.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_keys.helpers.FuncCtx;
import org.klesun.deep_keys.helpers.SearchContext;
import org.klesun.deep_keys.resolvers.var_res.ArgRes;
import org.klesun.deep_keys.resolvers.var_res.DocParamRes;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;

/**
 * provides completion of keys in `$someVar = ['' => 123]`
 * by looking at what keys are used in the `$someVar`
 * made first of all for cases when function takes an associative array and it
 * is a pain to collect key names one-by-one from the function implementation
 */
public class UsedKeysPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookup(String keyName)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(PhpIcons.PARAMETER)
            .withTypeText("from usage");
    }

    private static L<ArrayIndex> findUsedIndexes(Method meth, String varName)
    {
        return Tls.findChildren(
            meth.getLastChild(),
            ArrayAccessExpressionImpl.class,
            subPsi -> !(subPsi instanceof FunctionImpl)
        )
            .fop(acc -> opt(acc.getValue())
                .fap(toCast(VariableImpl.class))
                .flt(varUsage -> varName.equals(varUsage.getName()))
                .map(varUsage -> acc.getIndex()));
    }

    private static L<String> resolveArgUsedKeys(Method meth, int argOrder)
    {
        SearchContext fakeSearch = new SearchContext();
        FuncCtx fakeCtx = new FuncCtx(fakeSearch, L());
        return L(meth.getParameters()).gat(argOrder)
            .fap(toCast(ParameterImpl.class))
            // TODO: include both keys from the doc and keys from the usage
            .map(arg -> {
                return list(
                    findUsedIndexes(meth, arg.getName())
                        .map(idx -> idx.getValue())
                        .fop(toCast(StringLiteralExpressionImpl.class))
                        .map(lit -> lit.getContents()),
                    opt(arg.getDocComment())
                        .map(doc -> doc.getParamTagByName(arg.getName()))
                        .fap(doc -> new DocParamRes(fakeCtx).resolve(doc))
                        .map(mt -> mt.getKeyNames())
                        .def(L())
                ).fap(a -> a);
            })
            .def(L());
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        L<String> usedKeys = opt(parameters.getPosition().getParent())
            .fap(literal -> opt(literal.getParent())
                .map(arrKey -> arrKey.getParent())
                .fap(parent -> Opt.fst(list(
                    // assoc key
                    Tls.cast(ArrayHashElementImpl.class, parent)
                        .flt(hash -> literal.isEquivalentTo(hash.getKey()))
                        .map(hash -> hash.getParent())
                        .fap(toCast(ArrayCreationExpressionImpl.class)),
                    // indexed value (that may become associative key as user continues typing)
                    Tls.cast(ArrayCreationExpressionImpl.class, parent)
                ))))
            // TODO: handle nested arrays
            // TODO: handle assignments of array to a variable passed to the func further
            // assuming it may be used only in a function call for now
            .fap(arrCtor -> opt(arrCtor.getParent())
                .fap(toCast(ParameterListImpl.class))
                .fap(argList -> {
                    int order = L(argList.getParameters()).indexOf(arrCtor);
                    L<String> alreadyDeclared = L(arrCtor.getHashElements())
                        .fop(el -> opt(el.getKey()))
                        .fop(toCast(StringLiteralExpressionImpl.class))
                        .map(lit -> lit.getContents());
                    return opt(argList.getParent())
                        .fap(toCast(MethodReferenceImpl.class))
                        .flt(call -> order > -1)
                        .map(call -> call.resolve())
                        .fap(toCast(MethodImpl.class))
                        .map(meth -> resolveArgUsedKeys(meth, order)
                            .flt(k -> !alreadyDeclared.contains(k)));
                }))
            .def(L());

        usedKeys.map(m -> makeLookup(m))
            // to preserve order
            .map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, 3000 - i))
            .fch(result::addElement);
        result.addLookupAdvertisement("Press <Page Down> few times to skip built-in suggestions");

        long elapsed = System.nanoTime() - startTime;
        System.out.println("Resolved used keys in " + (elapsed / 1000000000.0) + " seconds");
    }
}
