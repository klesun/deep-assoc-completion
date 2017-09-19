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

    private static LookupElement makeLookup(DeepType.Key keyEntry, Project project)
    {
        String type = keyEntry.getTypes().gat(0).map(t -> t.briefType.toString()).def("unknown");
        return makeLookupBase(keyEntry.name, type);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        SearchContext search = new SearchContext().setDepth(35);
        IFuncCtx funcCtx = new FuncCtx(search, L());

        long startTime = System.nanoTime();
        Lang.opt(parameters.getPosition().getParent())
            .thn(literal -> Lang.opt(literal.getParent())
                .fap(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fap(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fap(toCast(PhpExpression.class))
                .map(srcExpr -> funcCtx.findExprType(srcExpr).types)
                .thn(types -> {
                    List<String> options = new ArrayList<>();
                    for (DeepType t: types) {
                        for (String k: t.keys.keySet()) {
                            options.add(k);
                        }
                    }

                    int i = 0;
                    Set<String> suggested = new HashSet<>();
                    for (DeepType type: types) {
                        for (DeepType.Key keyEntry: type.keys.values()) {
                            String key = keyEntry.name;
                            if (suggested.contains(key)) continue;
                            suggested.add(key);

                            if (literal instanceof StringLiteralExpression) {
                                key = "'" + key + "'";
                            }
                            Project project = parameters.getPosition().getProject();
                            LookupElement lookup = makeLookup(keyEntry, project);
                            result.addElement(PrioritizedLookupElement.withPriority(lookup, 3000 - ++i));
                        }
                        L(type.indexTypes).gat(0).flt(t -> type.keys.size() == 0).thn(t -> {
                            for (int k = 0; k < 10; ++k) {
                                result.addElement(makeLookupBase(k + "", t.briefType.toString()));
                            }
                        });
                    }
                    result.addLookupAdvertisement("Press <Page Down> few times to skip built-in suggestions");
                })
                .els(() -> System.out.println("Failed to find declared array keys")));

        long elapsed = System.nanoTime() - startTime;
    }
}
