package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
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

    private static L<String> resolveReplaceKeys(ParameterList argList, int order)
    {
        SearchContext search = new SearchContext();
        FuncCtx ctx = new FuncCtx(search, list());
        return L(argList.getParameters())
            .flt((psi, i) -> i < order)
            .fop(toCast(PhpExpression.class))
            .fap(exp -> ctx.findExprType(exp).getKeyNames());

    }

    private static L<ArrayIndex> findUsedIndexes(Function meth, String varName)
    {
        return Tls.findChildren(
            meth.getLastChild(),
            ArrayAccessExpressionImpl.class,
            subPsi -> !(subPsi instanceof FunctionImpl)
        )
            .fop(acc -> opt(acc.getValue())
                .fop(toCast(VariableImpl.class))
                .flt(varUsage -> varName.equals(varUsage.getName()))
                .map(varUsage -> acc.getIndex()));
    }

    private static L<String> resolveArgUsedKeys(Function meth, int argOrder)
    {
        SearchContext fakeSearch = new SearchContext();
        FuncCtx fakeCtx = new FuncCtx(fakeSearch, L());
        return L(meth.getParameters()).gat(argOrder)
            .fop(toCast(ParameterImpl.class))
            .map(arg -> list(
                findUsedIndexes(meth, arg.getName())
                    .map(idx -> idx.getValue())
                    .fop(toCast(StringLiteralExpressionImpl.class))
                    .map(lit -> lit.getContents()),
                opt(arg.getDocComment())
                    .map(doc -> doc.getParamTagByName(arg.getName()))
                    .fop(doc -> new DocParamRes(fakeCtx).resolve(doc))
                    .map(mt -> mt.getKeyNames())
                    .def(L())
            ).fap(a -> a))
            .def(L());
    }

    private static Opt<Function> resolveFunc(ParameterList argList)
    {
        return opt(argList.getParent())
            .fop(par -> Opt.fst(list(
                Tls.cast(FunctionReference.class, par)
                    .map(call -> call.resolve()),
                Tls.cast(MethodReferenceImpl.class, par)
                    .map(call -> call.resolve()),
                Tls.cast(NewExpressionImpl.class, par)
                    .map(newEx -> newEx.getClassReference())
                    .map(ref -> ref.resolve())
            ))  .fop(toCast(Function.class)));
    }

    private static Opt<ArrayCreationExpression> assertArrCtorKey(CompletionParameters parameters)
    {
        return opt(parameters.getPosition().getParent())
            .fop(literal -> opt(literal.getParent())
                .map(arrKey -> arrKey.getParent())
                .fop(parent -> Opt.fst(list(
                    // assoc key
                    Tls.cast(ArrayHashElementImpl.class, parent)
                        .flt(hash -> literal.isEquivalentTo(hash.getKey()))
                        .map(hash -> hash.getParent())
                        .fop(toCast(ArrayCreationExpression.class)),
                    // indexed value (that may become associative key as user continues typing)
                    Tls.cast(ArrayCreationExpression.class, parent)
                ))));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        L<String> usedKeys = assertArrCtorKey(parameters)
            // TODO: handle nested arrays
            // TODO: handle assignments of array to a variable passed to the func further
            // assuming it may be used only in a function call for now
            .fap(arrCtor -> {
                L<String> alreadyDeclared = L(arrCtor.getHashElements())
                    .fop(el -> opt(el.getKey()))
                    .fop(toCast(StringLiteralExpressionImpl.class))
                    .map(lit -> lit.getContents());
                return opt(arrCtor.getParent())
                    .fop(toCast(ParameterList.class))
                    .fap(argList -> {
                        int order = L(argList.getParameters()).indexOf(arrCtor);
                        return resolveFunc(argList)
                            .fap(meth -> list(
                                resolveArgUsedKeys(meth, order),
                                opt(meth.getName())
                                    .flt(n -> n.equals("array_merge") || n.equals("array_replace"))
                                    .fap(n -> resolveReplaceKeys(argList, order))
                            ).fap(a -> a));
                    })
                    .flt(k -> !alreadyDeclared.contains(k))
                    ;
            });

        usedKeys.map(m -> makeLookup(m))
            // to preserve order
            .map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, 3000 - i))
            .fch(result::addElement);
        result.addLookupAdvertisement("Press <Page Down> few times to skip built-in suggestions");

        long elapsed = System.nanoTime() - startTime;
        System.out.println("Resolved used keys in " + (elapsed / 1000000000.0) + " seconds");
    }
}
