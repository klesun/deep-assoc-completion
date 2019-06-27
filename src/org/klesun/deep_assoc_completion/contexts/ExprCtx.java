package org.klesun.deep_assoc_completion.contexts;

import com.intellij.openapi.project.Project;
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
 * like FuncCtx, but points to the parent _expression_ not the parent _function_
 * I'm going to use this to keep record of _expression tree_ for depth limits and debug
 */
public class ExprCtx implements IExprCtx {
    final public int depth;
    final private FuncCtx funcCtx;
    final public PsiElement expr;
    final public Opt<ExprCtx> parent;
    final public L<ExprCtx> children = list();
    public boolean doNotCache = false;
    public Opt<Integer> typeCnt = non();

    private ExprCtx(FuncCtx funcCtx, PsiElement expr, int depth, Opt<ExprCtx> parent) {
        this.funcCtx = funcCtx;
        this.expr = expr;
        this.depth = depth;
        this.parent = parent;
    }

    public ExprCtx(FuncCtx funcCtx, PsiElement expr, int depth) {
        this(initTopCtx(funcCtx, expr), expr, depth, non());
    }

    public IFuncCtx func() {
        return funcCtx;
    }

    private static FuncCtx initTopCtx(FuncCtx funcCtx, PsiElement expr) {
        Opt<Method> methOpt = Tls.findParent(expr, Method.class);
        Opt<PhpClass> clsOpt = methOpt.fop(m -> Tls.findParent(m, PhpClass.class));
        if (methOpt.has() && clsOpt.has()) {
            Method meth = methOpt.unw();
            PhpClass cls = clsOpt.unw();
            if (meth.isStatic()) {
                funcCtx = funcCtx.subCtxSelfCls(cls);
            } else {
                funcCtx = funcCtx.subCtxThisCls(cls);
            }
        }
        return funcCtx;
    }

    private ExprCtx subExpr(PsiElement expr, FuncCtx funcCtx) {
        ExprCtx nextCtx = new ExprCtx(funcCtx, expr, depth + 1, som(this));
        nextCtx.doNotCache = this.doNotCache;
        children.add(nextCtx);
        return nextCtx;
    }

    public ExprCtx subCtxEmpty() {
        return subExpr(expr, funcCtx.subCtxEmpty());
    }

    public ExprCtx subCtxDoc(PsiElement fakeFileSource) {
        FuncCtx funcSubCtx = funcCtx.subCtxEmpty();
        funcSubCtx.fakeFileSource = som(fakeFileSource);
        funcSubCtx.clsIdeaType = getSelfType();
        funcSubCtx.instGetter = funcCtx.instGetter;
        return subExpr(expr, funcSubCtx);
    }

    public ExprCtx subCtxDirect(FunctionReference funcCall) {
        return subExpr(funcCall, funcCtx.subCtxDirect(funcCall, this::findExprType));
    }

    public ExprCtx subCtxDirect(NewExpression funcCall) {
        return subExpr(funcCall, funcCtx.subCtxDirect(funcCall, this::findExprType));
    }

    public ExprCtx subCtxSingleArgArr(PhpExpression argArr, int argOrder) {
        return subExpr(argArr, funcCtx.subCtxSingleArgArr(argArr, argOrder, this::findExprType));
    }

    public ExprCtx subCtxIndirect(PhpExpression args) {
        return subExpr(args, funcCtx.subCtxIndirect(args, this::findExprType));
    }

    public ExprCtx subCtxMagicProp(FieldReference fieldRef) {
        return subExpr(fieldRef, funcCtx.subCtxMagicProp(fieldRef, this::findExprType));
    }

    public IExprCtx subCtxMem(MemberReference fieldRef) {
        return subExpr(fieldRef, funcCtx.subCtxMem(fieldRef, this::findExprType));
    }

    @Override
    public IExprCtx subCtxSelfCls(PhpClass clsPsi) {
        return subExpr(clsPsi, funcCtx.subCtxSelfCls(clsPsi));
    }

    @Override
    public IExprCtx subCtxThisCls(PhpClass clsPsi) {
        return subExpr(clsPsi, funcCtx.subCtxThisCls(clsPsi));
    }

    public ExprCtx withClosure(L<T2<String, S<MemIt<DeepType>>>> closureVars, IExprCtx outsideCtx) {
        return subExpr(expr, funcCtx.withClosure(closureVars, outsideCtx.func()));
    }

    public Opt<PsiElement> getFakeFileSource() {
        return funcCtx.fakeFileSource;
    }

    public Opt<Project> getProject() {
        return funcCtx.getSearch().project;
    }
    public SearchCtx getSearch() {
        return funcCtx.getSearch();
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

    public L<T2<String, S<MemIt<DeepType>>>> getClosureVars() {
        return funcCtx.closureVars;
    }

    public It<DeepType> findExprType(PhpExpression expr) {
        return It(funcCtx.getSearch().findExprType(expr, subExpr(expr, funcCtx)));
    }

    public It<DeepType> limitResolveDepth(int depthLimit, PhpExpression expr) {
        int depth = Math.max(funcCtx.getSearch().maxDepth - depthLimit, this.depth);
        ExprCtx nextCtx = new ExprCtx(funcCtx, expr, depth, som(this));
        nextCtx.doNotCache  = true;
        children.add(nextCtx);
        return It(nextCtx.findExprType(expr));
    }

    public int getDepth()
    {
        return depth;
    }
}
