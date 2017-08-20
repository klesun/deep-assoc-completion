package org.klesun.deep_keys.resolvers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
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
        String cls = opt(call.getClassReference())
            .map(clsPsi -> clsPsi.getName()).def("");
        String met = opt(call.getName()).def("");

        if (cls.equals("Fp")) {
            if (met.equals("map")) {
                if (params.length >= 2) {
                    DeepType result = new DeepType(call);
                    PsiElement callback = params[0];
                    PsiElement array = params[1];

                    // TODO: think of a way how to pass them to the function
                    S<MultiType> onDemand = Tls.onDemand(() ->
                            findPsiExprType(array).getEl());
                    L<S<MultiType>> argGetters = list(onDemand::get);
                    IFuncCtx funcCtx = ctx.subCtx(argGetters);

                    findPsiExprType(callback).types.map(t -> t.returnTypes)
                        .fch(rts -> result.indexTypes.addAll(rts));

                    resultTypes.add(result);
                }
            } else if (met.equals("filter")) {
                if (params.length >= 2) {
                    resultTypes.addAll(findPsiExprType(params[1]).types);
                }
            } else if (met.equals("flatten")) {
                if (params.length >= 1) {
                    resultTypes.addAll(findPsiExprType(params[0]).getEl().types);
                }
            } else if (met.equals("groupBy")) {
                if (params.length >= 2) {
                    resultTypes.add(findPsiExprType(params[1]).getInArray(call));
                }
            }
        } else if (cls.equals("ArrayUtil")) {
            if (met.equals("getFirst") || met.equals("getLast")) {
                if (params.length >= 1) {
                    resultTypes.addAll(findPsiExprType(params[0]).getEl().types);
                }
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
        List<Method> meths = list();
        L(PhpIndex.getInstance(proj).getClassesByName(clsName)).s
            .forEach(cls -> meths.addAll(L(cls.getMethods())
                .flt(m -> Objects.equals(m.getName(), func)).s));
        return meths;
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
