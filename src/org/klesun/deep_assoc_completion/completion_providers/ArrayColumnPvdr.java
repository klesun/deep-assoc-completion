package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.lang.Lang.*;

/**
 * provides completion in `array_column($segments, '')`
 */
public class ArrayColumnPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(DeepKeysPvdr.getIcon())
            .withTypeText(type);
    }

    private static LookupElement makeLookup(DeepType.Key keyEntry)
    {
        String type = keyEntry.getTypes().gat(0).map(t -> t.briefType.toString()).def("unknown");
        return makeLookupBase(keyEntry.name, type);
    }

    private static L<LookupElement> makeOptions(MultiType mt)
    {
        L<LookupElement> result = L();
        Set<String> suggested = new HashSet<>();
        for (DeepType type: mt.types) {
            for (DeepType.Key keyEntry: type.keys.values()) {
                String key = keyEntry.name;
                if (suggested.contains(key)) continue;
                suggested.add(key);
                result.add(makeLookup(keyEntry));
            }
            L(type.getElemTypes()).gat(0).flt(t -> type.keys.size() == 0).thn(t -> {
                for (int k = 0; k < 5; ++k) {
                    result.add(makeLookupBase(k + "", t.briefType.toString()));
                }
            });
        }
        return result;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        SearchContext search = new SearchContext()
            .setDepth(DeepKeysPvdr.getMaxDepth(parameters.isAutoPopup()));
        FuncCtx funcCtx = new FuncCtx(search);

        long startTime = System.nanoTime();
        MultiType mt = opt(parameters.getPosition().getParent())
            .map(literal -> literal.getParent())
            .map(literal -> literal.getParent())
            .fop(toCast(FunctionReferenceImpl.class))
            .flt(call -> "array_column".equals(call.getName()))
            .fop(call -> L(call.getParameters()).gat(0))
            .fop(toCast(PhpExpression.class))
            .map(arr -> funcCtx.findExprType(arr).getEl())
            .def(MultiType.INVALID_PSI);

        makeOptions(mt).fch(result::addElement);
        result.addLookupAdvertisement("Press <Page Down> few times to skip built-in suggestions");

        long elapsed = System.nanoTime() - startTime;
        System.out.println("Resolved in " + (elapsed / 1000000000.0) + " seconds");
    }
}
