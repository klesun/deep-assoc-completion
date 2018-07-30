package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstantReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.ClassReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.Objects;

public class ArrCtorRes extends Lang
{
    final private FuncCtx ctx;

    public ArrCtorRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public static L<PhpClass> resolveMtCls(MultiType mtArg, Project project)
    {
        L<PhpClass> resolved = opt(mtArg)
            .map(mt -> mt.getIdeaType())
            .map(tpe -> L(tpe.filterUnknown().filterNull().filterMixed().filter(PhpType.OBJECT).getTypes())
                .fap(clsPath -> L(PhpIndex.getInstance(project).getAnyByFQN(clsPath)))
                .fop(rvd -> opt(rvd)))
            .fap(clses -> clses);
        if (resolved.size() == 0) {
            // allow no namespace in php doc class references
            PhpIndex idx = PhpIndex.getInstance(project);
            L(mtArg.getIdeaType().filterUnknown().filterNull()
                .filterMixed().filter(PhpType.OBJECT).getTypes()).flt(fqn -> !fqn.isEmpty())
                .fch(clsName -> {
                    clsName = clsName.replaceAll("^\\\\", "");
                    resolved.addAll(idx.getClassesByName(clsName));
                    resolved.addAll(idx.getInterfacesByName(clsName));
                    resolved.addAll(idx.getInterfacesByName(clsName));
                });
        }
        return resolved;
    }

    public L<PhpClass> resolveObjCls(PhpExpression expr)
    {
        MultiType mt = opt(expr)
            .map(xpr -> ctx.findExprType(xpr))
            .def(MultiType.INVALID_PSI);
        return resolveMtCls(mt, expr.getProject());
    }

    public L<PhpClass> resolveInstance(PsiElement instExpr)
    {
        return opt(instExpr.getFirstChild())
            .fop(toCast(PhpExpression.class))
            .fap(xpr -> resolveObjCls(xpr))
            ;
    }

    public static Opt<PhpClass> resolveClass(PsiElement clsPsi)
    {
        return opt(clsPsi.getFirstChild())
            .fop(expr -> Opt.fst(list(
                Tls.cast(ClassConstantReferenceImpl.class, expr)
                    .flt(cst -> Objects.equals(cst.getName(), "class"))
                    .map(cst -> cst.getClassReference())
                    .fop(toCast(ClassReferenceImpl.class))
                    .map(clsRef -> clsRef.resolve())
                    .fop(toCast(PhpClass.class)),
                Tls.cast(StringLiteralExpression.class, expr)
                    .map(lit -> lit.getContents())
                    .fop(clsName -> Opt.fst(list(
                        "self".equals(clsName)
                            ? Tls.findParent(clsPsi, PhpClass.class, a -> true)
                            : opt(null),
                        L(PhpIndex.getInstance(expr.getProject())
                            .getAnyByFQN(clsName)).gat(0)
                    )))
            )));
    }

    /** like in [Ns\Employee::class, 'getSalary'] */
    private L<Method> resolveMethodFromArray(L<PsiElement> refParts)
    {
        return refParts.gat(1)
            .map(psi -> psi.getFirstChild())
            .fop(toCast(StringLiteralExpression.class))
            .map(lit -> lit.getContents())
            .fap(met -> refParts.gat(0)
                .fap(clsPsi -> list(
                    resolveClass(clsPsi).fap(a -> list(a)),
                    resolveInstance(clsPsi)
                ))
                .fap(a -> a)
                .fop(cls -> opt(cls.findMethodByName(met))));
    }

    public DeepType resolve(ArrayCreationExpressionImpl expr)
    {
        DeepType arrayType = new DeepType(expr);

        L<PsiElement> orderedParams = L(expr.getChildren())
            .flt(psi -> !(psi instanceof ArrayHashElement));

        resolveMethodFromArray(orderedParams)
            .map(meth -> MethCallRes.findMethRetType(meth))
            .fch(retTypeGetter -> arrayType.returnTypeGetters.add(retTypeGetter));

        // indexed elements
        orderedParams
            .fch((valuePsi, i) -> Tls.cast(PhpExpression.class, valuePsi)
                // currently each value is wrapped into a plane Psi element
                // i believe this is likely to change in future - so we try both cases
                .elf(() -> opt(valuePsi.getFirstChild()).fop(toCast(PhpExpression.class)))
                .thn(val -> arrayType.addKey(i + "", ctx.getRealPsi(val))
                    .addType(() -> ctx.findExprType(val), Tls.getIdeaType(val))));

        // keyed elements
        L(expr.getHashElements()).fch((keyRec) -> opt(keyRec.getValue())
            .fop(toCast(PhpExpression.class))
            .thn(v -> {
                S<MultiType> getType = S(() -> ctx.findExprType(v));
                opt(keyRec.getKey())
                    .fop(toCast(PhpExpression.class))
                    .map(keyPsi -> ctx.findExprType(keyPsi).types)
                    .map(keyTypes -> L(keyTypes).fop(t -> opt(t.stringValue)))
                    .thn(keyStrValues -> {
                        if (keyStrValues.size() > 0) {
                            keyStrValues.fch(key -> arrayType
                                .addKey(key, ctx.getRealPsi(keyRec))
                                .addType(getType, Tls.getIdeaType(v)));
                        } else {
                            arrayType.anyKeyElTypes.add(getType);
                        }
                    });
            }));

        return arrayType;
    }
}
