package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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
import org.klesun.deep_assoc_completion.structures.Mkt;
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

    private static Opt<PsiElement> getPrev(PsiElement psi) {
        do {
            PsiElement prev = psi.getPrevSibling();
            if (prev != null) {
                return som(prev);
            }
            psi = psi.getParent();
        } while (psi != null);

        return non();
    }

    private static Opt<T2<String, String>> assertSymfonyVarDoc(PsiElement psi) {
        return Tls.cast(PsiCommentImpl.class, psi)
            .fop(cmt -> Tls.regex("^\\{#\\s*@var\\s+(\\w+)(?:\\s+\\S+)?\\s*(=\\s*\\S.*?)\\s*#\\}$", cmt.getText()))
            .map(match -> T2(match.get(0), match.get(1)));
    }

    /**
     * examples:
     * {# @var someVar \SomeNs\SomeVarClass #}
     * {# @var someVarRec = \SomeNs\SomeVarClass::makeRecord() #}
     */
    private static It<T2<String, String>> getSymfonyVarDocExprs(PsiElement caretLeaf) {
        Opt<PsiElement> prevOpt = getPrev(caretLeaf);
        List<T2<String, String>> varTuples = new ArrayList<>();
        while (prevOpt.has()) {
            PsiElement prev = prevOpt.unw();
            assertSymfonyVarDoc(prev).thn(varTuples::add);
            prevOpt = getPrev(prev);
        }
        return It(varTuples);
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

    private static L<String> getKeyPath(PsiElement caretLeaf, PsiFile f) {
        int r = caretLeaf.getTextOffset();
        int l = Math.max(0, r - 100);
        String prefix = f.getText().substring(l, r);
        return Tls.regex("^[\\s\\S]*?((?:\\w+\\.)+)\\w*$", prefix)
            .fap(m -> m.gat(0))
            .fap(str -> L(str.split("\\.")))
            .arr();
    }

    private Opt<ExprCtx> makeExprCtx(CompletionParameters parameters) {
        return opt(parameters.getOriginalPosition()).map(caretLeaf -> {
            int depth = AssocKeyPvdr.getMaxDepth(parameters);
            SearchCtx search = new SearchCtx(parameters).setDepth(depth);
            FuncCtx funcCtx = new FuncCtx(search);
            search.isMain = true;
            return new ExprCtx(funcCtx, caretLeaf, 0);
        });
    }

    private It<DeepType> resolve(ExprCtx exprCtx, TwigFile f) {
        PsiElement caretLeaf = exprCtx.expr;

        It<DeepType> varsDefsTit = getSymfonyVarDocExprs(caretLeaf).map(tuple -> {
            String varName = tuple.a;
            String eqExpr = tuple.b;
            Mt eqMt = new DocParamRes(exprCtx)
                .parseEqExpression(eqExpr, f)
                .wap(Mt::mem);
            return Mkt.assoc(caretLeaf, som(T2(varName, eqMt)));
        });
        It<DeepType> rootTit = getRootDocTypes(f, exprCtx);

        L<String> keyPath = getKeyPath(caretLeaf, f);
        if (keyPath.size() > 0) {
            String varName = keyPath.remove(0);
            L<T2<String, String>> parentLoops = getParentLoops(caretLeaf);
            Mt rootMt = Mt.mem(rootTit);
            It<DeepType> varTit = It.cnc(
                resolveVar(varName, parentLoops, rootMt),
                // more correct approach would be to only use var definition if it's name was not overridden in a loop, but
                // for simplicity sake I'll keep all definitions names global, there is no support for {% set ... %} anyway
                varsDefsTit.fap(t -> Mt.getKeySt(t, varName))
            );
            return varTit.fap(t -> Mt.getKeyPath(t, keyPath));
        } else {
            return It.cnc(varsDefsTit, rootTit);
        }
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext processingContext,
        @NotNull CompletionResultSet result
    ) {
        if (DumbService.isDumb(parameters.getPosition().getProject())) {
            // following code relies on complex reference resolutions
            // very much, so trying to resolve type during indexing
            // is pointless and is likely to cause exceptions
            return;
        }
        It<LookupElement> options = makeExprCtx(parameters)
            .fap(exprCtx -> {
                Mt mt = opt(exprCtx.expr.getContainingFile())
                    .cst(TwigFile.class)
                    .fap(f -> resolve(exprCtx, f))
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
