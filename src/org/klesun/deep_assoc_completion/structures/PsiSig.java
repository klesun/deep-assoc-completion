package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.lang.L;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.list;

/**
 * defies a uniqueness of a PSI
 * needed because there are PSI created dynamically by us from phpdoc text
 */
public class PsiSig {
    final private PsiElement psi;
    final private IExprCtx ctx;

    public PsiSig(PsiElement psi, IExprCtx ctx) {
        this.psi = psi;
        this.ctx = ctx;
    }

    private L<Object> getHashValues() {
        PsiElement realPsi = ctx.getFakeFileSource().def(psi);
        return list(realPsi, ctx.func());
    }

    public int hashCode() {
        return getHashValues().hashCode();
    }

    public boolean equals(Object thatRaw) {
        return Tls.cast(PsiSig.class, thatRaw)
            .any(that ->
                this.getHashValues().equals(that.getHashValues()) &&
                this.psi.getTextOffset() == that.psi.getTextOffset() &&
                this.psi.getClass().equals(that.psi.getClass()) &&
                this.psi.getText().equals(that.psi.getText()));
    }
}
