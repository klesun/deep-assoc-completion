package org.klesun.deep_keys.entry;

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
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.deep_keys.helpers.FuncCtx;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.SearchContext;
import org.klesun.lang.Lang;

import java.util.*;

import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.toCast;

/**
 * contains the completion logic
 */
public class DeepKeysPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookup(DeepType.Key keyEntry, Project project)
    {
        LookupElementBuilder result = LookupElementBuilder.create(keyEntry.name)
            .bold()
            .withIcon(PhpIcons.FIELD);

        if (keyEntry.types.size() > 0) {
            DeepType type = keyEntry.types.get(0);
            result = result.withTypeText(type.briefType.toString());
        } else {
            result = result.withTypeText("unknown");
        }

        return result;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        SearchContext search = new SearchContext().setDepth(20);
        IFuncCtx funcCtx = new FuncCtx(search, L());

        Lang.opt(parameters.getPosition().getParent())
            .thn(literal -> Lang.opt(literal.getParent())
                .fap(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fap(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fap(toCast(PhpExpression.class))
                .map(srcExpr -> funcCtx.findExprType(srcExpr).types)
                .thn(types -> {
                    // TODO: use element type information
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
                    }

                    result.addLookupAdvertisement("Press <End> to skip built-in suggestions");
                })
                .els(() -> System.out.println("Failed to find declared array keys")));
    }
}
