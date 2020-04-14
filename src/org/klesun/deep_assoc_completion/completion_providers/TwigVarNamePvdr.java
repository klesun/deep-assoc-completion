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
import com.jetbrains.twig.elements.TwigCompositeElement;
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

    private static L<T2<String, String>> getParentLoops(PsiElement caretLeaf)
    {
        // el var name, arr var name tuples
        L<T2<String, String>> fchs = L();
        Opt<PsiElement> parentOpt = opt(caretLeaf.getParent());
        while (parentOpt.has()) {
            PsiElement parent = parentOpt.unw();
            // ignoring the key as it is not of use for us for now
            Opt<L<String>> fchMatch = Tls.regex("^\\{\\%\\s*for\\s+(?:\\w+\\s*,\\s*)?(\\w+)\\s*in\\s*(\\w+).*", parent.getText());
            if (fchMatch.has()) {
                L<String> m = fchMatch.unw();
                fchs.add(T2(m.get(0), m.get(1)));
            }
            parentOpt = opt(parent.getParent());
        }
        return fchs;
    }

    private static It<DeepType> resolveVar(String varName, L<T2<String, String>> parentLoops, Mt rootMt)
    {
        for (int i = 0; i < parentLoops.size(); ++i) {
            String itemVar = parentLoops.get(i).a;
            String arrVar = parentLoops.get(i).b;
            if (itemVar.equals(varName)) {
                return resolveVar(arrVar, parentLoops.sub(i + 1), rootMt)
                    .fap(parentType -> Mt.getElSt(parentType));
            }
        }
        return rootMt.getKey(varName).types.itr();
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

                Mt mt = opt(caretLeaf.getContainingFile())
                    .cst(TwigFile.class)
                    .fap(f -> {
                        It<DeepType> rootTit = getRootDocTypes(f, exprCtx);

                        int r = caretLeaf.getTextOffset();
                        int l = Math.max(0, r - 100);
                        String prefix = f.getText().substring(l, r);
                        L<String> keyPath = Tls.regex("^[\\s\\S]*?((?:\\w+\\.)+)\\w*$", prefix)
                            .fap(m -> m.gat(0))
                            .fap(str -> L(str.split("\\.")))
                            .arr();
                        if (keyPath.size() > 0) {
                            String varName = keyPath.remove(0);
                            L<T2<String, String>> parentLoops = getParentLoops(caretLeaf);
                            Mt rootMt = Mt.mem(rootTit);
                            It<DeepType> varTit = resolveVar(varName, parentLoops, rootMt);
                            return varTit.fap(t -> Mt.getKeyPath(t, keyPath));
                        } else {
                            return rootTit;
                        }
                    })
                    .wap(Mt::mem);

                return mt.types
                    .fap(t -> t.keys)
                    .fap(k -> k.keyType.getTypes())
                    .fap(kt -> opt(kt.stringValue))
                    .unq()
                    .map(name -> LookupElementBuilder.create(name)
                        .withIcon(AssocKeyPvdr.getIcon())
                        .withTailText(" = " + mt.getKey(name).getBriefValueText(50)));
            });
        options.forEach(result::addElement);
    }
}
