package org.klesun.deep_assoc_completion.resolvers.builtins;

import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.JBIterable;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.Build;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Key;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.*;
import org.klesun.lang.iterators.RegexIterator;

import java.util.regex.Pattern;

import static org.klesun.lang.Lang.*;

public class MysqliRes
{
    final private IExprCtx ctx;

    public MysqliRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private static It<String> getTableColumnsInRootLegacy(DasObject root, String table)
    {
        return It(root.getDbChildren(DasObject.class, ObjectKind.TABLE))
            .flt(t -> t.getName().equals(table))
            .fap(tab -> tab.getDbChildren(DasObject.class, ObjectKind.COLUMN))
            .map(col -> col.getName());
    }

    private static It<String> getTableColumnsInRoot(DasObject root, String table)
    {
        S<It<DasObject>> mysqlTables = () ->
            It(root.getDasChildren(ObjectKind.TABLE))
                .map(t -> (DasObject)t);
        S<It<DasObject>> postgresTables = () ->
            It(root.getDasChildren(ObjectKind.SCHEMA))
                .fap(sc -> sc.getDasChildren(ObjectKind.TABLE))
                .map(t -> (DasObject)t);

        return It.frs(mysqlTables, postgresTables)
            .flt(t -> t.getName().equals(table))
            .fap(tab -> tab.getDasChildren(ObjectKind.COLUMN))
            .map(col -> col.getName()).unq();
    }

    public static It<String> getTableColumns(String table, Project project)
    {
        return It(DbPsiFacade.getInstance(project).getDataSources())
            .fap(src -> src.getModel().getModelRoots())
            .fap(root -> It.frs(
                () -> getTableColumnsInRootLegacy(root, table),
                () -> getTableColumnsInRoot(root, table)
            ));
    }

    public DeepType parseSqlSelect(DeepType strType, Project project)
    {
        String sql = opt(strType.stringValue).def("");
        int regexFlags = Pattern.DOTALL | Pattern.CASE_INSENSITIVE;
        Opt<L<String>> matched = Opt.fst(
            () -> Tls.regex("\\s*SELECT\\s+(\\S.*?)\\s+FROM\\s+([A-Za-z_][A-Za-z0-9_]*)?.*?", sql, regexFlags),
            () -> Tls.regex("\\s*SELECT\\s+(\\S.*)", sql, regexFlags) // partial SQL without FROM
        );
        Iterable<Key> keys = matched.fap(matches -> {
            It<String> fields = It(matches.gat(0).def("").split(",", -1));
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
        }).map(name -> new Key(name, ctx.getRealPsi(strType.definition))
            .addType(() -> new Mt(list(new DeepType(strType.definition, PhpType.STRING))), PhpType.STRING));

        return new Build(strType.definition, PhpType.ARRAY)
            .keys(keys)
            .get();
    }

    private static It<String> getBindVars(DeepType sqlStrT)
    {
        String pattern = ":([A-Za-z_][A-Za-z0-9_]*)";
        String text = opt(sqlStrT.stringValue).def("");
        return It(() -> new RegexIterator(pattern, text))
            .map(groups -> groups.get(1));
    }

    /** @param mysqliResultExpr - Nullable */
    public It<DeepType> fetch_assoc(PhpExpression mysqliResultExpr)
    {
        return opt(mysqliResultExpr)
            .fop(toCast(PhpExpression.class))
            .fap(obj -> ctx.findExprType(obj))
            .fap(t -> t.pdoFetchTypes);
    }

    /** @param mysqliResultExpr - Nullable */
    public It<DeepType> fetch_all(PhpExpression mysqliResultExpr, L<PsiElement> callArgs)
    {
        if (!callArgs.gat(0).any(p -> p.getText().equals("MYSQLI_ASSOC") || p.getText().equals("PDO::FETCH_ASSOC"))) {
            return It.non();
        }
        DeepType arrType = opt(mysqliResultExpr)
            .fop(toCast(PhpExpression.class))
            .fap(obj -> ctx.findExprType(obj))
            .fap(t -> t.pdoFetchTypes)
            .wap(rowTit -> Mt.getInArraySt(rowTit, mysqliResultExpr));
        return It(som(arrType));
    }

    public It<DeepType> query(L<PsiElement> callArgs)
    {
        return callArgs.gat(0).cst(PhpExpression.class)
            .fap(expr -> {
                MemIt<DeepType> rowTypes = ctx.findExprType(expr)
                    .flt(strType -> !opt(strType.stringValue).def("").equals(""))
                    .map(strType -> parseSqlSelect(strType, expr.getProject())).mem();
                return It.cnc(
                    som(new DeepType(expr).btw(t -> {
                        // it's not a PDO, but nah
                        rowTypes.itr().fch((rowt, i) -> t.pdoFetchTypes.add(rowt));
                    })),
                    // since PHP 5.4 mysqli_result can also be iterated
                    som(Mt.getInArraySt(It(rowTypes), expr))
                );
            });
    }

    public It<DeepType> resolveOopCall(Method meth, IExprCtx argCtx, MethodReference methCall)
    {
        String clsNme = opt(meth.getContainingClass()).map(cls -> cls.getName()).def("");
        if (clsNme.equals("PDO") && meth.getName().equals("query") ||
            clsNme.equals("PDO") && meth.getName().equals("prepare")
        ) {
            DeepType type = new DeepType(methCall);
            argCtx.func().getArg(0)
                .fap(mt -> mt.types)
                .fch(strType -> {
                    DeepType fetchType = new MysqliRes(ctx)
                        .parseSqlSelect(strType, meth.getProject());
                    type.pdoFetchTypes.add(fetchType);
                    getBindVars(strType).fch(type.pdoBindVars::add);
                });
            return It(list(type));
        } else if (clsNme.equals("PDOStatement") && meth.getName().equals("fetch")
                || clsNme.equals("mysqli_result") && meth.getName().equals("fetch_assoc")
        ) {
            return new MysqliRes(ctx).fetch_assoc(methCall.getClassReference());
        } else if (clsNme.equals("PDOStatement") && meth.getName().equals("fetchAll")
                || clsNme.equals("mysqli_result") && meth.getName().equals("fetch_all")
        ) {
            return new MysqliRes(ctx).fetch_all(methCall.getClassReference(), L(methCall.getParameters()));
        } else if (clsNme.equals("mysqli") && meth.getName().equals("query")) {
            return new MysqliRes(ctx).query(L(methCall.getParameters()));
        } else {
            return It.non();
        }
    }

    public It<DeepType> resolveProceduralCall(String funcName, PsiElement[] args)
    {
        Opt<PhpExpression> oopThis = L(args).gat(0).cst(PhpExpression.class);
        L<PsiElement> oopArgs = L(args).sub(1);
        if (funcName.equals("mysqli_query")) {
            return query(L(args).sub(1));
        } else if (funcName.equals("mysqli_fetch_assoc")) {
            return fetch_assoc(oopThis.def(null));
        } else if (funcName.equals("mysqli_fetch_all")) {
            return fetch_all(oopThis.def(null), oopArgs);
        } else {
            return It.non();
        }
    }
}
