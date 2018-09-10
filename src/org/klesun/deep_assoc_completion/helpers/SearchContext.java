package org.klesun.deep_assoc_completion.helpers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_assoc_completion.DeepTypeResolver;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.lang.L;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SearchContext extends Lang
{
    // parametrized fields
    private long startTime = System.nanoTime();
    private long lastReportTime = System.nanoTime();
    public int depthLeft = 20;
    public int initialDepth = depthLeft;
    public boolean debug = false;
    private Opt<Double> timeout = opt(null);
    private Opt<Project> project = opt(null);
    // for performance measurement
    private int expressionsResolved = 0;
    final public L<PhpExpression> psiTrace = L();
    final private Map<FuncCtx, Map<PhpExpression, MultiType>> ctxToExprToResult = new HashMap<>();

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

    private Integer getMaxExpressions()
    {
        // max expressions per single search - guard
        // against memory overflow in circular references
        return project.map(project -> {
            DeepSettings settings = DeepSettings.inst(project);
            return settings.totalExpressionLimit;
        }).def(10000);
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

    private Opt<MultiType> takeFromCache(FuncCtx ctx, PhpExpression expr)
    {
        if (!ctxToExprToResult.containsKey(ctx)) {
            return opt(null);
        }
        if (!ctxToExprToResult.get(ctx).containsKey(expr)) {
            return opt(null);
        }
        MultiType mt = ctxToExprToResult.get(ctx).get(expr);
        return opt(mt);
    }

    private void putToCache(FuncCtx ctx, PhpExpression expr, MultiType result)
    {
        if (!ctxToExprToResult.containsKey(ctx)) {
            ctxToExprToResult.put(ctx, new HashMap<>());
        }
        if (ctxToExprToResult.get(ctx).containsKey(expr)) {
            ctxToExprToResult.get(ctx).remove(expr);
        }
        ctxToExprToResult.get(ctx).put(expr, result);
    }

    public Opt<MultiType> findExprType(PhpExpression expr, FuncCtx funcCtx)
    {
        /** @debug */
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
            System.out.print(indent);
            String fileText = expr.getContainingFile().getText();
            int phpLineNum = Tls.substr(fileText, 0, expr.getTextOffset()).split("\n").length;
            StackTraceElement caller = new Exception().getStackTrace()[2];
            System.out.println(depthLeft + " " + Tls.singleLine(expr.getText(), 120) + "       - " + expr.getContainingFile().getName() + ":" + phpLineNum + "       - " + caller.getClassName() + ":" + caller.getLineNumber() + " ### " + funcCtx);
        }

        if (depthLeft <= 0) {
            return opt(null);
        } else if (++expressionsResolved > getMaxExpressions()) {
            /** @debug */
            System.out.println(indent + "## Expression limit guard reached " + expressionsResolved + " " + expr.getText());
            return opt(null);
        } else if (timeout.flt(tout -> seconds > tout).has()) {
            String fileText = expr.getContainingFile().getText();
            int phpLineNum = Tls.substr(fileText, 0, expr.getTextOffset()).split("\n").length;
            System.out.println(indent + "## Timed out " + seconds + " " + expr.getClass() + " " + Tls.singleLine(expr.getText(), 50) + " " + expr.getContainingFile().getName() + ":" + phpLineNum);
            return opt(null);
        }
        --depthLeft;
        psiTrace.add(expr);
        if (isRecursion()) {
            if (debug) {
                System.out.println(indent + "** CIRCULAR REFERENCE DETECTED");
            }
            psiTrace.remove(psiTrace.size() - 1);
            ++depthLeft;
            return opt(MultiType.CIRCULAR_REFERENCE);
        }
        long startTime = System.nanoTime();

        Opt<MultiType> result = takeFromCache(funcCtx, expr);
        if (result.has()) {
            if (debug) {
                System.out.println(indent + "<< TAKING RESULT FROM CACHE");
            }
        } else {
            putToCache(funcCtx, expr, MultiType.CIRCULAR_REFERENCE);
            result = DeepTypeResolver.resolveIn(expr, funcCtx)
                .map(ts -> new MultiType(ts));
            result.thn(mt -> putToCache(funcCtx, expr, mt));
        }

        psiTrace.remove(psiTrace.size() - 1);
        ++depthLeft;

        if (debug) {
            long elapsed = System.nanoTime() - startTime;
            System.out.println(indent + "* " + result.fap(a -> a.types).size() +
                " types in " + (BigDecimal.valueOf(elapsed / 1000000000.0).toPlainString()) + " : " + Tls.implode(", ", result.fap(a -> a.getKeyNames())));
        }

        return result;
    }

    public int getExpressionsResolved()
    {
        return this.expressionsResolved;
    }
}
