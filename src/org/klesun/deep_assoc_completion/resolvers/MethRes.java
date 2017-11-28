package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;
import java.util.Objects;

public class MethRes extends Lang
{
    private IFuncCtx ctx;

    public MethRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private MultiType findPsiExprType(PsiElement psi)
    {
        return Tls.cast(PhpExpression.class, psi)
            .map(casted -> ctx.findExprType(casted))
            .def(new MultiType(L()));
    }

    public static boolean nameIs(MethodReferenceImpl call, String cls, String mth)
    {
        String callCls = opt(call.getClassReference())
            .map(clsPsi -> clsPsi.getName()).def("");
        String callMet = opt(call.getName()).def("");

        return callCls.equals(cls) && callMet.equals(mth);
    }

    /**
     * similar to built-in functions. by "Util" i mean some custom
     * functions that do general stuff, like map/filter/sort/etc...
     * currently hardcoded with one of my team project functions, in future
     * should be customizable either in plugin settings or by a separate plugin
     */
    private L<DeepType> findUtilMethCallTypes(MethodReferenceImpl call)
    {
        L<DeepType> resultTypes = list();
        PsiElement[] params = call.getParameters();

        if (nameIs(call, "Fp", "map")) {
            if (params.length >= 2) {
                DeepType result = new DeepType(call);
                PsiElement callback = params[0];
                PsiElement array = params[1];
                findPsiExprType(callback).types.map(t -> t.returnTypes)
                    .fch(rts -> result.indexTypes.addAll(rts));

                resultTypes.add(result);
            }
        } else if (nameIs(call, "Fp", "filter")) {
            if (params.length >= 2) {
                resultTypes.addAll(findPsiExprType(params[1]).types);
            }
        } else if (nameIs(call, "Fp", "flatten")) {
            if (params.length >= 1) {
                resultTypes.addAll(findPsiExprType(params[0]).getEl().types);
            }
        } else if (nameIs(call, "Fp", "groupBy")) {
            if (params.length >= 2) {
                resultTypes.add(findPsiExprType(params[1]).getInArray(call));
            }
        } else if (nameIs(call, "ArrayUtil", "getFirst")
                || nameIs(call, "ArrayUtil", "getLast")
        ) {
            if (params.length >= 1) {
                resultTypes.addAll(findPsiExprType(params[0]).getEl().types);
            }
        }
        return resultTypes;
    }

    private static L<Method> findOverridingMethods(Method meth)
    {
        return opt(PhpIndex.getInstance(meth.getProject()))
            .fap(idx -> opt(meth.getContainingClass())
                .map(cls -> idx.getAllSubclasses(cls.getFQN())))
            .map(clses -> L(clses))
            .def(L())
            .fop(cls -> opt(cls.findMethodByName(meth.getName())));
    }

    public static L<DeepType> findMethRetType(Method meth, IFuncCtx funcCtx)
    {
        L<Method> impls = meth.isAbstract()
            ? findOverridingMethods(meth)
            : list(meth);
        return impls
            .fap(m -> ClosRes.findFunctionReturns(m))
            .map(ret -> ret.getArgument())
            .fop(toCast(PhpExpression.class))
            .map(retVal -> funcCtx.findExprType(retVal))
            .fap(mt -> mt.types);
    }

    private static List<Method> resolveMethodsNoNs(String clsName, String func, Project proj)
    {
        PhpIndex idx = PhpIndex.getInstance(proj);
        return new L<PhpClass>()
            .cct(L(idx.getClassesByName(clsName)))
            .cct(L(idx.getInterfacesByName(clsName)))
            .cct(L(idx.getTraitsByName(clsName)))
            .fap(cls -> L(cls.getMethods()))
            .flt(m -> Objects.equals(m.getName(), func));
    }

    private static Opt<L<Method>> resolveMethodFromCall(MethodReferenceImpl call)
    {
        return Opt.fst(list(opt(null)
            , opt(L(call.multiResolve(false)))
                .map(l -> l.map(v -> v.getElement()))
                .map(l -> l.fop(toCast(Method.class)))
                .flt(l -> l.s.size() > 0)
            , opt(call.getClassReference())
                .map(cls -> resolveMethodsNoNs(cls.getName(), call.getName(), call.getProject()))
                .map(meths -> L(meths))
                .flt(l -> l.s.size() > 0)
        ));
    }

    public MultiType resolveCall(MethodReferenceImpl funcCall)
    {
        L<PsiElement> args = L(funcCall.getParameters());
        L<S<MultiType>> argGetters = args.map((psi) -> () -> findPsiExprType(psi));
        IFuncCtx funcCtx = ctx.subCtx(argGetters);
        return new MultiType(list(
            findUtilMethCallTypes(funcCall),
            resolveMethodFromCall(funcCall)
                .map(funcs -> funcs.fap(func -> findMethRetType(func, funcCtx)))
            .def(list())
        ).fap(a -> a));
    }
}
