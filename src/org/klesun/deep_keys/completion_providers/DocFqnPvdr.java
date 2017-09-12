package org.klesun.deep_keys.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_keys.DeepType;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.*;

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

    private Opt<List<String>> extractTypedFqnPart(String docValue, Project project)
    {
        PhpIndex idx = PhpIndex.getInstance(project);
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
                .fap(m -> m.gat(0))
                .map(CamelHumpMatcher::new)
                .map(p -> L(idx.getAllClassNames(p))
                    .map(cls -> cls + "::"))
        ));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        opt(parameters.getPosition().getParent())
            .thn(literal -> extractTypedFqnPart(literal.getText(), literal.getProject())
                .thn(options -> L(options)
                    .map((lookup) -> LookupElementBuilder.create(lookup))
                    .fch(result::addElement))
                .els(() -> System.out.println("No FQN-s found with such partial name - " + literal.getText())));
    }
}
