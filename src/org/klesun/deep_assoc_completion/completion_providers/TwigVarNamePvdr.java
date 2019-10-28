package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.PsiCommentImpl;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.twig.TwigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.deep_assoc_completion.helpers.GuiUtil;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.icons.DeepIcons;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.*;

import javax.swing.*;
import java.net.URL;
import java.util.*;

import static org.klesun.lang.Lang.*;

public class TwigVarNamePvdr extends CompletionProvider<CompletionParameters>
{
    private static It<DeepType> getRootDocTypes(TwigFile f, IExprCtx ctx)
    {
        return It(f.getChildren())
            .cst(PsiCommentImpl.class)
            .fap(cmt -> opt(cmt.getText()))
            .fap(txt -> Tls.regex("^\\{#\\s*data-source\\s*(=\\s*[\\s\\S]+)#\\}$", txt))
            .fap(m -> m.gat(0))
            .fap(eqExpr -> new DocParamRes(ctx).parseEqExpression(eqExpr, f));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        It<LookupElement> options = opt(parameters.getOriginalPosition())
            .fap(caretLeaf -> {
                int depth = AssocKeyPvdr.getMaxDepth(parameters);
                SearchCtx search = new SearchCtx(parameters).setDepth(depth);
                FuncCtx funcCtx = new FuncCtx(search);
                search.isMain = true;
                ExprCtx exprCtx = new ExprCtx(funcCtx, caretLeaf, 0);

                Mt rootMt = opt(caretLeaf.getContainingFile())
                    .cst(TwigFile.class)
                    .fap(f -> getRootDocTypes(f, exprCtx))
                    .wap(Mt::new);

                return rootMt.types
                    .fap(t -> t.keys)
                    .fap(k -> k.keyType.getTypes())
                    .fap(kt -> opt(kt.stringValue))
                    .unq()
                    .map(name -> LookupElementBuilder.create(name)
                        .withIcon(AssocKeyPvdr.getIcon())
                        .withTailText(" = " + rootMt.getKey(name).getBriefValueText(50)));
            });
        options.forEach(result::addElement);
    }
}
