package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.PhpPsiElementImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.resolvers.UsageBasedTypeResolver;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Key;
import org.klesun.lang.*;

import java.util.Set;

import static org.klesun.lang.Lang.*;

/**
 *  \/
 * [''] + zhopa = ['zhopa' => ]
 *
 * if user just started typing the key, there is no => after it, hence IDEA
 * parses it as sequential element - show assoc key options here as well if any
 */
public class ArrCtorIncompleteAssocKeyPvdr extends CompletionProvider<CompletionParameters> {

	private static Opt<ArrayCreationExpression> getArrCtor(StringLiteralExpression lit) {
		return opt(lit.getParent())
			.cst(PhpPsiElementImpl.class)
			.fop(psi -> opt(psi.getParent()))
			.cst(ArrayCreationExpression.class);
	}

	private static It<LookupElementBuilder> makeOptions(Key keyObj) {
		L<DeepType> valtarr = keyObj.getGrantedValues();
		return keyObj.keyType.types.fap((t) -> opt(t.stringValue)
			.flt(strVal -> !t.isNumber)
			.map(strVal -> UsedStrValsPvdr.makeLookupBase(strVal, valtarr, Tls.implode(" ", keyObj.comments).trim())));
	}

	private static void followUpConstructs(InsertionContext ctx) {
		String construct = " => ";
		int pos = ctx.getTailOffset() + 1;
		ctx.getEditor().getDocument().insertString(pos, construct);
		ctx.getEditor().getCaretModel().moveToOffset(pos + construct.length());
	}

	@Override
	protected void addCompletions(
		@NotNull CompletionParameters parameters,
		@NotNull ProcessingContext processingContext,
		@NotNull CompletionResultSet result
	) {
		Mutable<Boolean> hadComments = new Mutable<>(false);
		opt(parameters.getPosition().getParent())
			.cst(StringLiteralExpression.class)
			.fap(lit -> {
				SearchCtx search = new SearchCtx(lit.getProject())
					.setDepth(AssocKeyPvdr.getMaxDepth(
						parameters.isAutoPopup(), lit.getProject()
					));
				FuncCtx funcCtx = new FuncCtx(search);
				IExprCtx exprCtx = new ExprCtx(funcCtx, lit, 0);

				return getArrCtor(lit).fap(arrCtor -> {
					Set<String> alreadyDeclared = UsageBasedTypeResolver.getExplicitKeys(arrCtor, lit);
					return new UsageBasedTypeResolver(exprCtx).resolve(arrCtor)
						.fap(assoct -> assoct.keys)
						.btw(ke -> {
							if (L(ke.comments).str().trim().length() > 0) {
								hadComments.set(true);
							}
						})
						.fap(ArrCtorIncompleteAssocKeyPvdr::makeOptions)
						.flt(lookup -> !alreadyDeclared.contains(lookup.getLookupString()))
						.map(lookup -> lookup.withInsertHandler((ctx, $) -> followUpConstructs(ctx)));
				});
			})
			.map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, 10000 - i * 100))
			.unq(LookupElement::getLookupString)
			.forEach(result::addElement);

		if (hadComments.get()) {
			// note, this character is not a simple space, it's U+2003 EM SPACE (mutton)
			result.addLookupAdvertisement(Tls.repeat(" ", 80 ));
		}
	}
}
