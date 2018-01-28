package org.klesun.deep_assoc_completion.helpers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_assoc_completion.DeepTypeResolver;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SearchContext extends Lang
{
    // parametrized fields
    private int depth = 20;
    private int initialDepth = depth;
    private boolean debug = false;
    // max expressions per single search - guard
    // against memory overflow in circular references
    private int maxExpressions = 10000;
    // for performance measurement
    private int expressionsResolved = 0;
    final public L<PhpExpression> psiTrace = L();
    final private Map<FuncCtx, Map<PhpExpression, MultiType>> ctxToExprToResult = new HashMap<>();

    public SearchContext()
    {
    }

    public SearchContext setDepth(int depth)
    {
        this.depth = initialDepth = depth;
        return this;
    }

    public SearchContext setDebug(boolean debug)
    {
        this.debug = debug;
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

    private Opt<MultiType> takeFromCache(FuncCtx ctx, PhpExpression expr)
    {
        if (!ctxToExprToResult.containsKey(ctx)) {
            return opt(null);
        }
        if (!ctxToExprToResult.get(ctx).containsKey(expr)) {
            return opt(null);
        }
        return opt(ctxToExprToResult.get(ctx).get(expr));
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
            System.out.println(indent + "## Expression limit guard reached " + expressionsResolved + " " + expr.getText());
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

        Opt<MultiType> result = takeFromCache(funcCtx, expr);
        if (result.has()) {
            if (debug) {
                System.out.println(indent + "<< TAKING RESULT FROM CACHE");
            }
        } else {
            result = DeepTypeResolver.resolveIn(expr, funcCtx)
                .map(ts -> new MultiType(ts));
            result.thn(mt -> putToCache(funcCtx, expr, mt));
        }

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
