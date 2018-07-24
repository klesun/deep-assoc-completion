package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.*;

import static org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr.getMaxDepth;
import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.list;
import static org.klesun.lang.Lang.opt;

/**
 * provides completion for class/functions inside a @param doc comment
 */
public class DocFqnPvdr extends CompletionProvider<CompletionParameters>
{
    private static String makeMethOrderValue(Method meth)
    {
        String result = "";
        result += meth.getAccess() == PhpModifier.Access.PUBLIC ? "+" : "-";
        return result;
    }

    private Opt<List<String>> extractTypedFqnPart(String docValue, PhpIndex idx)
    {
        return Opt.fst(list(opt(null)
            , Tls.regex(" *= *([A-Z][A-Za-z0-9_]+)::([a-zA-Z0-9_]*?)(IntellijIdeaRulezzz.*)?", docValue)
                // have to complete method
                .map(mtch -> {
                    String clsName = mtch.gat(0).unw();
                    PrefixMatcher metMatcher = new CamelHumpMatcher(mtch.gat(1).unw());
                    return L(idx.getClassesByName(clsName))
                        .cct(L(idx.getInterfacesByName(clsName)))
                        .cct(L(idx.getTraitsByName(clsName)))
                        .flt(cls -> clsName.equals(cls.getName()) ||
                                    clsName.endsWith("\\" + cls.getName()))
                        .fap(cls -> L(cls.getOwnMethods())
                            .srt(m -> makeMethOrderValue(m))
                            .map(m -> m.getName())
                            .flt(p -> metMatcher.prefixMatches(p))
                            .map(f -> f + "()"));
                })
            , Tls.regex(" *= *([A-Z][A-Za-z0-9_]+?)(IntellijIdeaRulezzz.*)?", docValue)
                // have to complete class
                .fop(m -> m.gat(0))
                .map(CamelHumpMatcher::new)
                .map(p -> L(idx.getAllClassNames(p))
                    .map(cls -> cls + "::"))
        ));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        int depth = getMaxDepth(parameters);
        SearchContext search = new SearchContext().setDepth(depth);

        opt(parameters.getPosition().getParent())
            .thn(literal -> {
                String docValue = literal.getText();
                Project project = literal.getProject();
                PhpIndex idx = PhpIndex.getInstance(project);
                // method name completion
                extractTypedFqnPart(docValue, idx)
                    .thn(options -> L(options)
                        .map((lookup) -> LookupElementBuilder.create(lookup)
                            .withIcon(DeepKeysPvdr.getIcon()))
                        .fch(result::addElement))
                    .els(() -> result.addLookupAdvertisement("No FQN-s found with such partial name - " + literal.getText()));

                // assoc array completion
                String prefix = "<?php\n$arg = ";
                Tls.regex("^\\s*=\\s*(.+)$", docValue)
                    .fop(match -> match.gat(0))
                    .map(expr -> prefix + expr + ";")
                    .map(expr -> PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr))
                    .map(file -> file.findElementAt(file.getText().indexOf("IntellijIdeaRulezzz")))
                    .map(psi -> DeepKeysPvdr.resolveAtPsi(psi, search))
                    .fap(mt -> mt.getKeyNames().map(k -> DeepKeysPvdr.makeFullLookup(mt, k)))
                    .fch(result::addElement)
                    ;
            });
    }
}
