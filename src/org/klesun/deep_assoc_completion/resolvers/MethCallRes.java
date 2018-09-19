package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.database.model.DasObject;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.*;

import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
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

    public static It<Method> findOverridingMethods(Method meth)
    {
        return opt(PhpIndex.getInstance(meth.getProject()))
            .fop(idx -> opt(meth.getContainingClass())
                .map(cls -> idx.getAllSubclasses(cls.getFQN())))
            .fap(clses -> L(clses))
            .fop(cls -> opt(cls.findMethodByName(meth.getName())));
    }

    private static L<DasObject> getDasChildren(DasObject parent, ObjectKind kind)
    {
        // return getDasChildren(ObjectKind.COLUMN);
        return L(parent.getDbChildren(DasObject.class, kind));
    }

    private static It<String> getTableColumns(String table, Project project)
    {
        return L(DbPsiFacade.getInstance(project).getDataSources())
            .fap(src -> L(src.getModel().getModelRoots()))
            .fap(root -> getDasChildren(root, ObjectKind.TABLE))
            .flt(t -> t.getName().equals(table))
            .fap(tab -> getDasChildren(tab, ObjectKind.COLUMN))
            .map(col -> col.getName());
    }

    private DeepType parseSqlSelect(DeepType strType, Project project)
    {
        DeepType parsedType = new DeepType(strType.definition, PhpType.ARRAY);
        String sql = opt(strType.stringValue).def("");
        int regexFlags = Pattern.DOTALL | Pattern.CASE_INSENSITIVE;
        Opt<L<String>> matched = Opt.fst(
            () -> Tls.regex("SELECT\\s+(\\S.*?)\\s+FROM\\s+([A-Za-z_][A-Za-z0-9_]*)?.*?", sql, regexFlags),
            () -> Tls.regex("SELECT\\s+(\\S.*)", sql, regexFlags) // partial SQL without FROM
        );
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
            .addType(() -> new Mt(list(new DeepType(strType.definition, PhpType.STRING))), PhpType.STRING));
        return parsedType;
    }

    private static It<String> getBindVars(DeepType sqlStrT)
    {
        Matcher matcher = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)")
            .matcher(opt(sqlStrT.stringValue).def(""));
        boolean hasFirst = matcher.find();
        return It(() -> new Iterator<String>() {
            boolean hasNext = hasFirst;
            public boolean hasNext() {
                return hasNext;
            }
            public String next() {
                String var = matcher.group(1);
                hasNext = matcher.find();
                return var;
            }
        });
    }

    private Mt findBuiltInRetType(Method meth, FuncCtx argCtx, MethodReference methCall)
    {
        It<DeepType> types = It(list());
        String clsNme = opt(meth.getContainingClass()).map(cls -> cls.getName()).def("");
        if (clsNme.equals("PDO") && meth.getName().equals("query") ||
            clsNme.equals("PDO") && meth.getName().equals("prepare")
        ) {
            DeepType type = new DeepType(methCall);
            argCtx.getArg(0)
                .fap(mt -> mt.types)
                .fch(strType -> {
                    DeepType fetchType = parseSqlSelect(strType, meth.getProject());
                    type.pdoFetchTypes.add(fetchType);
                    getBindVars(strType).fch(type.pdoBindVars::add);
                });
            types = It(list(type));
        } else if (clsNme.equals("PDOStatement") && meth.getName().equals("fetch")) {
            It<DeepType> pdoTypes = opt(methCall.getClassReference())
                .fop(toCast(PhpExpression.class))
                .fap(obj -> ctx.findExprType(obj))
                .fap(t -> t.pdoFetchTypes);
            types = It(pdoTypes);
        }
        return new Mt(types);
    }

    private static It<DeepType> parseReturnDoc(PhpDocReturnTag returnDoc, FuncCtx funcCtx)
    {
        FuncCtx docCtx = new FuncCtx(funcCtx.getSearch());
        docCtx.fakeFileSource = opt(returnDoc);
        return Tls.regex("^\\s*(like\\s*|=|)((?:\\[|[a-zA-Z]+[\\(:]|new\\s+).*)$", returnDoc.getTagValue())
            .fop(match -> match.gat(1))
            .fop(expr -> DocParamRes.parseExpression(expr, returnDoc.getProject(), docCtx))
            .def(It.non());
    }

    public static F<FuncCtx, It<DeepType>> findMethRetType(Method meth)
    {
        return (FuncCtx funcCtx) -> {
            It<Method> impls = It(list(meth));
            if (meth.isAbstract()) {
                impls = It.cnc(list(meth), findOverridingMethods(meth));
                // ignore $this and args in implementations
                // since there may be dozens of them (Laravel)
                funcCtx = new FuncCtx(funcCtx.getSearch());
            }
            FuncCtx finalCtx = funcCtx;
            return impls.fap(m -> It.cnc(
                ClosRes.getReturnedValue(m, finalCtx).types,
                opt(meth.getDocComment()).map(doc -> doc.getReturnTag())
                    .fap(tag -> parseReturnDoc(tag, finalCtx)),
                opt(m.getReturnType()).fap(rt -> list(new DeepType(rt, rt.getType())))
            ));
        };
    }

    public static It<Method> resolveMethodsNoNs(MethodReference call, FuncCtx ctx)
    {
        String cls = opt(call.getClassReference()).map(c -> c.getText()).def("");
        String mth = opt(call.getName()).def("");
        if (cls.equals("self") || cls.equals("static")) {
            cls = ctx.fakeFileSource
                .fop(doc -> Tls.findParent(doc, PhpClass.class, a -> true))
                .map(clsPsi -> clsPsi.getFQN())
                .def(cls);
        }
        return MethCallRes.resolveMethodsNoNs(cls, mth, call.getProject());
    }

    private static It<Method> resolveMethodsNoNs(String partialFqn, String func, Project proj)
    {
        PhpIndex idx = PhpIndex.getInstance(proj);
        String justName = L(partialFqn.split("\\\\")).lst().unw();
        return It.cnc(
            L(idx.getClassesByName(justName)),
            L(idx.getInterfacesByName(justName)),
            L(idx.getTraitsByName(justName))
        ).flt(cls -> cls.getFQN().endsWith(partialFqn))
            .fap(cls -> L(cls.getMethods()))
            .flt(m -> Objects.equals(m.getName(), func));
    }

    private It<Method> findReferenced(MethodReferenceImpl fieldRef, FuncCtx ctx)
    {
//         return opt(fieldRef.resolve()).fop(toCast(Method.class));
        return opt(fieldRef.getClassReference())
            .fap(obj -> ctx.clsIdeaType
                .flt(typ -> obj.getText().equals("static"))
                .fap(typ -> ArrCtorRes.resolveIdeaTypeCls(typ, obj.getProject()))
                .def(new ArrCtorRes(ctx).resolveObjCls(obj)))
            .fap(cls -> L(cls.getMethods()))
            .flt(f -> f.getName().equals(fieldRef.getName()));
    }

    private Opt<It<Method>> resolveMethodFromCall(MethodReferenceImpl call, FuncCtx ctx)
    {
        return Opt.fst(() -> opt(null)
            , () -> opt(findReferenced(call, ctx)).flt(found -> found.has())
            , () -> opt(It(call.multiResolve(false)))
                .map(l -> l.map(v -> v.getElement()))
                .map(l -> l.fop(toCast(Method.class)))
                .flt(l -> l.has())
            , () -> opt(resolveMethodsNoNs(call, ctx))
                .flt(l -> l.has())
        );
    }

    public Mt resolveCall(MethodReferenceImpl funcCall)
    {
        FuncCtx funcCtx = ctx.subCtxDirect(funcCall);
        L<DeepType> rtypes = resolveMethodFromCall(funcCall, ctx)
            .fap(funcs -> funcs)
            .fap(func -> It.cnc(
                findMethRetType(func).apply(funcCtx),
                findBuiltInRetType(func, funcCtx, funcCall).types
            )).arr();
        return new Mt(rtypes);
    }
}
