package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.ConstantReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.built_in_typedefs.ArgTypeDefs;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.ScopeFinder;
import org.klesun.lang.It;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;


/**
 * provide completion in built-in function constant argument using the mapping in /built_in_typedefs/
 */
public class ArgCstPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElementBuilder makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(type);
    }

    private static Opt<T2<String, Integer>> assertBuiltInFuncArg(PsiElement caretLeaf)
    {
        return opt(caretLeaf.getParent())
            .cst(ConstantReferenceImpl.class) // IntellijIdeaRulezzz
            .fop(cst -> opt(cst.getParent()))
            .map(lst -> Tls.cast(BinaryExpressionImpl.class, lst) // doStuff(A | B)
                .fop(bin -> opt(bin.getParent())).def(lst))
            .cst(ParameterList.class)
            .fop(cst -> opt(cst.getParent()))
            .cst(FunctionReferenceImpl.class)
            .fop(f -> opt(f.getName())
                .flt(n -> !"".equals(n))
                .map(n -> {
                    int argOrder = It(f.getParameters())
                        .flt(p -> p.getTextOffset() < caretLeaf.getTextOffset())
                        .flt(p -> !ScopeFinder.isPartOf(caretLeaf, p))
                        .arr().size();
                    return T2(n, argOrder);
                }));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        int pos = parameters.getEditor().getCaretModel().getOffset();
        final String text;
        try {
            text = parameters.getOriginalFile().getText();
        } catch (AssertionError exc) {
            // #147 may happen in blade files, I believe only with some
            // custom directives set up, dunno if it is jetbrains bug...
            return;
        }
        String ch = Tls.substr(text, pos - 1, pos);
        PsiElement caretLeaf = parameters.getPosition();
        It<LookupElement> suggestions = assertBuiltInFuncArg(parameters.getPosition())
            .fap(t -> t.nme((funcName, argOrder) -> {
                if (parameters.isAutoPopup() && list(" ", "(").contains(ch)) {
                    // skip all other contributors, since I enabled auto-popup in func args only for my
                    // particular case, not for hundreds of suggestions of all possible values in the scope
                    result.runRemainingContributors(parameters, otherSrc -> {});
                }
                SearchCtx search = new SearchCtx(caretLeaf.getProject())
                    .setDepth(AssocKeyPvdr.getMaxDepth(parameters.isAutoPopup(), caretLeaf.getProject()));
                FuncCtx funcCtx = new FuncCtx(search);
                IExprCtx exprCtx = new ExprCtx(funcCtx, caretLeaf, 0);

                return new ArgTypeDefs(exprCtx.subCtxEmpty())
                    .getArgType(funcName, caretLeaf, argOrder);
            }))
            .fap(t -> t.cstName
                .map(cstName -> makeLookupBase(cstName, t.briefType + "")
                    .withTailText(opt(t.stringValue).map(strVal -> " = " + strVal).def(""), true)));

        suggestions.fch(result::addElement);
    }

    public static boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar)
    {
        if (typeChar == ' ') {
            if (position.getText().equals(",")) {
                return opt(position.getParent())
                    .cst(ParameterList.class)
                    .fop(cst -> opt(cst.getParent()))
                    .cst(FunctionReferenceImpl.class)
                    .any(call -> !"".equals(opt(call.getName()).def("")));
            } else if (position.getText().equals("|")) {
                return opt(position.getParent())
                    .fop(lst -> Tls.cast(BinaryExpressionImpl.class, lst))
                    .fop(cst -> opt(cst.getParent()))
                    .cst(ParameterList.class)
                    .fop(cst -> opt(cst.getParent()))
                    .cst(FunctionReferenceImpl.class)
                    .any(call -> !"".equals(opt(call.getName()).def("")));
            }
        } else if (typeChar == '(') {
            return position.getParent() instanceof ConstantReferenceImpl;
        }
        return false;
    }
}
