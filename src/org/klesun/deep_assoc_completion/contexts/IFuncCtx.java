package org.klesun.deep_assoc_completion.contexts;

import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.ArgOrder;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

/** provides harmless read-only functions from FuncCtx */
public interface IFuncCtx {
    boolean hasArgs();
    boolean areArgsKnown();
    Opt<Mt> getArg(ArgOrder order);
    default Opt<Mt> getArg(Integer index) {
        return getArg(new ArgOrder(index, false));
    }
    default Mt getArgMt(Integer index) {
        return getArg(new ArgOrder(index, false)).def(Mt.INVALID_PSI);
    }
    int getCallStackLength();
    // DON'T ADD findExpType() HERE!!!

    public Opt<PhpType> getSelfType();
    public Opt<Lang.S<Mt>> getInstGetter();
}
