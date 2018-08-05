package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.tags.PhpDocReturnTagImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.*;

import static org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr.getMaxDepth;
import static org.klesun.lang.Lang.*;

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

    private Opt<List<String>> extractTypedFqnPart(String docValue, Project project, PsiElement docPsi)
    {
        Opt<PhpClass> caretClass = Tls.findParent(docPsi, PhpClass.class, a -> true);
        PhpIndex idx = PhpIndex.getInstance(project);
        return Opt.fst(list(opt(null)
            , Tls.regex(" *([A-Za-z][A-Za-z0-9_]+)::([a-zA-Z0-9_]*?)(IntellijIdeaRulezzz.*)?", docValue)
                // have to complete method
                .map(mtch -> {
                    String clsName = mtch.gat(0).unw();
                    PrefixMatcher metMatcher = new CamelHumpMatcher(mtch.gat(1).unw());
                    return L(idx.getClassesByName(clsName))
                        .cct(L(idx.getInterfacesByName(clsName)))
                        .cct(L(idx.getTraitsByName(clsName)))
                        .cct(caretClass.fap(cls -> list(cls)))
                        .flt(cls -> clsName.equals(cls.getName()) ||
                                    clsName.endsWith("\\" + cls.getName()) ||
                                    (clsName.equals("self") || clsName.equals("static")) &&
                                    cls.isEquivalentTo(caretClass.def(null)))
                        .fap(cls -> L(cls.getOwnMethods())
                            .srt(m -> makeMethOrderValue(m))
                            .map(m -> m.getName())
                            .flt(p -> metMatcher.prefixMatches(p))
                            .map(f -> f + "()"));
                })
            , Tls.regex(" *([A-Za-z][A-Za-z0-9_]+?)(IntellijIdeaRulezzz.*)?", docValue)
                // have to complete class
                .fop(m -> m.gat(0))
                .map(CamelHumpMatcher::new)
                .map(p -> L(idx.getAllClassNames(p))
                    .cct(list("self", "static"))
                    .map(cls -> cls + "::"))
        ));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        int depth = getMaxDepth(parameters);
        SearchContext search = new SearchContext(parameters).setDepth(depth);

        opt(parameters.getPosition().getParent())
            .thn(tagValue -> {
                String docValue = tagValue.getText();
                Project project = tagValue.getProject();

                String prefix = "<?php\n$arg = ";
                String regex = "^\\s*=\\s*(.+)$";
                if (tagValue.getParent() instanceof PhpDocReturnTagImpl) {
                    PhpDocReturnTagImpl returnTag = (PhpDocReturnTagImpl)tagValue.getParent();
                    regex = "^\\s*(?:like|=|)\\s*(.+)$";
                    if (docValue.matches("::[a-zA-Z0-9_]*IntellijIdeaRulezzz")) {
                        // class name gets resolved as return type psi
                        String typePart = L(returnTag.getChildren())
                            .fop(toCast(PhpDocType.class))
                            .map(typ -> typ.getText())
                            .wap(parts -> Tls.implode("", parts));
                        docValue = typePart + docValue;
                    }
                }
                FuncCtx ctx = new FuncCtx(search);
                ctx.fakeFileSource = opt(tagValue);
                Tls.regex(regex, docValue)
                    .fop(match -> match.gat(0))
                    // method name completion
                    .thn(expr -> extractTypedFqnPart(expr, project, tagValue)
                        .thn(options -> L(options)
                            .map((lookup) -> LookupElementBuilder.create(lookup)
                                .withIcon(DeepKeysPvdr.getIcon()))
                            .fch(result::addElement)))
                    // assoc array completion
                    .map(expr -> prefix + expr + ";")
                    .map(expr -> PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr))
                    .map(file -> file.findElementAt(file.getText().indexOf("IntellijIdeaRulezzz")))
                    .map(psi -> DeepKeysPvdr.resolveAtPsi(psi, ctx))
                    .fap(mt -> mt.getKeyNames().map(k -> DeepKeysPvdr.makeFullLookup(mt, k)))
                    .fch(result::addElement)
                    ;
            });
    }
}
