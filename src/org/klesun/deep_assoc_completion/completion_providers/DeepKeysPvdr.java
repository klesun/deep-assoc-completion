package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Lang;

import java.util.*;

import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.toCast;

/**
 * contains the completion logic
 */
public class DeepKeysPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(PhpIcons.FIELD)
            .withTypeText(type);
    }

    private static LookupElement makeLookup(DeepType.Key keyEntry)
    {
        return makeLookupBase(keyEntry.name, new MultiType(keyEntry.getTypes()).getBriefTypeText());
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        SearchContext search = new SearchContext().setDepth(35);
        IFuncCtx funcCtx = new FuncCtx(search, L());

        L<LookupElement> suggestions = L();

        long startTime = System.nanoTime();
        Lang.opt(parameters.getPosition().getParent())
            .thn(literal -> Lang.opt(literal.getParent())
                .fop(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fop(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fop(toCast(PhpExpression.class))
                .map(srcExpr -> funcCtx.findExprType(srcExpr).types)
                .thn(types -> {
                    MultiType mt = new MultiType(types);
                    L<String> keyNames = mt.getKeyNames();
                    L<DeepType> indexTypes = mt.types.fap(t -> t.indexTypes);

                    /** preliminary keys without type - I think they may be considerably faster in some cases */
//                    suggestions.addAll(keyNames
//                        .map(keyName -> makeLookupBase(keyName, "no type info"))
//                        .map((lookup,i) -> PrioritizedLookupElement.withPriority(lookup, 3000 - ++i)));
//                    result.addAllElements(suggestions);

                    suggestions.addAll(keyNames
                        .map(keyName -> makeLookupBase(keyName, mt.getKey(keyName).getBriefTypeText()))
                        .map((lookup,i) -> PrioritizedLookupElement.withPriority(lookup, 3000 - ++i)));

                    if (indexTypes.size() > 0) {
                        String typeText = new MultiType(L(indexTypes)).getBriefTypeText();
                        for (int k = 0; k < 5; ++k) {
                            suggestions.add(makeLookupBase(k + "", typeText));
                        }
                    }
                    long elapsed = System.nanoTime() - startTime;
                    result.addLookupAdvertisement("Inferred in " + elapsed + " nanoseconds");
                })
                .els(() -> result.addLookupAdvertisement("Failed to find declared array keys")));

        result.addAllElements(suggestions);
        Set<String> suggested = new HashSet<>(suggestions.map(l -> l.getLookupString()));

        result.runRemainingContributors(parameters, otherSourceResult -> {
            // remove dupe buil-in suggestions
            LookupElement lookup = otherSourceResult.getLookupElement();
            if (!suggested.contains(lookup.getLookupString())) {
                result.addElement(lookup);
            }
        });
    }
}
