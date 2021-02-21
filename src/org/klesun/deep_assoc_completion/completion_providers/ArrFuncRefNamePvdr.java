package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.It;

import static com.intellij.codeInsight.completion.PrioritizedLookupElement.withPriority;
import static org.klesun.lang.Lang.*;

/**
 * provides completion of method names in `[Some\ClassName::class, '']
 * IDEA already provides such functionality, but only when you pass
 * such array somewhere where it is explicitly said to be `callable`
 * this provider, on the other hand, resolves method  _always_
 */
public class ArrFuncRefNamePvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookup(Method method, boolean isExact)
    {
        LookupElementBuilder looks = LookupElementBuilder.create(method.getName())
            .withBoldness(isExact)
            .withItemTextItalic(!isExact)
            .withTailText(isExact ? "" : " static", true)
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(method.getLocalType(false).filterUnknown().toString());
        if (!isExact) {
            looks = looks
                .withItemTextForeground(JBColor.GRAY);
        }
        return looks;
    }

    /**
     * @return tuples (method, isExact) - when isExact = false, option should be greyed out, such
     * options will include static method call on an instance, but not instance method on a class
     */
    public static It<T2<Method, Boolean>> resolve(StringLiteralExpression literal, boolean isAutoPopup)
    {
        SearchCtx search = new SearchCtx(literal.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(isAutoPopup, literal.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);
        return opt(literal.getParent())
            .map(arrVal -> arrVal.getParent())
            .fop(toCast(ArrayCreationExpressionImpl.class))
            .map(arrCtor -> L(arrCtor.getChildren()))
            .flt(params -> params.size() == 2)
            .flt(params -> literal.isEquivalentTo(params.get(1).getFirstChild()))
            .fop(params -> params.gat(0))
            .fap(clsPsi -> It.cnc(
                ArrCtorRes.resolveClsRefPsiCls(clsPsi)
                    .fap(cls -> It(cls.getMethods()))
                    .flt(meth -> meth.isStatic())
                    .map(meth -> T2(meth, true)),
                new ArrCtorRes(new ExprCtx(funcCtx, clsPsi, 0))
                    .resolveInstPsiCls(clsPsi)
                    .fap(cls -> It(cls.getMethods()))
                    .flt(meth -> meth.getMethodType(false) != Method.MethodType.CONSTRUCTOR)
                    .map(meth -> T2(meth, !meth.isStatic())))
            );
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        if (DumbService.isDumb(parameters.getPosition().getProject())) {
            // following code relies on complex reference resolutions
            // very much, so trying to resolve type during indexing
            // is pointless and is likely to cause exceptions
            return;
        }
        It<T2<Method, Boolean>> methods = opt(parameters.getPosition().getParent())
            .cst(StringLiteralExpressionImpl.class)
            .fap(literal -> resolve(literal, parameters.isAutoPopup()));

        methods
            .map((t, i) -> {
                int priority = t.b ? 200 - i : 100 - i;
                LookupElement looks = makeLookup(t.a, t.b);
                return withPriority(looks, priority);
            })
            .fch(result::addElement);
    }
}
