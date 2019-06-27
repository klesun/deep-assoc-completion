package org.klesun.deep_assoc_completion.contexts;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.ArgOrder;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.*;

import java.util.HashMap;

/** a node in called function stack trace with args */
public class FuncCtx extends Lang implements IFuncCtx
{
    enum EArgPsiType {DIRECT, ARR, NONE, INDIRECT, SELF_CLS, THIS_CLS};

    final private Opt<FuncCtx> parent;
    final public Opt<PsiElement> uniqueRef;
    final private SearchCtx search;
    final private L<S<Mt>> argGetters;
    private L<Integer> variadicOrders = L();
    public Opt<Lang.S<Mt>> instGetter = opt(null);
    public Opt<PhpType> clsIdeaType = opt(null);
    public L<T2<String, S<MemIt<DeepType>>>> closureVars = list();
    final private EArgPsiType argPsiType;
    /** use this when you need to reference a real PSI during parsing of PHP Doc */
    public Opt<PsiElement> fakeFileSource = opt(null);
    private L<StackTraceElement> debugCtorTrace = L();

    private HashMap<Integer, Mt> cachedArgs = new HashMap<>();

    public FuncCtx(SearchCtx search)
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
                        .addType(() -> mt, mt.getIdeaTypes().fst().def(PhpType.EMPTY)));
                return new Mt(list(allArgs));
            });
        }
    }

    // when self::doSomething is used in non-static context and doSomething() is a non-static method
    // I believe this is deprecated in PHP 7, but there are few places in our framework that use this
    public static boolean isWhitelistedStaticThis(PhpExpression ref)
    {
        Opt<PhpClass> clsOpt = Tls.findParent(ref, PhpClass.class, a -> true);
        return clsOpt.map(cls -> cls.getFQN()).any(fqn -> fqn.startsWith("\\Dyninno\\Core\\Database\\"));
    }

    private void setThisType(MemberReference memRef, F<PhpExpression, It<DeepType>> findExprType)
    {
        opt(memRef.getClassReference())
            .thn(clsRef -> {
                this.clsIdeaType = opt(clsRef.getType());
                if (!(clsRef instanceof ClassReference) || isWhitelistedStaticThis(memRef)) {
                    this.instGetter = opt(() -> new Mt(findExprType.apply(clsRef)));
                }
            });
    }

    public FuncCtx subCtxDirect(FunctionReference funcCall, F<PhpExpression, It<DeepType>> findExprType)
    {
        FuncCtx self = subCtxDirectGeneric(funcCall, findExprType);
        Tls.cast(MethodReference.class, funcCall)
            .thn(methCall -> self.setThisType(methCall, findExprType));
        return self;
    }

    /** the property name passed to the __get($propName) */
    public FuncCtx subCtxMagicProp(FieldReference fieldRef, F<PhpExpression, It<DeepType>> findExprType)
    {
        Opt<S<Mt>> argGetter = Opt.fst(
            () -> opt(fieldRef.getName())
                .flt(nme -> !"".equals(nme))
                .map(nme -> () -> new DeepType(fieldRef, PhpType.STRING, nme).mt()),
            () -> It(fieldRef.getChildren())
                .flt((c,i) -> i > 0) // skip first psi, it is the object var
                .cst(Variable.class)
                .fst()
                .map(vari -> () -> new Mt(findExprType.apply(vari)))
        );
        FuncCtx subCtx = new FuncCtx(this, argGetter.arr(), fieldRef, EArgPsiType.DIRECT);
        subCtx.setThisType(fieldRef, findExprType);
        return subCtx;
    }

    public FuncCtx subCtxMem(MemberReference fieldRef, F<PhpExpression, It<DeepType>> findExprType)
    {
        FuncCtx subCtx = new FuncCtx(this, L(), fieldRef, EArgPsiType.DIRECT);
        subCtx.setThisType(fieldRef, findExprType);
        return subCtx;
    }

    public FuncCtx subCtxSelfCls(PhpClass clsPsi)
    {
        FuncCtx subCtx = new FuncCtx(this, L(), clsPsi, EArgPsiType.SELF_CLS);
        subCtx.clsIdeaType = som(clsPsi.getType());
        return subCtx;
    }

    public FuncCtx subCtxThisCls(PhpClass clsPsi)
    {
        FuncCtx subCtx = new FuncCtx(this, L(), clsPsi, EArgPsiType.THIS_CLS);
        PhpType pst = clsPsi.getType();
        subCtx.clsIdeaType = som(pst);
        subCtx.instGetter = som(() -> new DeepType(clsPsi, pst).mt());
        return subCtx;
    }

    public FuncCtx subCtxDirect(NewExpression funcCall, F<PhpExpression, It<DeepType>> findExprType)
    {
        FuncCtx self = subCtxDirectGeneric(funcCall, findExprType);
        opt(funcCall.getClassReference())
            .thn(ref -> self.clsIdeaType = opt(ref.getType()));
        return self;
    }

    public FuncCtx subCtxDirectGeneric(ParameterListOwner funcCall, F<PhpExpression, It<DeepType>> findExprType)
    {
        L<PsiElement> psiArgs = L(funcCall.getParameters());
        L<S<Mt>> argGetters = psiArgs.map((psi) -> S(() ->
            Tls.cast(PhpExpression.class, psi) .uni(
                arg -> new Mt(findExprType.apply(arg)),
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
    public FuncCtx subCtxSingleArgArr(PhpExpression argArr, int argOrder, F<PhpExpression, It<DeepType>> findExprType)
    {
        L<S<Mt>> argGetters = Tls.range(0, argOrder + 1)
            .map(i -> S(() -> i == argOrder
                ? findExprType.apply(argArr).fap(Mt::getElSt).wap(Mt::new)
                : Mt.INVALID_PSI))
            .arr();
        return new FuncCtx(this, argGetters, argArr, EArgPsiType.ARR);
    }

    /** when you have expression PSI and it is not directly passed to the func, ex. call_user_func_array() */
    public FuncCtx subCtxIndirect(PhpExpression args, F<PhpExpression, It<DeepType>> findExprType)
    {
        S<Mt> getMt = Tls.onDemand(() -> new Mt(findExprType.apply(args)));
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

    public FuncCtx withClosure(L<T2<String, S<MemIt<DeepType>>>> closureVars, IFuncCtx outsideCtx)
    {
        FuncCtx closCtx = new FuncCtx(parent.def(this), argGetters, uniqueRef.def(null), EArgPsiType.INDIRECT);
        closCtx.closureVars = closureVars; // probably should add to caching criteria...
        closCtx.fakeFileSource = this.fakeFileSource;
        closCtx.clsIdeaType = outsideCtx.getSelfType();
        closCtx.instGetter = outsideCtx.getInstGetter();
        closCtx.variadicOrders = this.variadicOrders;
        closCtx.cachedArgs = this.cachedArgs;
        return closCtx;
    }

    /** will return false if function was called with 0 arguments or args unknown */
    public boolean hasArgs()
    {
        // this probably must also include clsIdeaType.has()...
        return argGetters.size() > 0 || instGetter.has();
    }

    /** will return true if function was called with 0 arguments */
    public boolean areArgsKnown()
    {
        return !list(EArgPsiType.NONE, EArgPsiType.SELF_CLS, EArgPsiType.THIS_CLS).contains(argPsiType);
    }

    public SearchCtx getSearch()
    {
        return search;
    }

    public Opt<PhpType> getSelfType()
    {
        return clsIdeaType;
    }

    public Opt<S<Mt>> getInstGetter()
    {
        return instGetter;
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
}
