package org.klesun.deep_keys.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstantReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.ClassReferenceImpl;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.Objects;

public class ArrCtorRes extends Lang
{
    final private IFuncCtx ctx;

    public ArrCtorRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public static Opt<PhpClass> resolveClass(PsiElement clsPsi)
    {
        return opt(clsPsi.getFirstChild())
            .fap(toCast(ClassConstantReferenceImpl.class))
            .flt(cst -> Objects.equals(cst.getName(), "class"))
            .map(cst -> cst.getClassReference())
            .fap(toCast(ClassReferenceImpl.class))
            .map(clsRef -> clsRef.resolve())
            .fap(toCast(PhpClass.class));
    }

    /** like in [Ns\Employee::class, 'getSalary'] */
    private static Opt<Method> resolveMethodFromArray(L<PsiElement> refParts)
    {
        return refParts.gat(1)
            .map(psi -> psi.getFirstChild())
            .fap(toCast(StringLiteralExpression.class))
            .map(lit -> lit.getContents())
            .fap(met -> refParts.gat(0)
                .fap(clsPsi -> resolveClass(clsPsi))
                .map(cls -> cls.findMethodByName(met)));
    }

    public DeepType resolve(ArrayCreationExpressionImpl expr)
    {
        DeepType arrayType = new DeepType(expr);

        Lang.L<PsiElement> orderedParams = L(expr.getChildren())
            .flt(psi -> !(psi instanceof ArrayHashElement));

        resolveMethodFromArray(orderedParams)
            // TODO: think of a way how to pass args here
            .map(meth -> MethRes.findMethRetType(meth, ctx.subCtx(L())))
            .thn(arrayType.returnTypes::addAll);

        // indexed elements
        orderedParams
            .fch((valuePsi, i) -> Tls.cast(PhpExpression.class, valuePsi)
                // currently each value is wrapped into a plane Psi element
                // i believe this is likely to change in future - so we try both cases
                .elf(() -> opt(valuePsi.getFirstChild()).fap(toCast(PhpExpression.class)))
                .thn(val -> arrayType.addKey(i + "", val)
                    .addType(() -> ctx.findExprType(val))));

        // keyed elements
        L(expr.getHashElements()).fch((keyRec) -> opt(keyRec.getValue())
            .fap(toCast(PhpExpression.class))
            .map(v -> S(() -> ctx.findExprType(v)))
            .thn(getType -> opt(keyRec.getKey())
                .fap(toCast(PhpExpression.class))
                .map(keyPsi -> ctx.findExprType(keyPsi).types)
                .map(keyTypes -> L(keyTypes).fop(t -> opt(t.stringValue)))
                .thn(keyTypes -> {
                    if (keyTypes.s.size() > 0) {
                        keyTypes.fch(key -> arrayType.addKey(key, keyRec).addType(getType));
                    } else {
                        arrayType.indexTypes.addAll(getType.get().types);
                    }
                })));

        return arrayType;
    }

}
