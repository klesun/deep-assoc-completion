package org.klesun.deep_assoc_completion.contexts;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FieldReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.*;

import java.util.Collection;
import java.util.Map;

import static org.klesun.lang.Lang.*;

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
    IExprCtx subCtxSingleArgArr(PhpExpression argArr);
    IExprCtx subCtxIndirect(PhpExpression args);
    IExprCtx subCtxMagicProp(FieldReference fieldRef);
    IExprCtx withClosure(L<T2<String, S<MemIt<DeepType>>>> closureVars);
    It<DeepType> getThisType();
    Opt<PhpType> getSelfType();
    L<T2<String, S<MemIt<DeepType>>>> getClosureVars();
    Opt<PsiElement> getFakeFileSource();
    Map<PsiFile, Collection<FieldReferenceImpl>> getFieldRefCache();
    Opt<Project> getProject();
    int getDepth();

    It<DeepType> findExprType(PhpExpression expr);
    It<DeepType> limitResolveDepth(int depthLimit, PhpExpression expr);


    /**
     * when you parse text, attempts to go to a PSI
     * in it will lead you to a fake foo.bar file
     * I would rather go to the doc
     */
    default PsiElement getRealPsi(PsiElement maybeFake)
    {
        PsiFile file = maybeFake.getContainingFile();
        PsiDirectory dir = file.getContainingDirectory();
        if (dir == null && getFakeFileSource().has()) {
            return getFakeFileSource().unw();
        } else {
            return maybeFake;
        }
    }

    default Boolean isInComment()
    {
        return getFakeFileSource().has();
    }
}
