package org.klesun.deep_keys.entry;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.lang.Lang;

import java.util.*;

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
        // TODO: support properties like if they were variables

        // TODO: show error when user tries to access not existing
        // key if array definition does not have unknown parts

        // TODO: use phpdocs:
        //   1. Reference to the function that provides such output
        //   2. Relaxed parse of description like ['k1' => 'v1', ...], array('k1' => 'v1', ...)

        // TODO: inspect so that type passed to function was compatible with expected type (has used keys)

        Lang.opt(parameters.getPosition().getParent())
            .thn(literal -> Lang.opt(literal.getParent())
                .fap(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fap(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .map(srcExpr -> DeepTypeResolver.findExprType(srcExpr, 20))
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
