package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.tags.PhpDocReturnTagImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.HashSet;

import static org.klesun.deep_assoc_completion.completion_providers.AssocKeyPvdr.getMaxDepth;
import static org.klesun.lang.Lang.*;

/**
 * provides completion for class/functions inside a @param doc comment
 */
public class DocFqnPvdr extends CompletionProvider<CompletionParameters>
{
    final private static String regex = "^\\s*=\\s*(.+)$";
    final private static String returnRegex = "^\\s*(?:like|=|)\\s*(.+)$";

    private static String makeMethOrderValue(Method meth)
    {
        String result = "";
        result += meth.getAccess() == PhpModifier.Access.PUBLIC ? "+" : "-";
        return result;
    }

    private Opt<It<String>> extractTypedFqnPart(String docValue, Project project, PsiElement docPsi)
    {
        Opt<PhpClass> caretClass = Tls.findParent(docPsi, PhpClass.class, a -> true);
        PhpIndex idx = PhpIndex.getInstance(project);
        return Opt.fst(() -> opt(null)
            , () -> Tls.regex("(?:|.*\\(|.*\\[\\s*|.*=>\\s*) *([A-Za-z][A-Za-z0-9_]+)::([a-zA-Z0-9_]*?)(IntellijIdeaRulezzz.*)?", docValue)
                // have to complete method
                .map(mtch -> {
                    String clsName = mtch.gat(0).unw();
                    PrefixMatcher metMatcher = new CamelHumpMatcher(mtch.gat(1).unw());
                    return It.cnc(
                        idx.getClassesByName(clsName),
                        idx.getInterfacesByName(clsName),
                        idx.getTraitsByName(clsName),
                        caretClass.fap(cls -> list(cls))
                    ).flt(cls -> clsName.equals(cls.getName()) ||
                        clsName.endsWith("\\" + cls.getName()) ||
                        (clsName.equals("self") || clsName.equals("static")) &&
                        cls.isEquivalentTo(caretClass.def(null))
                    ).fap(cls -> L(cls.getOwnMethods())
                        .srt(m -> makeMethOrderValue(m))
                        .map(m -> m.getName())
                        .flt(p -> metMatcher.prefixMatches(p))
                        .map(f -> f + "()"));
                })
            , () -> Tls.regex("(|.*new\\s+|.*\\(|.*\\[\\s*|.*=>\\s*) *([A-Z][A-Za-z0-9_]+?)(IntellijIdeaRulezzz.*)?", docValue)
                // have to complete class
                .map(m -> {
                    String stopLexeme = m.get(0);
                    String className = m.get(1);
                    CamelHumpMatcher matcher = new CamelHumpMatcher(className);
                    return It.cnc(
                        idx.getAllClassNames(matcher),
                        list("self", "static")
                    ).map(cls -> cls + (stopLexeme.endsWith("new ") ? "()" : "::"));
                })
        );
    }

    private It<LookupElement> parseDocValue(String docValue, SearchCtx search, PsiElement tagValue, String regex)
    {
        FuncCtx ctx = new FuncCtx(search);
        ctx.fakeFileSource = opt(tagValue);
        IExprCtx exprCtx = new ExprCtx(ctx, tagValue, 0);
        return Tls.regex(regex, docValue)
            .fop(match -> match.gat(0))
            .fap(expr -> {
                String fakeFileText = DocParamRes.EXPR_PREFIX + expr + DocParamRes.EXPR_POSTFIX;
                Opt<PsiFile> fakeFileOpt = opt(PsiFileFactory.getInstance(tagValue.getProject())
                    .createFileFromText(PhpLanguage.INSTANCE, fakeFileText));
                return It.cnc(
                    // method name completion
                    extractTypedFqnPart(expr, tagValue.getProject(), tagValue)
                        .fap(options -> options)
                        .map((lookup) -> LookupElementBuilder.create(lookup)
                            .withIcon(AssocKeyPvdr.getIcon())),
                    // assoc array completion
                    fakeFileOpt
                        .map(file -> file.findElementAt(file.getText().indexOf("IntellijIdeaRulezzz")))
                        .map(psi -> AssocKeyPvdr.resolveAtPsi(psi, exprCtx).wap(Mt::mem))
                        .fap(mt -> mt.getKeyNames().map(k -> AssocKeyPvdr.makeFullLookup(mt, k, non())))
                );
            });
    }

    private It<LookupElement> parseTagValue(PsiElement tagValue, SearchCtx search)
    {
        String docValue = tagValue.getText();
        String regex = DocFqnPvdr.regex;
        if (tagValue.getParent() instanceof PhpDocReturnTagImpl) {
            PhpDocReturnTagImpl returnTag = (PhpDocReturnTagImpl)tagValue.getParent();
            L<PhpDocType> docTypes = It(returnTag.getChildren())
                .fop(toCast(PhpDocType.class)).arr();
            regex = returnRegex;
            if (docValue.matches("::[a-zA-Z0-9_]*IntellijIdeaRulezzz")) {
                // class name gets resolved as return type psi
                String typePart = docTypes
                    .map(typ -> typ.getText())
                    .wap(parts -> Tls.implode("", parts));
                docValue = typePart + docValue;
            } else if (docTypes.size() == 0 || tagValue instanceof PhpDocType) {
                // do not suggest class in @return start, since IDEA already does that
                regex = "^\\b$"; // regex that will match nothing
            }
        }
        return parseDocValue(docValue, search, tagValue, regex);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        int depth = getMaxDepth(parameters);
        SearchCtx search = new SearchCtx(parameters).setDepth(depth);

        opt(parameters.getPosition().getParent())
            .fap(tagValue -> parseTagValue(tagValue, search))
            .fch(result::addElement);
    }

    public void addCompletionsMultiline(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        Opt<PhpDocTag> docTagOpt = Tls.findPrevSibling(parameters.getPosition(), PhpDocTag.class);
        docTagOpt.thn(docTag -> {
            String regex = DocFqnPvdr.regex;
            if (docTag instanceof PhpDocReturnTagImpl) {
                regex = returnRegex;
            }
            SearchCtx search = new SearchCtx(parameters).setDepth(getMaxDepth(parameters));
            parseDocValue(docTag.getTagValue(), search, docTag, regex)
                .fch(result::addElement);
        });
    }
}
