package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.database.model.DasObject;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FieldReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class MethCallRes extends Lang
{
    final private FuncCtx ctx;

    public MethCallRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public static boolean nameIs(MethodReferenceImpl call, String cls, String mth)
    {
        String callCls = opt(call.getClassReference())
            .map(clsPsi -> clsPsi.getName()).def("");
        String callMet = opt(call.getName()).def("");

        return callCls.equals(cls) && callMet.equals(mth);
    }

    public static L<Method> findOverridingMethods(Method meth)
    {
        return opt(PhpIndex.getInstance(meth.getProject()))
            .fop(idx -> opt(meth.getContainingClass())
                .map(cls -> idx.getAllSubclasses(cls.getFQN())))
            .map(clses -> L(clses))
            .def(L())
            .fop(cls -> opt(cls.findMethodByName(meth.getName())));
    }

    private static L<DasObject> getDasChildren(DasObject parent, ObjectKind kind)
    {
        // return getDasChildren(ObjectKind.COLUMN);
        return L(parent.getDbChildren(DasObject.class, kind));
    }

    private static L<String> getTableColumns(String table, Project project)
    {
        return L(DbPsiFacade.getInstance(project).getDataSources())
            .fap(src -> L(src.getModel().getModelRoots()))
            .fap(root -> getDasChildren(root, ObjectKind.TABLE))
            .flt(t -> t.getName().equals(table))
            .fap(tab -> getDasChildren(tab, ObjectKind.COLUMN))
            .map(col -> col.getName());
    }

    private Opt<DeepType> parseSqlSelect(DeepType strType, Project project)
    {
        DeepType parsedType = new DeepType(strType.definition, PhpType.ARRAY);
        String sql = opt(strType.stringValue).def("");
        int regexFlags = Pattern.DOTALL | Pattern.CASE_INSENSITIVE;
        Opt<L<String>> matched = Opt.fst(list(
            Tls.regex("SELECT\\s+(\\S.*?)\\s+FROM\\s+([A-Za-z_][A-Za-z0-9_]*)?.*?", sql, regexFlags),
            Tls.regex("SELECT\\s+(\\S.*)", sql, regexFlags) // partial SQL without FROM
        ));
        matched.fap(matches -> {
            L<String> fields = L(matches.gat(0).def("").split(",", -1));
            String table = matches.gat(1).def("");
            return fields.map(str -> str.trim())
                .fap(f -> {
                    if (f.equals("*")) {
                        return getTableColumns(table, project);
                    } else {
                        return Tls.regex("(\\S+.*?\\.)?(\\S+\\s+AS\\s+)?(\\S+)", f, regexFlags)
                            .fop(m -> m.gat(2))
                            .fap(a -> list(a));
                    }
                });
        }).fch(name -> parsedType.addKey(name, ctx.getRealPsi(strType.definition))
            .addType(() -> new MultiType(list(new DeepType(strType.definition, PhpType.STRING))), PhpType.STRING));
        return opt(parsedType);
    }

    private MultiType findBuiltInRetType(Method meth, FuncCtx argCtx, MethodReference methCall)
    {
        L<DeepType> types = L();
        String clsNme = opt(meth.getContainingClass()).map(cls -> cls.getName()).def("");
        if (clsNme.equals("PDO") && meth.getName().equals("query") ||
            clsNme.equals("PDO") && meth.getName().equals("prepare")
        ) {
            L<DeepType> parsedSql = argCtx.getArg(0)
                .fap(mt -> mt.types)
                .fop(type -> parseSqlSelect(type, meth.getProject()));
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

    private static MultiType parseReturnDoc(PhpDocReturnTag returnDoc, FuncCtx funcCtx)
    {
        FuncCtx docCtx = new FuncCtx(funcCtx.getSearch());
        docCtx.fakeFileSource = opt(returnDoc);
        return Tls.regex("^\\s*(like\\s*|)(.+)$", returnDoc.getTagValue())
            .fop(match -> match.gat(1))
            .fop(expr -> DocParamRes.parseExpression(expr, returnDoc.getProject(), docCtx))
            .def(MultiType.INVALID_PSI);
    }

    public static F<FuncCtx, L<DeepType>> findMethRetType(Method meth)
    {
        L<Method> impls = meth.isAbstract()
            ? findOverridingMethods(meth)
            : list(meth);
        return (FuncCtx funcCtx) -> impls.fap(m -> list(
            ClosRes.getReturnedValue(m, funcCtx).types,
            opt(meth.getDocComment()).map(doc -> doc.getReturnTag())
                .fap(tag -> parseReturnDoc(tag, funcCtx).types)
        ).fap(a -> a));
    }

    public static L<Method> resolveMethodsNoNs(MethodReference call, FuncCtx ctx)
    {
        String cls = opt(call.getClassReference()).map(c -> c.getName()).def("");
        String mth = opt(call.getName()).def("");
        if (cls.equals("self") || cls.equals("static")) {
            cls = ctx.fakeFileSource
                .fop(doc -> Tls.findParent(doc, PhpClass.class, a -> true))
                .map(clsPsi -> clsPsi.getName())
                .def(cls);
        }
        return L(MethCallRes.resolveMethodsNoNs(cls, mth, call.getProject()));
    }

    private static L<Method> resolveMethodsNoNs(String clsName, String func, Project proj)
    {
        PhpIndex idx = PhpIndex.getInstance(proj);
        return new L<PhpClass>()
            .cct(L(idx.getClassesByName(clsName)))
            .cct(L(idx.getInterfacesByName(clsName)))
            .cct(L(idx.getTraitsByName(clsName)))
            .fap(cls -> L(cls.getMethods()))
            .flt(m -> Objects.equals(m.getName(), func));
    }

    private L<Method> findReferenced(MethodReferenceImpl fieldRef)
    {
//         return opt(fieldRef.resolve()).fop(toCast(Method.class));
        return opt(fieldRef.getClassReference())
            .fap(obj -> new ArrCtorRes(ctx).resolveObjCls(obj))
            .fap(cls -> L(cls.getMethods()))
            .flt(f -> f.getName().equals(fieldRef.getName()));
    }

    private Opt<L<Method>> resolveMethodFromCall(MethodReferenceImpl call)
    {
        return Opt.fst(list(opt(null)
            , opt(findReferenced(call)).flt(found -> found.size() > 0)
            , opt(L(call.multiResolve(false)))
                .map(l -> l.map(v -> v.getElement()))
                .map(l -> l.fop(toCast(Method.class)))
                .flt(l -> l.s.size() > 0)
            , opt(resolveMethodsNoNs(call, ctx))
                .flt(l -> l.s.size() > 0)
        ));
    }

    public MultiType resolveCall(MethodReferenceImpl funcCall)
    {
        FuncCtx funcCtx = ctx.subCtxDirect(funcCall);
        L<DeepType> rtypes = resolveMethodFromCall(funcCall)
            .map(funcs -> list(
                funcs.fap(func -> findMethRetType(func).apply(funcCtx)),
                funcs.fap(func -> findBuiltInRetType(func, funcCtx, funcCall).types)
            ).fap(a -> a))
            .def(list());
        return new MultiType(rtypes);
    }
}
