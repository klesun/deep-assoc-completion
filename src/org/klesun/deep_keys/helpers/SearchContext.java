package org.klesun.deep_keys.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.deep_keys.resolvers.*;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

public class SearchContext extends Lang
{
    // parametrized fields
    private int depth = 20;

    public SearchContext()
    {
    }

    public SearchContext setDepth(int depth)
    {
        this.depth = depth;
        return this;
    }

    public Opt<MultiType> findExprType(PhpExpression expr, FuncCtx funcCtx)
    {
        // i didn't figure out how to catch circular
        // references yet, gonna leave it for another time
//        if (exprTrace.contains(expr)) {
//            // circular reference
//            return opt(null);
//        }

        if (depth <= 0) return opt(null);
        --depth;

        Opt<MultiType> result = DeepTypeResolver.resolveIn(expr, funcCtx)
            .map(ts -> new MultiType(ts));

        ++depth;
        return result;
    }
}
