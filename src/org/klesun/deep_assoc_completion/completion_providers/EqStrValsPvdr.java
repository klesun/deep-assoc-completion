package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Lang;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.lang.Lang.*;

// string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
// should suggest possible values of 'type'
public class EqStrValsPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
                .bold()
                .withIcon(PhpIcons.FIELD)
                .withTypeText(type);
    }

    private static Lang.L<LookupElement> makeOptions(MultiType mt)
    {
        return mt.getStringValues().map(strVal -> makeLookupBase(strVal, "string"));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        SearchContext search = new SearchContext().setDepth(35);
        FuncCtx funcCtx = new FuncCtx(search);

        long startTime = System.nanoTime();
        MultiType mt = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .map(literal -> literal.getParent()) // BinaryExpressionImpl
            .fop(toCast(BinaryExpressionImpl.class))
            .fop(bin -> opt(bin.getOperation())
                .flt(op -> op.getText().equals("==") || op.getText().equals("===")
                        || op.getText().equals("!=") || op.getText().equals("!=="))
                .map(op -> bin.getLeftOperand())
                .fop(toCast(PhpExpression.class))
                .map(exp -> funcCtx.findExprType(exp)))
            .def(MultiType.INVALID_PSI);

        makeOptions(mt).fch(result::addElement);
        long elapsed = System.nanoTime() - startTime;
        result.addLookupAdvertisement("Resolved in " + (elapsed / 1000000000.0) + " seconds");
    }
}
