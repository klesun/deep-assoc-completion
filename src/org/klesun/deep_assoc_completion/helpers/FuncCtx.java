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
    final private Opt<PsiElement> uniqueRef;
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

    public FuncCtx(FuncCtx parentCtx, L<S<Mt>> argGetters, @NotNull PsiElement uniqueRef, EArgPsiType argPsiType)
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
                return opt(fromVariadic.itr().fap(mt -> mt.types).wap(Mt::new));
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
        It<DeepType> result = It(search.findExprType(expr, this));
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

    public boolean hasArgs()
    {
        // this probably must also include clsIdeaType.has()...
        return argGetters.size() > 0 || instGetter.has();
    }

    public SearchContext getSearch()
    {
        return search;
    }

    public int hashCode()
    {
        if (!uniqueRef.has() || !hasArgs()) {
            // all contexts without args are same if class is same
            return ArrCtorRes.ideaTypeToFqn(this.clsIdeaType.def(null)).hashCode();
        } else {
            int noParentSalt = 7052005; // random int
            int parentHash = parent.uni(p -> p.hashCode(), () -> noParentSalt);
            return Arrays.asList(parentHash, uniqueRef.unw().hashCode()).hashCode();
        }
    }

    public boolean equals(Object thatRaw)
    {
        Boolean result = Tls.cast(FuncCtx.class, thatRaw)
            .map(that -> {
                // this could be written much shorter with one function getParents()
                // and list comparison, but taking all parents could be
                // not efficient, and we want this function to be AFAP
                if (this.argPsiType != that.argPsiType) return false;
                if (clsIdeaType.has()) {
                    Set<String> thisFqn = ArrCtorRes.ideaTypeToFqn(this.clsIdeaType.unw());
                    Set<String> thatFqn = ArrCtorRes.ideaTypeToFqn(that.clsIdeaType.def(null));
                    if (!thisFqn.equals(thatFqn)) return false;
                }
                if (!this.hasArgs()) return !that.hasArgs();
                if (!this.uniqueRef.has()) return !that.uniqueRef.has();
                if (!that.uniqueRef.has()) return false;
                if (!this.uniqueRef.unw().isEquivalentTo(that.uniqueRef.unw())) return false;
                if (!this.parent.has()) return !that.parent.has();
                if (!that.parent.has()) return false;

                return this.parent.unw().equals(that.parent.unw());
            })
            .def(false);
        return result;
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

    public int getCallStackLength()
    {
        int callStackLength = 0;
        FuncCtx tmp = this;
        while (tmp != null) {
            ++callStackLength;
            tmp = tmp.parent.def(null);
        }
        return callStackLength;
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
