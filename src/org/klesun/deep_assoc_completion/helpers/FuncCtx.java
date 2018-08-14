package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

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
    final private L<Lang.S<MultiType>> argGetters;
    private L<Integer> variadicOrders = L();
    public Opt<Lang.S<MultiType>> instGetter = opt(null);
    public Opt<PhpType> clsIdeaType = opt(null);
    final private EArgPsiType argPsiType;
    /** use this when you need to reference a real PSI during parsing of PHP Doc */
    public Opt<PsiElement> fakeFileSource = opt(null);

    private HashMap<Integer, MultiType> cachedArgs = new HashMap<>();

    public FuncCtx(SearchContext search)
    {
        this.argGetters = L();
        this.search = search;
        this.uniqueRef = opt(null);
        this.parent = opt(null);
        this.argPsiType = EArgPsiType.NONE;
    }

    public FuncCtx(FuncCtx parentCtx, L<S<MultiType>> argGetters, @NotNull PsiElement uniqueRef, EArgPsiType argPsiType)
    {
        this.argGetters = argGetters;
        this.search = parentCtx.search;
        this.uniqueRef = opt(uniqueRef);
        this.parent = opt(parentCtx);
        this.argPsiType = argPsiType;
        this.fakeFileSource = opt(parentCtx)
            .fop(par -> par.fakeFileSource);
    }

    private MultiType getCached(int index, S<MultiType> argGetter)
    {
        if (!cachedArgs.containsKey(index)) {
            cachedArgs.put(index, MultiType.CIRCULAR_REFERENCE);
            MultiType mt = argGetter.get();
            cachedArgs.replace(index, mt);
        }
        return cachedArgs.get(index);
    }

    private Opt<MultiType> getPassedVariadicPart(int order)
    {
        return variadicOrders.fst()
            .flt(firstVari -> firstVari <= order)
            .map(firstVari -> argGetters.fap((get, i) -> {
                if (i >= firstVari) {
                    MultiType mt = getCached(order, get);
                    if (variadicOrders.contains(i)) {
                        mt = mt.getEl();
                    }
                    return mt.types;
                } else {
                    return list();
                }
            }))
            .map(types -> new MultiType(types));
    }

    public Opt<MultiType> getArg(ArgOrder orderObj)
    {
        Opt<MultiType> fromVariadic = getPassedVariadicPart(orderObj.order);
        if (fromVariadic.has()) {
            if (orderObj.isVariadic) {
                return uniqueRef.map(ref -> fromVariadic.unw().getInArray(ref)).map(t -> new MultiType(list(t)));
            } else {
                return opt(fromVariadic.fap(mt -> mt.types).wap(MultiType::new));
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
                return new MultiType(list(allArgs));
            });
        }
    }

    public Opt<MultiType> getArg(Integer index)
    {
        return getArg(new ArgOrder(index, false));
    }

    public MultiType getArgMt(Integer index)
    {
        return getArg(new ArgOrder(index, false)).def(MultiType.INVALID_PSI);
    }

    @NotNull
    public MultiType findExprType(PhpExpression expr)
    {
        MultiType result = search.findExprType(expr, this).def(new MultiType(L()));
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
                    self.instGetter = opt(() -> findExprType(ref));
                }
            });
        return self;
    }

    public FuncCtx subCtxDirect(NewExpression funcCall)
    {
        return subCtxDirectGeneric(funcCall);
    }

    public FuncCtx subCtxDirectGeneric(ParameterListOwner funcCall)
    {
        L<PsiElement> psiArgs = L(funcCall.getParameters());
        L<S<MultiType>> argGetters = psiArgs.map((psi) -> () -> Tls.cast(PhpExpression.class, psi)
            .uni(arg -> findExprType(arg), () -> MultiType.INVALID_PSI));
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
        L<S<MultiType>> argGetters = list(() -> findExprType(argArr).getEl());
        return new FuncCtx(this, argGetters, argArr, EArgPsiType.ARR);
    }

    /** when you have expression PSI and it is not directly passed to the func, ex. call_user_func_array() */
    public FuncCtx subCtxIndirect(PhpExpression args)
    {
        S<MultiType> getMt = Tls.onDemand(() -> findExprType(args));
        L<S<MultiType>> argGetters = list();
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
    public L<MultiType> getArgs()
    {
        return argGetters.map(g -> g.get());
    }

    public String toString()
    {
        L<PsiElement> parents = L();
        FuncCtx tmp = this;
        while (tmp != null && tmp.uniqueRef.has()) {
            parents.add(tmp.uniqueRef.unw());
            tmp = tmp.parent.def(null);
        }
        return Tls.implode(" | ", parents.map(p -> Tls.singleLine(p.getText(), 600)));
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
