package org.klesun.deep_assoc_completion.helpers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_assoc_completion.DeepTypeResolver;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class SearchContext extends Lang
{
    // parametrized fields
    private int depth = 20;
    private int initialDepth = depth;
    public boolean argInferenceEnabled = true;
    private boolean debug = false;
    // max expressions per single search - guard
    // against memory overflow in circular references
    private int maxExpressions = 2000;
    // for performance measurement
    private int expressionsResolved = 0;
    final public L<PhpExpression> psiTrace = L();

    /** @debug */
    Map<PhpExpression, Map<FuncCtx, Opt<MultiType>>> cachedResults = new LinkedHashMap<>();

    public SearchContext()
    {
    }

    public SearchContext setDepth(int depth)
    {
        this.depth = initialDepth = depth;
        return this;
    }

    /**
     * temporarily decrease depth to quickly get some
     * optional info (like type text in completion dialog)
     */
    public int limitDepthLeft(int maxDepth)
    {
        int wasDepth = this.depth;
        this.depth = Math.min(this.depth, maxDepth);
        return wasDepth;
    }

    public SearchContext setArgInferenceEnabled(boolean value)
    {
        this.argInferenceEnabled = value;
        return this;
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

    private Opt<Opt<MultiType>> getFromCacheIfAny(PhpExpression expr, FuncCtx funcCtx)
    {
        if (!cachedResults.containsKey(expr)) {
            return opt(null);
        }
        if (!cachedResults.get(expr).containsKey(funcCtx)) {
            return opt(null);
        }
        return opt(cachedResults.get(expr).get(funcCtx));
    }

    private void putIntoCache(PhpExpression expr, FuncCtx funcCtx, Opt<MultiType> resolved)
    {
        if (!cachedResults.containsKey(expr)) {
            cachedResults.put(expr, new LinkedHashMap<>());
        }
        if (cachedResults.get(expr).containsKey(funcCtx)) {
            cachedResults.get(expr).remove(funcCtx);
        }
        cachedResults.get(expr).put(funcCtx, resolved);
    }

    public Opt<MultiType> findExprType(PhpExpression expr, FuncCtx funcCtx)
    {
        String indent = "";
        for (int i = 0; i < initialDepth - depth; ++i) {
            indent += " ";
        }
        if (debug) {
            System.out.print(indent);
            String fileText = expr.getContainingFile().getText();
            int phpLineNum = Tls.substr(fileText, 0, expr.getTextOffset()).split("\n").length;
            StackTraceElement caller = new Exception().getStackTrace()[2];
            System.out.println(depth + " " + expr.getText().split("\n")[0] + "       - " + expr.getContainingFile().getName() + ":" + phpLineNum + "       - " + caller.getClassName() + ":" + caller.getLineNumber());
        }

        if (depth <= 0) {
            return opt(null);
        } else if (++expressionsResolved > maxExpressions) {
            /** @debug */
            System.out.println("Expression limit guard reached " + expressionsResolved);
            return opt(null);
        }
        --depth;
        psiTrace.add(expr);
        if (isRecursion()) {
            if (debug) {
                System.out.println(indent + "** CIRCULAR REFERENCE DETECTED");
            }
            psiTrace.remove(psiTrace.size() - 1);
            ++depth;
            return opt(MultiType.CIRCULAR_REFERENCE);
        }
        long startTime = System.nanoTime();

        Opt<MultiType> result = DeepTypeResolver.resolveIn(expr, funcCtx)
            .map(ts -> new MultiType(ts));

        psiTrace.remove(psiTrace.size() - 1);
        ++depth;

        if (debug) {
            long elapsed = System.nanoTime() - startTime;
            System.out.println(indent + "* " + result.fap(a -> a.types).size() + " types in " + (BigDecimal.valueOf(elapsed / 1000000000.0).toPlainString()));
        }

        return result;
    }

    public int getExpressionsResolved()
    {
        return this.expressionsResolved;
    }
}
