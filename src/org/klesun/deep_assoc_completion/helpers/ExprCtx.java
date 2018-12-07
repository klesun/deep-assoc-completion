package org.klesun.deep_assoc_completion.helpers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.FieldReference;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.FieldReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.*;

import java.util.Collection;
import java.util.Map;

import static org.klesun.lang.Lang.*;

/**
 * like FuncCtx, but points to the parent _expression_ not the parent _function_
 * I'm going to use this to keep record of _expression tree_ for depth limits and debug
 */
public class ExprCtx implements IExprCtx {
    final public int depth;
    final private FuncCtx funcCtx;
    final public PsiElement expr;
    final public L<ExprCtx> children = list();
    public boolean doNotCache = false;
    public Opt<Integer> typeCnt = non();

    public ExprCtx(FuncCtx funcCtx, PsiElement expr, int depth) {
        this.funcCtx = funcCtx;
        this.expr = expr;
        this.depth = depth;
    }

    public IFuncCtx func() {
        return funcCtx;
    }

    private ExprCtx subExpr(PsiElement expr, FuncCtx funcCtx) {
        ExprCtx nextCtx = new ExprCtx(funcCtx, expr, depth + 1);
        nextCtx.doNotCache = this.doNotCache;
        children.add(nextCtx);
        return nextCtx;
    }

    public ExprCtx subCtxEmpty() {
        return subExpr(expr, funcCtx.subCtxEmpty());
    }

    public ExprCtx subCtxEmpty(PsiElement fakeFileSource) {
        FuncCtx funcSubCtx = funcCtx.subCtxEmpty();
        funcSubCtx.fakeFileSource = som(fakeFileSource);
        return subExpr(expr, funcSubCtx);
    }

    public ExprCtx subCtxDirect(FunctionReference funcCall) {
        return subExpr(expr, funcCtx.subCtxDirect(funcCall, this::findExprType));
    }

    public ExprCtx subCtxDirect(NewExpression funcCall) {
        return subExpr(expr, funcCtx.subCtxDirect(funcCall, this::findExprType));
    }

    public ExprCtx subCtxSingleArgArr(PhpExpression argArr) {
        return subExpr(expr, funcCtx.subCtxSingleArgArr(argArr, this::findExprType));
    }

    public ExprCtx subCtxIndirect(PhpExpression args) {
        return subExpr(expr, funcCtx.subCtxIndirect(args, this::findExprType));
    }

    public ExprCtx subCtxMagicProp(FieldReference fieldRef) {
        return subExpr(expr, funcCtx.subCtxMagicProp(fieldRef, this::findExprType));
    }

    public ExprCtx withClosure(L<T2<String, S<MemoizingIterable<DeepType>>>> closureVars) {
        return subExpr(expr, funcCtx.withClosure(closureVars));
    }

    public Opt<PsiElement> getFakeFileSource() {
        return funcCtx.fakeFileSource;
    }

    public Opt<Project> getProject() {
        return funcCtx.getSearch().project;
    }

    public Map<PsiFile, Collection<FieldReferenceImpl>> getFieldRefCache() {
        return funcCtx.getSearch().fileToFieldRefs;
    }

    public It<DeepType> getThisType() {
        return funcCtx.instGetter.fap(g -> g.get().types);
    }

    public Opt<PhpType> getSelfType() {
        return funcCtx.clsIdeaType;
    }

    public L<T2<String, S<MemoizingIterable<DeepType>>>> getClosureVars() {
        return funcCtx.closureVars;
    }

    public It<DeepType> findExprType(PhpExpression expr) {
        return It(funcCtx.getSearch().findExprType(expr, subExpr(expr, funcCtx)));
    }

    public It<DeepType> limitResolveDepth(int depthLimit, PhpExpression expr) {
        int depth = Math.max(funcCtx.getSearch().maxDepth - depthLimit, this.depth);
        ExprCtx nextCtx = new ExprCtx(funcCtx, expr, depth);
        nextCtx.doNotCache  = true;
        children.add(nextCtx);
        return It(nextCtx.findExprType(expr));
    }
}
