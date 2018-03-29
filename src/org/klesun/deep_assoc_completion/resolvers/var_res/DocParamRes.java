package org.klesun.deep_assoc_completion.resolvers.var_res;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.Statement;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocParamRes extends Lang
{
    private FuncCtx ctx;

    public DocParamRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private static Opt<MultiType> parseExpression(String expr, Project project, FuncCtx docCtx)
    {
        // adding "$arg = " so anonymous functions were parsed as expressions
        expr = "<?php\n$arg = " + expr + ";";
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr);
        return opt(psiFile.getFirstChild())
            .fop(toCast(GroupStatement.class))
            .map(gr -> gr.getFirstPsiChild())
            .fop(toCast(Statement.class))
            .map(st -> st.getFirstChild())
            .fop(toCast(PhpExpression.class))
            .map(ex -> docCtx.findExprType(ex));
    }

    private Opt<MultiType> parseDoc(PhpDocTag doc, Project project)
    {
        Pattern pattern = Pattern.compile("^\\s*=\\s*(.+)$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(doc.getTagValue());
        FuncCtx docCtx = new FuncCtx(ctx.getSearch());
        docCtx.fakeFileSource = opt(doc);
        if (matcher.matches()) {
            return parseExpression(matcher.group(1), project, docCtx);
        } else {
            return opt(null);
        }
    }

    public Opt<MultiType> resolve(PhpDocTag doc)
    {
        return parseDoc(doc, doc.getProject());
    }
}
