package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/** a node in called function stack trace with args */
public class FuncCtx extends Lang
{
    enum EArgPsiType {DIRECT, ARR, NONE, INDIRECT};

    final private Opt<FuncCtx> parent;
    final public Opt<PsiElement> uniqueRef;
    final private SearchContext search;
    final private L<S<Mt>> argGetters;
    private L<Integer> variadicOrders = L();
    public Opt<Lang.S<Mt>> instGetter = opt(null);
    public Opt<PhpType> clsIdeaType = opt(null);
    final private EArgPsiType argPsiType;
    /** use this when you need to reference a real PSI during parsing of PHP Doc */
    public Opt<PsiElement> fakeFileSource = opt(null);
    private L<StackTraceElement> debugCtorTrace = L();

    private HashMap<Integer, Mt> cachedArgs = new HashMap<>();

    public FuncCtx(SearchContext search)
    {
        this.argGetters = L();
        this.search = search;
        this.uniqueRef = opt(null);
        this.parent = opt(null);
        this.argPsiType = EArgPsiType.NONE;
        if (search.debug) {
            debugCtorTrace = L(new Exception().getStackTrace());
        }
    }

    public FuncCtx(FuncCtx parentCtx, L<S<Mt>> argGetters, PsiElement uniqueRef, EArgPsiType argPsiType)
    {
        this.argGetters = argGetters;
        this.search = parentCtx.search;
        this.uniqueRef = opt(uniqueRef);
        this.parent = opt(parentCtx);
        this.argPsiType = argPsiType;
        this.fakeFileSource = opt(parentCtx)
            .fop(par -> par.fakeFileSource);
        if (search.debug) {
            debugCtorTrace = L(new Exception().getStackTrace());
        }
    }

    private Mt getCached(int index, S<Mt> argGetter)
    {
        if (!cachedArgs.containsKey(index)) {
            cachedArgs.put(index, Mt.CIRCULAR_REFERENCE);
            Mt mt = argGetter.get();
            cachedArgs.replace(index, mt);
        }
        return cachedArgs.get(index);
    }

    private Opt<Mt> getPassedVariadicPart(int order)
    {
        return variadicOrders.fst()
            .flt(firstVari -> firstVari <= order)
            .map(firstVari -> argGetters.fap((get, i) -> {
                if (i >= firstVari) {
                    Mt mt = getCached(order, get);
                    if (variadicOrders.contains(i)) {
                        mt = mt.getEl();
                    }
                    return mt.types;
                } else {
                    return list();
                }
            }))
            .map(types -> new Mt(types));
    }

    public Opt<Mt> getArg(ArgOrder orderObj)
    {
        Opt<Mt> fromVariadic = getPassedVariadicPart(orderObj.order);
        if (fromVariadic.has()) {
            if (orderObj.isVariadic) {
                return uniqueRef.map(ref -> fromVariadic.unw().getInArray(ref)).map(t -> new Mt(list(t)));
            } else {
                return opt(fromVariadic.fap(mt -> mt.types).wap(Mt::new));
            }
        } else if (!orderObj.isVariadic) {
            int index = orderObj.order;
            return argGetters.gat(index).map(argGetter -> getCached(index, argGetter));
        } else {
            return uniqueRef.map(ref -> {
                DeepType allArgs = new DeepType(ref, PhpType.ARRAY);
                argGetters.sub(orderObj.order)
                    .map((argGetter, i) -> getCached(i, argGetter))
                    .fch((mt, i) -> allArgs.addKey(i + "", ref)
                        .addType(() -> mt, mt.getIdeaType()));
                return new Mt(list(allArgs));
            });
        }
    }

    public Opt<Mt> getArg(Integer index)
    {
        return getArg(new ArgOrder(index, false));
    }

    public Mt getArgMt(Integer index)
    {
        return getArg(new ArgOrder(index, false)).def(Mt.INVALID_PSI);
    }

    @NotNull
    public It<DeepType> findExprType(PhpExpression expr)
    {
        return It(search.findExprType(expr, this));
    }

    public It<DeepType> limitResolve(int limit, PhpExpression expr)
    {
        int oldDepth = search.depthLeft;
        search.setDepth(Math.min(oldDepth, limit));
        It<DeepType> result = findExprType(expr);
        search.setDepth(oldDepth);
        return result;
    }

    public FuncCtx subCtxDirect(FunctionReference funcCall)
    {
        FuncCtx self = subCtxDirectGeneric(funcCall);
        Tls.cast(MethodReference.class, funcCall)
            .map(methCall -> methCall.getClassReference())
            .thn(ref -> {
                if (ref instanceof ClassReference) {
                    self.clsIdeaType = opt(ref.getType());
                } else {
                    self.instGetter = opt(() -> new Mt(findExprType(ref)));
                }
            });
        return self;
    }

    public FuncCtx subCtxDirect(NewExpression funcCall)
    {
        FuncCtx self = subCtxDirectGeneric(funcCall);
        opt(funcCall.getClassReference())
            .thn(ref -> self.clsIdeaType = opt(ref.getType()));
        return self;
    }

    public FuncCtx subCtxDirectGeneric(ParameterListOwner funcCall)
    {
        L<PsiElement> psiArgs = L(funcCall.getParameters());
        L<S<Mt>> argGetters = psiArgs.map((psi) -> S(() ->
            Tls.cast(PhpExpression.class, psi) .uni(
                arg -> new Mt(findExprType(arg)),
                () -> Mt.INVALID_PSI)
        )).arr();
        FuncCtx subCtx = new FuncCtx(this, argGetters, funcCall, EArgPsiType.DIRECT);
        psiArgs.fch((arg, i) -> {
            if (opt(arg.getPrevSibling()).map(sib -> sib.getText()).def("").equals("...")) {
                subCtx.variadicOrders.add(i);
            }
        });
        return subCtx;
    }

    /** context from args passed in array for example in array_map or array_filter */
    public FuncCtx subCtxSingleArgArr(PhpExpression argArr)
    {
        L<S<Mt>> argGetters = list(() -> findExprType(argArr).fap(Mt::getElSt).wap(Mt::new));
        return new FuncCtx(this, argGetters, argArr, EArgPsiType.ARR);
    }

    /** when you have expression PSI and it is not directly passed to the func, ex. call_user_func_array() */
    public FuncCtx subCtxIndirect(PhpExpression args)
    {
        S<Mt> getMt = Tls.onDemand(() -> new Mt(findExprType(args)));
        L<S<Mt>> argGetters = list();
        // always 10 arguments, got any problem?
        // it probably should be done correctly one day...
        for (int i = 0; i < 10; ++i) {
            String key = i + "";
            argGetters.add(() -> getMt.get().getKey(key));
        }
        return new FuncCtx(this, argGetters, args, EArgPsiType.INDIRECT);
    }

    public FuncCtx subCtxEmpty()
    {
        return new FuncCtx(this, list(), null, EArgPsiType.NONE);
    }

    public boolean hasArgs()
    {
        // this probably must also include clsIdeaType.has()...
        return argGetters.size() > 0 || instGetter.has();
    }

    public SearchContext getSearch()
    {
        return search;
    }

    private L<Object> getHashValues()
    {
        L<Object> values = list();
        values.add(argPsiType);
        values.add(clsIdeaType.map(ArrCtorRes::ideaTypeToFqn));
        values.add(hasArgs());
        if (!hasArgs()) return values;
        values.add(uniqueRef);
        if (!uniqueRef.has()) return values;
        values.add(parent);
        return values;
    }

    public int hashCode()
    {
        return getHashValues().hashCode();
    }

    public boolean equals(Object thatRaw)
    {
        return Tls.cast(FuncCtx.class, thatRaw)
            .map(that -> this.getHashValues().equals(that.getHashValues()))
            .def(false);
    }

    /** for debug */
    public It<Mt> getArgs()
    {
        return Tls.range(0, argGetters.size()).map(i -> getCached(i, argGetters.get(i)));
    }

    public String toString()
    {
        L<String> parents = L();
        FuncCtx tmp = this;
        while (tmp != null && tmp.uniqueRef.has()) {
            String trace = debugCtorTrace
                .flt(el -> !el.getClassName().endsWith("FuncCtx"))
                .map(el -> " " + Tls.substr(el.getClassName(), -20) + " " + el.getLineNumber())
                .fst().def("");
            parents.add(tmp.uniqueRef.unw().getText() + trace);
            tmp = tmp.parent.def(null);
        }
        return Tls.implode(" | ", parents.map(p -> Tls.singleLine(p, 600)));
    }

    public L<FuncCtx> getCallStack()
    {
        L<FuncCtx> result = list();
        FuncCtx tmp = this;
        while (tmp != null) {
            result.add(tmp);
            tmp = tmp.parent.def(null);
        }
        return result;
    }

    public int getCallStackLength()
    {
        return getCallStack().size();
    }

    /**
     * when you parse text, attempts to go to a PSI
     * in it will lead you to a fake foo.bar file
     * I would rather go to the doc
     */
    public PsiElement getRealPsi(PsiElement maybeFake)
    {
        PsiFile file = maybeFake.getContainingFile();
        PsiDirectory dir = file.getContainingDirectory();
        if (dir == null && fakeFileSource.has()) {
            return fakeFileSource.unw();
        } else {
            return maybeFake;
        }
    }
}
