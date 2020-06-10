package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.*;

import static org.klesun.lang.Lang.*;

/**
 * DeepType builder
 */
public class Build {
    final private DeepType deepType;

    public Build(@NotNull PsiElement definition, PhpType briefType) {
        this.deepType = new DeepType(definition, briefType);
    }

    public Build stringValue(String stringValue) {
        this.deepType.stringValue = stringValue;
        return this;
    }

    public Build isExactPsi(Boolean isExactPsi) {
        this.deepType.isExactPsi = isExactPsi;
        return this;
    }

    public Build isNumber(Boolean isNUmber) {
        this.deepType.isNumber = isNUmber;
        return this;
    }

    public Build ctorArgs(IExprCtx ctorArgs) {
        this.deepType.ctorArgs = opt(ctorArgs);
        return this;
    }

    public Build generics(L<Mt> generics) {
        this.deepType.generics = generics;
        return this;
    }

    public Build clsRefType(PhpType clsType) {
        this.deepType.clsRefType = som(clsType);
        return this;
    }

    public Build keys(IReusableIt<KeyEntry> keys) {
        this.deepType.keys = keys;
        return this;
    }

    public Build keys(Iterable<KeyEntry> keys) {
        this.keys(new MemIt<>(keys));
        return this;
    }

    public Build returnTypeGetters(Iterable<Lang.F<IExprCtx, MemIt<DeepType>>> returnTypeGetters) {
        this.deepType.returnTypeGetters = L(returnTypeGetters);
        return this;
    }

    public DeepType get() {
        return deepType;
    }

    public It<DeepType> itr() {
        return som(deepType).itr();
    }
}
