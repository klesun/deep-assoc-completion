package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterListOwner;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.FieldReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.util.Collection;
import java.util.Map;

import static org.klesun.lang.Lang.som;

/**
 * making it an interface since I'm refactor the context
 * class passed to resolvers for the third time already
 *
 * maybe should rename to just ICtx or IResolutionCtx since
 * it does not have any expression-specific methods...
 */
public interface IExprCtx {
    IFuncCtx func();
    IExprCtx subCtxEmpty();
    IExprCtx subCtxEmpty(PsiElement fakeFileSource);
    IExprCtx subCtxDirect(FunctionReference funcCall);
    IExprCtx subCtxDirect(NewExpression funcCall);
    IExprCtx subCtxDirectGeneric(ParameterListOwner funcCall);
    IExprCtx subCtxSingleArgArr(PhpExpression argArr);
    IExprCtx subCtxIndirect(PhpExpression args);
    It<DeepType> getThisType();
    Opt<PhpType> getSelfType();
    Opt<PsiElement> getFakeFileSource();
    Map<PsiFile, Collection<FieldReferenceImpl>> getFieldRefCache();

    It<DeepType> findExprType(PhpExpression expr);
    It<DeepType> limitResolve(int limit, PhpExpression expr);
}
