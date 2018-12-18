package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.Global;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.VarRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;

// string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
// should suggest possible values of 'type'
public class VarNamePvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElementBuilder makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(type);
    }

    private static It<LookupElement> makeOptions(It<DeepType> tit)
    {
        return tit.fap(t -> t.keys)
            .fap(k -> k.keyType.getNames()
                .map(strVal -> makeLookupBase("$" + strVal, k.getTypes().map(t -> t.briefType).unq().str("|")))
                .map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, -1000 - i)))
            .unq(l -> l.getLookupString());
    }

    private static boolean isGlobalContext(Variable caretVar)
    {
        return caretVar.getParent() instanceof Global || !Tls.findParent(caretVar, Function.class, a -> true).has();
    }

    private static It<Variable> getGlobalsMagicVarUsages(Project project)
    {
        try {
            return It(PhpIndex.getInstance(project).getVariablesByName("GLOBALS")).fst()
                .fap(globVar -> ReferencesSearch.search(globVar, GlobalSearchScope.allScope(globVar.getProject()), false))
                .fap(ref -> opt(ref.getElement()))
                .cst(Variable.class);
        } catch (Throwable exc) {
            // from my past experience with ReferencesSearch, it is
            // likely to throw random exceptions and cause random hangs...
            return It.non();
        }
    }

    private static It<DeepType> resolveGlobalsMagicVar(IExprCtx funcCtx, Variable caretVar)
    {
        return funcCtx.getProject()
            .flt(proj -> isGlobalContext(caretVar))
            .fap(proj -> getGlobalsMagicVarUsages(proj))
            .fap(glob -> new VarRes(funcCtx.subCtxEmpty()).resolveRef(glob, false))
            .wap(asses -> AssRes.assignmentsToTypes(asses));
    }

    private static It<FunctionReference> getCalsBefore(Variable caretVar)
    {
        return Tls.findParent(caretVar, Function.class, a -> true)
            .fap(meth -> Tls.findChildren(
                meth.getLastChild(),
                FunctionReference.class,
                subPsi -> !(subPsi instanceof Function) &&
                            subPsi.getTextOffset() < caretVar.getTextOffset()
            ));
    }

    private static It<DeepType> resolveExtract(IExprCtx exprCtx, Variable caretVar)
    {
        return getCalsBefore(caretVar)
            .lmt(75)
            .flt(call -> "extract".equals(call.getName()))
            .fap(call -> {
                IExprCtx extractCtx = exprCtx.subCtxDirect(call);
                Mt source = extractCtx.func().getArgMt(0);
                String prefix = extractCtx.func().getArgMt(2).getStringValues().fst().def("");
                return source.types.map(t -> {
                    DeepType resultt = new DeepType(t.definition, PhpType.ARRAY);
                    t.keys.fch(k -> k.keyType.getNames()
                        .fch(n -> resultt.addKey(prefix + n, k.definition)
                            .addType(() -> new Mt(k.getTypes()), k.getBriefTypes().wap(Mt::joinIdeaTypes))));
                    return resultt;
                });
            });
    }

    /** @return type of an associative array with vars to suggest as keys */
    private static It<DeepType> resolve(VariableImpl caretVar, boolean isAutoPopup)
    {
        SearchCtx search = new SearchCtx(caretVar.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(isAutoPopup, caretVar.getProject()));
        if (isAutoPopup) {
            // it would be sad if it deeply scanned global
            // vars in whole project when you just type a var
            search.setDepth(Math.min(search.maxDepth, 15));
        }
        FuncCtx funcCtx = new FuncCtx(search);
        IExprCtx exprCtx = new ExprCtx(funcCtx, caretVar, 0);

        return It.cnc(
            resolveGlobalsMagicVar(exprCtx, caretVar),
            resolveExtract(exprCtx, caretVar)
        );
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        // run remaining so that our completions were in the bottom of the list since
        // they are global hence least probably to be needed in a given context
        result.runRemainingContributors(parameters, true);
        It<DeepType> tit = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .fop(toCast(VariableImpl.class))
            .fap(lit -> resolve(lit, parameters.isAutoPopup()));

        makeOptions(tit).fch(lookupElement -> result.addElement(lookupElement));
        long elapsed = System.nanoTime() - startTime;
        double seconds = elapsed / 1000000000.0;
        if (seconds > 0.1) {
            System.out.println("resolved str values in " + seconds + " seconds");
        }
    }

    // ================================
    //  GotoDeclarationHandler part follows
    // ================================

    public static It<PsiElement> resolveDeclPsis(@NotNull PsiElement psi, int mouseOffset)
    {
        return opt(psi.getParent())
            .fop(toCast(VariableImpl.class))
            .fap(lit -> resolve(lit, false)
                .fap(globt -> globt.keys)
                .flt(k -> k.keyType.getNames().any(n -> n.equals(lit.getName())))
                .map(t -> t.definition));
    }
}
