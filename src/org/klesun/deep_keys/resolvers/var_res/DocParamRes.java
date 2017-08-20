package org.klesun.deep_keys.resolvers.var_res;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.Statement;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocParamRes extends Lang
{
    private IFuncCtx ctx;

    public DocParamRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private Opt<MultiType> parseExpression(String expr, Project project)
    {
        expr = "<?php\n" + expr + ";";
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr);

        return opt(psiFile.getFirstChild())
            .fap(toCast(GroupStatement.class))
            .map(gr -> gr.getFirstPsiChild())
            .fap(toCast(Statement.class))
            .map(st -> st.getFirstChild())
            .fap(toCast(PhpExpression.class))
            .map(ex -> ctx.findExprType(ex));
    }

    private Opt<MultiType> parseDoc(String descr, Project project)
    {
        Pattern pattern = Pattern.compile("^\\s*=\\s*(.+)$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(descr);
        if (matcher.matches()) {
            return parseExpression(matcher.group(1), project);
        } else {
            return opt(null);
        }
    }

    public Opt<MultiType> resolve(PhpDocParamTag doc)
    {
        return opt(doc.getTagValue())
            .fap(descr -> parseDoc(descr, doc.getProject()));
    }
}
