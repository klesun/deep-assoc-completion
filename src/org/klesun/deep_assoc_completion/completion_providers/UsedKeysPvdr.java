package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.KeyUsageResolver;
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
            .withIcon(DeepKeysPvdr.getIcon())
            .withTypeText("from usage");
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
        SearchContext search = new SearchContext()
            .setDepth(DeepKeysPvdr.getMaxDepth(parameters));
        FuncCtx funcCtx = new FuncCtx(search);

        long startTime = System.nanoTime();
        L<String> usedKeys = assertArrCtorKey(parameters)
            .fap(arrCtor -> {
                L<String> alreadyDeclared = L(arrCtor.getHashElements())
                    .fop(el -> opt(el.getKey()))
                    .fop(toCast(StringLiteralExpressionImpl.class))
                    .map(lit -> lit.getContents());
                return new KeyUsageResolver(funcCtx, 3).resolve(arrCtor)
                    .flt(k -> !alreadyDeclared.contains(k));
            });

        usedKeys.map(m -> makeLookup(m))
            // to preserve order
            .map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, 3000 - i))
            .fch(result::addElement);

        long elapsed = System.nanoTime() - startTime;
        result.addLookupAdvertisement("Resolved used keys in " + (elapsed / 1000000000.0) + " seconds");
    }
}
