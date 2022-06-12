package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.project.DumbService;
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
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.VarRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.structures.Build;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Key;
import org.klesun.lang.It;
import org.klesun.lang.MemIt;
import org.klesun.lang.Tls;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.deep_assoc_completion.helpers.GuiUtil.runSafeRemainingContributors;
import static org.klesun.lang.Lang.*;

// string literal after `==` like in `$writeSsrRecords[0]['type'] === ''`
// should suggest possible values of 'type'
public class VarNamePvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElementBuilder makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName) // show without $ in popup
            .withLookupString('$' + keyName) // match leaf PSI starting with $
            .withInsertHandler((ctx, lookup) -> {
                // add $ back, as we removed it to show it same way as phpstorm does
                ctx.getEditor().getDocument().insertString(ctx.getStartOffset(), "$");
            })
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(type);
    }

    private static It<LookupElement> makeOptions(It<DeepType> tit)
    {
        return tit.fap(t -> t.keys)
            .fap(k -> k.keyType.getNames()
                .map(strVal -> makeLookupBase(strVal, k.getValueTypes().map(t -> t.briefType).unq().str("|")))
                .map((lookup, i) -> PrioritizedLookupElement.withPriority(lookup, -1000 - i)))
            .unq(LookupElement::getLookupString);
    }

    public static boolean isGlobalContext(Variable caretVar)
    {
        return caretVar.getParent() instanceof Global || !Tls.findParent(caretVar, Function.class, a -> true).has();
    }

    private static It<Variable> getGlobalsMagicVarUsages(Project project)
    {
        if (!CoreProgressManager.getInstance().hasProgressIndicator()) {
            return It.non(); // since 2022 ReferencesSearch only works in explicit actions having
                // a progress indicator, like _ctrl+b_, whereas implicit actions like _ctrl+hover_
                // would cause "Must be executed under progress indicator" errors shown to users
        }
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

    public static It<DeepType> resolveGlobalsMagicVar(IExprCtx funcCtx, Variable caretVar)
    {
        return funcCtx.getProject()
            .flt(proj -> isGlobalContext(caretVar))
            .fap(proj -> {
                if (!funcCtx.getSearch().globalsVarType.has()) {
                    funcCtx.getSearch().globalsVarType = som(new MemIt<>(It.non()));
                    It<DeepType> tit = getGlobalsMagicVarUsages(proj)
                        .fap(glob -> new VarRes(funcCtx.subCtxEmpty()).resolveRef(glob, false))
                        .wap(AssRes::assignmentsToTypes);
                    funcCtx.getSearch().globalsVarType = som(tit.mem());
                }
                return funcCtx.getSearch().globalsVarType.unw();
            });
    }

    private static It<FunctionReference> getCallsBefore(Variable caretVar)
    {
        return Tls.findParent(caretVar, Function.class, a -> true)
            .fap(meth -> opt(meth.getLastChild()))
            .fap(chd -> Tls.findChildren(
                chd,
                FunctionReference.class,
                subPsi -> !(subPsi instanceof Function) &&
                            subPsi.getTextOffset() < caretVar.getTextOffset()
            ));
    }

    private static It<DeepType> resolveExtract(IExprCtx exprCtx, Variable caretVar)
    {
        return getCallsBefore(caretVar)
            .lmt(75)
            .flt(call -> "extract".equals(call.getName()))
            .fap(call -> {
                IExprCtx extractCtx = exprCtx.subCtxDirect(call);
                Mt source = extractCtx.func().getArgMt(0);
                String prefix = extractCtx.func().getArgMt(2).getStringValues().fst().def("");
                return source.types.map(t -> new Build(t.definition, PhpType.ARRAY)
                    .keys(t.keys.fap(k -> {
                        PhpType briefType = k.getBriefTypes().wap(Mt::joinIdeaTypes);
                        return k.keyType.getNames()
                            .map(n -> new Key(prefix + n, k.definition)
                                .addType(() -> Mt.mem(k.getValueTypes()), briefType));
                    }))
                    .get());
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
        if (DumbService.isDumb(parameters.getPosition().getProject())) {
            // following code relies on complex reference resolutions
            // very much, so trying to resolve type during indexing
            // is pointless and is likely to cause exceptions
            return;
        }
        long startTime = System.nanoTime();
        Set<String> suggested = new HashSet();
        // run remaining so that our completions were in the bottom of the list since
        // they are global hence least probably to be needed in a given context
        runSafeRemainingContributors(result, parameters, (fromOtherSrc) -> {
            LookupElement kup = fromOtherSrc.getLookupElement();
            suggested.add(kup.getLookupString());
            result.passResult(fromOtherSrc);
        });
        It<DeepType> tit = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .fop(toCast(VariableImpl.class))
            .fap(lit -> resolve(lit, parameters.isAutoPopup()));

        makeOptions(tit)
            .flt(kup -> !suggested.contains(kup.getLookupString()))
            .forEach(result::addElement);
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
