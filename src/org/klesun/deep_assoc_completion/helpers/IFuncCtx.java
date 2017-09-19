package org.klesun.deep_assoc_completion.helpers;

import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.jetbrains.annotations.NotNull;
import org.klesun.lang.Lang.L;
import org.klesun.lang.Lang.S;
import org.klesun.lang.Opt;

/**
 * provides arguments passed to the current function
 * and a method to solve inner PSI expressions
 */
public interface IFuncCtx
{
    public Opt<MultiType> getArg(Integer index);
    @NotNull
    public MultiType findExprType(PhpExpression expr);
    public IFuncCtx subCtx(L<S<MultiType>> args);
    /** @debug */
    public int getArgCnt();
    public SearchContext getSearch();
}
