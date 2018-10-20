package org.klesun.deep_assoc_completion.helpers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.FieldReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.FieldReferenceImpl;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.DeepTypeResolver;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.lang.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SearchContext extends Lang
{
    // parametrized fields
    private long startTime = System.nanoTime();
    private long lastReportTime = System.nanoTime();
    public int depthLeft = 20;
    public int initialDepth = depthLeft;
    final public static boolean DEBUG_DEFAULT = false;
    public boolean debug = DEBUG_DEFAULT;
    private Opt<Double> timeout = opt(null);
    final public Opt<Project> project;
    // for performance measurement
    private int expressionsResolved = 0;
    final public L<PhpExpression> psiTrace = L();
    final private Map<FuncCtx, Map<PhpExpression, Iterable<DeepType>>> ctxToExprToResult = new HashMap<>();
    public Opt<Integer> overrideMaxExpr = non();
    final public Map<PsiFile, Collection<FieldReferenceImpl>> fileToFieldRefs = new HashMap<>();

    public SearchContext(@Nullable Project project)
    {
        this.project = opt(project);
    }

    public SearchContext(CompletionParameters parameters)
    {
        this(parameters.getEditor().getProject());
    }

    public SearchContext setDepth(int depth)
    {
        this.depthLeft = initialDepth = depth;
        return this;
    }

    public SearchContext setTimeout(double timeout)
    {
        this.timeout = opt(timeout);
        return this;
    }

    public SearchContext setDebug(boolean debug)
    {
        this.debug = debug;
        return this;
    }

    public Integer getMaxExpressions()
    {
        // max expressions per single search - guard
        // against memory overflow in circular references
        return Opt.fst(
            () -> overrideMaxExpr,
            () -> project.map(project -> {
                DeepSettings settings = DeepSettings.inst(project);
                return settings.totalExpressionLimit;
            })
        ).def(10000);
    }

    private <T> boolean endsWith(L<T> superList, L<T> subList)
    {
        for (int i = 0; i < subList.size(); ++i) {
            if (i >= superList.size() || !superList.get(-i - 1).equals(subList.get(-i - 1))) {
                return false;
            }
        }
        return true;
    }

    // should probably keep expression tree and start using this again
    private boolean isRecursion()
    {
        // imagine sequence: a b c d e f g e f g
        //                           ^_____^_____
        // I'm not sure this assumption is right, but I'll try to
        // treat any case where end repeats pre-end as recursion
        for (int i = 0; i < psiTrace.size() / 2; ++i) {
            L<PhpExpression> subList = psiTrace.sub(psiTrace.size() - i * 2 - 2, i + 1);
            if (endsWith(psiTrace, subList)) {
                return true;
            }
        }
        return false;
    }

    private Opt<Iterable<DeepType>> takeFromCache(FuncCtx ctx, PhpExpression expr)
    {
        if (!ctxToExprToResult.containsKey(ctx)) {
            return opt(null);
        }
        if (!ctxToExprToResult.get(ctx).containsKey(expr)) {
            return opt(null);
        }
        Iterable<DeepType> mt = ctxToExprToResult.get(ctx).get(expr);
        return opt(mt);
    }

    private static String formatPsi(PsiElement expr)
    {
        String fileText = expr.getContainingFile().getText();
        int phpLineNum = Tls.substr(fileText, 0, expr.getTextOffset()).split("\n").length;
        return Tls.singleLine(expr.getText(), 120) + " - " + expr.getContainingFile().getName() + ":" + phpLineNum;
    }

    private void putToCache(FuncCtx ctx, PhpExpression expr, Iterable<DeepType> result)
    {
        if (!ctxToExprToResult.containsKey(ctx)) {
            ctxToExprToResult.put(ctx, new HashMap<>());
        }
        if (ctxToExprToResult.get(ctx).containsKey(expr)) {
            ctxToExprToResult.get(ctx).remove(expr);
        }
        ctxToExprToResult.get(ctx).put(expr, result);
    }

    private void printCache()
    {
        Map<PhpExpression, L<FuncCtx>> psiToCtxs = new LinkedHashMap<>();
        ctxToExprToResult.forEach((ctx, psiToMt) -> {
            psiToMt.forEach((psi, mt) -> {
                if (!psiToCtxs.containsKey(psi)) {
                    psiToCtxs.put(psi, list());
                }
                psiToCtxs.get(psi).add(ctx);
            });
        });
        L(psiToCtxs.keySet())
            .srt(psi -> -psiToCtxs.get(psi).size())
            .fch(psi -> {
                System.out.println("  ** PSI: " + formatPsi(psi));
                psiToCtxs.get(psi).fch(ctx -> {
                    System.out.println("   +-- Ctx: " + ctx.hashCode() + " " + ctx);
                });
            });
    }

    public Iterable<DeepType> findExprType(PhpExpression expr, FuncCtx funcCtx)
    {
        long time = System.nanoTime();
        double seconds = (time - startTime) / 1000000000.0;
        if (!debug && (time - lastReportTime) / 1000000000.0 > 1.0) {
            lastReportTime = System.nanoTime();
            //System.out.println("deep-assoc-completion warning at " + time + ": type resolution takes " + seconds + " seconds " + expr.getText() + " " + expr.getClass());
        }

        String indent = "";
        for (int i = 0; i < initialDepth - depthLeft; ++i) {
            indent += " ";
        }
        if (debug) {
            // pretty useless now, actually, after we moved to iterators
            // I guess a new "context" should be created on each expression, not just function call to know the depth
            String trace = funcCtx.getCallStack()
                .map(ctx -> Tls.singleLine(ctx.uniqueRef.map(ref -> ref.getText()).def("(no args)"), 30))
                .wap(calls -> Tls.implode(" -> ", calls));
            System.out.print(trace + " | ");
            String fileText = expr.getContainingFile().getText();
            int phpLineNum = Tls.substr(fileText, 0, expr.getTextOffset()).split("\n").length;
            StackTraceElement caller = new Exception().getStackTrace()[2];
            System.out.println(depthLeft + " " + Tls.singleLine(expr.getText(), 120) + "       - " + expr.getContainingFile().getName() + ":" + phpLineNum + "       - " + caller.getClassName() + ":" + caller.getLineNumber() + " ### " + funcCtx);
        }

        // TODO: add to config
        if (funcCtx.getCallStackLength() > 9) { // on <= 6 tests fail
            return It.non();
        }
        if (++expressionsResolved > getMaxExpressions()) {
            /** @debug */
            //System.out.println(indent + "## Expression limit guard reached " + expressionsResolved + " " + expr.getText());
            return It.non();
        } else if (timeout.flt(tout -> seconds > tout).has()) {
            String fileText = expr.getContainingFile().getText();
            int phpLineNum = Tls.substr(fileText, 0, expr.getTextOffset()).split("\n").length;
            System.out.println(indent + "## Timed out " + seconds + " " + expr.getClass() + " " + Tls.singleLine(expr.getText(), 50) + " " + expr.getContainingFile().getName() + ":" + phpLineNum);
            return It.non();
        }

        Opt<Iterable<DeepType>> result = takeFromCache(funcCtx, expr);
        if (result.has()) {
            if (debug) {
                //System.out.println(indent + "<< TAKING RESULT FROM CACHE");
            }
        } else {
            if (!overrideMaxExpr.has()) {
                putToCache(funcCtx, expr, list());
            }
            // .unq() before caching is important since types taken
            // from cache would grow in count exponentially otherwise
            It<DeepType> tit = DeepTypeResolver.resolveIn(expr, funcCtx).unq();
            Iterable<DeepType> mit = new MemoizingIterable<>(tit.iterator());
            result = som(mit);
            if (!overrideMaxExpr.has()) {
                result.thn(mt -> putToCache(funcCtx, expr, mit));
            }
        }

        return It(result.def(It.non()));
    }

    public int getExpressionsResolved()
    {
        return this.expressionsResolved;
    }
}
