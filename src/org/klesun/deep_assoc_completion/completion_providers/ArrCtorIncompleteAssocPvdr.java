package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.lang.Lang.opt;
import static org.klesun.lang.Lang.toCast;

/**
 *  \/
 * [''] + zhopa = ['zhopa' => ]
 */
public class ArrCtorIncompleteAssocPvdr extends CompletionProvider<CompletionParameters> {
	@Override
	protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
	{
		long startTime = System.nanoTime();
		It<DeepType> assocTit = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
			.fop(toCast(StringLiteralExpression.class))
			.fap(lit -> resolve(lit, parameters.isAutoPopup()));

		makeOptions(assocTit)
			.forEach(result::addElement);
		long elapsed = System.nanoTime() - startTime;
		double seconds = elapsed / 1000000000.0;
		if (seconds > 0.1) {
			System.out.println("resolved str values in " + seconds + " seconds");
		}
	}
}
