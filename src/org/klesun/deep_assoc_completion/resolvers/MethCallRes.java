package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;
import java.util.Objects;

public class MethCallRes extends Lang
{
    private IFuncCtx ctx;

    public MethCallRes(IFuncCtx ctx)
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

    private static L<Method> findOverridingMethods(Method meth)
    {
        return opt(PhpIndex.getInstance(meth.getProject()))
            .fop(idx -> opt(meth.getContainingClass())
                .map(cls -> idx.getAllSubclasses(cls.getFQN())))
            .map(clses -> L(clses))
            .def(L())
            .fop(cls -> opt(cls.findMethodByName(meth.getName())));
    }

    private static Opt<DeepType> parseSqlSelect(DeepType strType)
    {
        DeepType parsedType = new DeepType(strType.definition, PhpType.ARRAY);
        String regex = "SELECT\\s+(.*?)(\\s+FROM.*)?";
        Tls.regex(regex, opt(strType.stringValue).def(""))
            .fop(matches -> matches.gat(0))
            .fap(fields -> L(fields.split(",", -1)))
            .map(str -> str.trim())
            .fop(f -> Tls.regex("(\\S+.*?\\.)?(\\S+\\s+[aA][sS]\\s+)?(\\S+)", f))
            .fop(m -> m.gat(2))
            .fch(name -> parsedType.addKey(name, strType.definition)
                .addType(() -> new MultiType(list(new DeepType(strType.definition, PhpType.STRING))), PhpType.STRING));
        return opt(parsedType);
    }

    private MultiType findBuiltInRetType(Method meth, IFuncCtx argCtx, MethodReference methCall)
    {
        L<DeepType> types = L();
        String clsNme = opt(meth.getContainingClass()).map(cls -> cls.getName()).def("");
        if (clsNme.equals("PDO") && meth.getName().equals("query") ||
            clsNme.equals("PDO") && meth.getName().equals("prepare")
        ) {
            L<DeepType> parsedSql = argCtx.getArg(0)
                .fap(mt -> mt.types)
                .fop(type -> parseSqlSelect(type));
            DeepType type = new DeepType(methCall);
            type.pdoTypes.addAll(parsedSql);
            types.add(type);
        } else if (clsNme.equals("PDOStatement") && meth.getName().equals("fetch")) {
            L<DeepType> pdoTypes = opt(methCall.getClassReference())
                .fop(toCast(PhpExpression.class))
                .map(obj -> ctx.findExprType(obj))
                .fap(mt -> mt.types.fap(t -> t.pdoTypes));
            types.addAll(pdoTypes);
        }
        return new MultiType(types);
    }

    public static F<IFuncCtx, L<DeepType>> findMethRetType(Method meth)
    {
        L<Method> impls = meth.isAbstract()
            ? findOverridingMethods(meth)
            : list(meth);
        return (IFuncCtx funcCtx) -> impls
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
        L<DeepType> rtypes = resolveMethodFromCall(funcCall)
            .map(funcs -> list(
                funcs.fap(func -> findMethRetType(func).apply(funcCtx)),
                funcs.fap(func -> findBuiltInRetType(func, funcCtx, funcCall).types)
            ).fap(a -> a))
            .def(list());
        return new MultiType(rtypes);
    }
}
