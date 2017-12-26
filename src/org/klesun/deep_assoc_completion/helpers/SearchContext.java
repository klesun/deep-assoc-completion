package org.klesun.deep_assoc_completion.helpers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_assoc_completion.DeepTypeResolver;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

public class SearchContext extends Lang
{
    // parametrized fields
    private int depth = 20;
    private int initialDepth = depth;
    public boolean argInferenceEnabled = true;
    private boolean debug = false;
    // max expressions per single search - guard
    // against memory overflow in circular references
    private int maxExpressions = 10000;
    // for performance measurement
    private int expressionsResolved = 0;
    final public L<PhpExpression> psiTrace = L();

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

    public Opt<MultiType> findExprType(PhpExpression expr, FuncCtx funcCtx)
    {
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
            return opt(MultiType.CIRCULAR_REFERENCE);
        }

        if (debug) {
            for (int i = 0; i < initialDepth - depth; ++i) {
                System.out.print("  ");
            }
            System.out.println(depth + " " + expr.getText().split("\n")[0]);
        }

        Opt<MultiType> result = DeepTypeResolver.resolveIn(expr, funcCtx)
            .map(ts -> new MultiType(ts));

        psiTrace.remove(psiTrace.size() - 1);
        ++depth;
        return result;
    }

    public int getExpressionsResolved()
    {
        return this.expressionsResolved;
    }
}
