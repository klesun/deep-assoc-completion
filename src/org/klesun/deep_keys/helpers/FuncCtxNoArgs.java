package org.klesun.deep_keys.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.opt;

/**
 * temporary helper to make transition from
 * context-less process to context-ful more smoothly
 */
public class FuncCtxNoArgs implements IFuncCtx
{
    int depth;

    public FuncCtxNoArgs(int depth)
    {
        this.depth = depth;
    }

    public Opt<MultiType> getArg(Integer index)
    {
        return opt(null);
    }

//    public MultiType findExprType(PsiElement expr)
//    {
//        return Tls.cast(PhpExpression.class, expr)
//            .map(casted -> findExprType(casted))
//            .def(new MultiType(L()));
//    }

    public MultiType findExprType(PhpExpression expr)
    {
        return new MultiType(DeepTypeResolver.findExprType(expr, depth));
    }

    public IFuncCtx subCtx(L<Lang.S<MultiType>> args) {
        return new FuncCtxNoArgs(depth);
    }
}
