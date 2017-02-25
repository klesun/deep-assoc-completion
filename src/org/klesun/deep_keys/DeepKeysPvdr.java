package org.klesun.deep_keys;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;
import org.klesun.lang.shortcuts.F;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * contains the completion logic
 */
public class DeepKeysPvdr extends CompletionProvider<CompletionParameters>
{
    /** i HATE writing "new " before every usage! */
    private static <T> Opt<T> opt(T value)
    {
        return new Opt(value);
    }

    private static <T, Tin extends PsiElement> F<Tin, Opt<T>> toCast(Class<T> cls)
    {
        return obj -> Tls.cast(cls, obj);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        // TODO: support lists of associative arrays
        // (like when parser returns segments and you iterate through them)

        // TODO: support lambdas (array_map, array_filter, etc...)

        // TODO: support properties like if they were variables

        // TODO: go to definition

        // TODO: show error when user tries to access not existing key

        // TODO: use phpdocs:
        //   1. Reference to the function that provides such output
        //   2. Relaxed parse of description like ['k1' => 'v1', ...], array('k1' => 'v1', ...)

        // TODO: autocomplete through yield?

        // TODO: autocomplete from XML file with payload example ($xml['body'][0]['main'][0][...])

        // TODO: inspect so that type passed to function was compatible with expected type (has used keys)

        opt(parameters.getPosition().getParent())
            .thn(literal -> opt(literal.getParent())
                .fap(toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fap(toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .map(srcExpr -> DeepTypeResolver.findExprType(srcExpr, 30))
                .thn(types -> {
                    // TODO: use element type information
                    List<String> options = new ArrayList<>();
                    for (DeepType t: types) {
                        for (String k: t.keys.keySet()) {
                            options.add(k);
                        }
                    }

                    if (!(literal instanceof StringLiteralExpression)) {
                        // add quotes if not inside quotes
                        options = options.stream()
                            .map(o -> "'" + o + "'")
                            .collect(Collectors.toList());
                    }

                    int i = 0;
                    Set<String> suggested = new HashSet<>();
                    for (String key: options) {
                        if (suggested.contains(key)) continue;
                        suggested.add(key);
                        result.addElement(
                            PrioritizedLookupElement.withPriority(
                                LookupElementBuilder.create(key)
                                    .bold()
                                    .withTailText("(assoc.)"),
                                300000 + --i
                            )
                        );
                    }

                    result.addLookupAdvertisement("Press <End> to skip built-in suggestions");
                })
                .els(() -> System.out.println("Failed to find declared array keys")));
    }
}
