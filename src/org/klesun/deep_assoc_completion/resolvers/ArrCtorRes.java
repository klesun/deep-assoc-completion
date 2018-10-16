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
import org.klesun.deep_assoc_completion.helpers.KeyType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.*;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ArrCtorRes extends Lang
{
    final private FuncCtx ctx;

    public ArrCtorRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public static Set<String> ideaTypeToFqn(@Nullable PhpType ideaType)
    {
        return new HashSet<>(opt(ideaType).def(PhpType.EMPTY).filterUnknown()
            .filterNull().filterMixed().filter(PhpType.OBJECT).getTypes());
    }

    public static It<PhpClass> resolveIdeaTypeCls(PhpType ideaType, Project project)
    {
        return It(ideaTypeToFqn(ideaType))
            .fap(clsPath -> PhpIndex.getInstance(project).getAnyByFQN(clsPath))
            .fop(rvd -> opt(rvd));
    }

    public static It<PhpClass> resolveMtCls(Mt mtArg, Project project)
    {
        It<PhpClass> resolved = opt(mtArg)
            .map(mt -> mt.getIdeaType())
            .fap(tpe -> resolveIdeaTypeCls(tpe, project))
            ;
        if (!resolved.has()) {
            // allow no namespace in php doc class references
            PhpIndex idx = PhpIndex.getInstance(project);
            return It(ideaTypeToFqn(mtArg.getIdeaType()))
                .flt(fqn -> !fqn.isEmpty())
                .fap(clsName -> {
                    clsName = clsName.replaceAll("^\\\\", "");
                    return It.cnc(
                        idx.getClassesByName(clsName),
                        idx.getInterfacesByName(clsName),
                        idx.getInterfacesByName(clsName)
                    );
                });
        }
        return resolved;
    }

    public It<PhpClass> resolveObjCls(PhpExpression expr)
    {
        Mt mt = new Mt(ctx.findExprType(expr));
        return resolveMtCls(mt, expr.getProject());
    }

    public It<PhpClass> resolveInstance(PsiElement instExpr)
    {
        return opt(instExpr.getFirstChild())
            .fop(toCast(PhpExpression.class))
            .fap(xpr -> resolveObjCls(xpr))
            ;
    }

    public static Opt<PhpClass> resolveClass(PsiElement clsPsi)
    {
        return opt(clsPsi.getFirstChild())
            .fop(expr -> Opt.fst(
                () -> Tls.cast(ClassConstantReferenceImpl.class, expr)
                    .flt(cst -> Objects.equals(cst.getName(), "class"))
                    .map(cst -> cst.getClassReference())
                    .fop(toCast(ClassReferenceImpl.class))
                    .map(clsRef -> clsRef.resolve())
                    .fop(toCast(PhpClass.class)),
                () -> Tls.cast(StringLiteralExpression.class, expr)
                    .map(lit -> lit.getContents())
                    .fop(clsName -> Opt.fst(
                        () -> "self".equals(clsName)
                            ? Tls.findParent(clsPsi, PhpClass.class, a -> true)
                            : opt(null),
                        () -> It(PhpIndex.getInstance(expr.getProject())
                            .getAnyByFQN(clsName)).fst()
                    ))
            ));
    }

    /** like in [Ns\Employee::class, 'getSalary'] */
    private It<Method> resolveMethodFromArray(L<PsiElement> refParts)
    {
        return refParts.gat(1)
            .map(psi -> psi.getFirstChild())
            .fop(toCast(StringLiteralExpression.class))
            .map(lit -> lit.getContents())
            .fap(met -> refParts.gat(0)
                .fap(clsPsi -> It.cnc(
                    resolveClass(clsPsi),
                    resolveInstance(clsPsi)
                ))
                .fop(cls -> opt(cls.findMethodByName(met))));
    }

    public DeepType resolve(ArrayCreationExpressionImpl expr)
    {
        DeepType arrayType = new DeepType(expr);

        L<PsiElement> orderedParams = It(expr.getChildren())
            .flt(psi -> !(psi instanceof ArrayHashElement)).arr();

        resolveMethodFromArray(orderedParams)
            .map(meth -> MethCallRes.findMethRetType(meth))
            .fch(retTypeGetter -> arrayType.returnTypeGetters
                .add((ctx) -> new MemoizingIterable<>(retTypeGetter.apply(ctx).iterator())));

        // indexed elements
        orderedParams
            .fch((valuePsi, i) -> Tls.cast(PhpExpression.class, valuePsi)
                // currently each value is wrapped into a plane Psi element
                // i believe this is likely to change in future - so we try both cases
                .elf(() -> opt(valuePsi.getFirstChild()).fop(toCast(PhpExpression.class)))
                .thn(val -> arrayType.addKey(i + "", ctx.getRealPsi(val))
                    .addType(() -> ctx.findExprType(val).wap(Mt::new), Tls.getIdeaType(val))));

        // keyed elements
        It(expr.getHashElements()).fch((keyRec) -> opt(keyRec.getValue())
            .fop(toCast(PhpExpression.class))
            .thn(v -> {
                S<Mt> getType = Tls.onDemand(() -> ctx.findExprType(v).wap(Mt::new));
                opt(keyRec.getKey())
                    .fop(toCast(PhpExpression.class))
                    .map(keyPsi -> ctx.findExprType(keyPsi))
                    .map(keyTypes -> keyTypes.fop(t -> opt(t.stringValue)))
                    .thn(keyStrValues -> {
                        if (keyStrValues.has()) {
                            keyStrValues.fch(key -> arrayType
                                .addKey(key, ctx.getRealPsi(keyRec))
                                .addType(getType, Tls.getIdeaType(v)));
                        } else {
                            arrayType.addKey(KeyType.unknown(keyRec)).addType(getType);
                        }
                    });
            }));

        return arrayType;
    }
}
