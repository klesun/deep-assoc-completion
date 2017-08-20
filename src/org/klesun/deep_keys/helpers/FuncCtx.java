package org.klesun.deep_keys.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.HashMap;

/** a node in called function stack trace with args */
public class FuncCtx extends Lang implements IFuncCtx
{
    final private SearchContext search;
    final private L<Lang.S<MultiType>> argGetters;

    private HashMap<Integer, MultiType> cachedArgs = new HashMap<>();

    public FuncCtx(SearchContext search, L<S<MultiType>> argGetters)
    {
        this.argGetters = argGetters;
        this.search = search;
    }

    public FuncCtx(FuncCtx parentCtx, L<S<MultiType>> argGetters)
    {
        this(parentCtx.search, argGetters);
    }

    public Opt<MultiType> getArg(Integer index)
    {
        return argGetters.gat(index).map(argGetter -> {
            if (!cachedArgs.containsKey(index)) {
                cachedArgs.put(index, argGetter.get());
            }
            return cachedArgs.get(index);
        });
    }

    public MultiType findExprType(PhpExpression expr)
    {
        return search.findExprType(expr, this).def(new MultiType(L()));
    }

    public IFuncCtx subCtx(Lang.L<Lang.S<MultiType>> args) {
        return new FuncCtx(this, args);
    }

    public int getArgCnt()
    {
        return argGetters.size();
    }
}
