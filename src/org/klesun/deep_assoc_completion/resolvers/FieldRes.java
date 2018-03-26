package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.*;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;

public class FieldRes extends Lang
{
    final private FuncCtx ctx;

    public FieldRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private static L<FieldReferenceImpl> findReferences(PsiFile file, String name)
    {
        // ReferenceSearch seems to cause freezes
//        SearchScope scope = GlobalSearchScope.fileScope(
//            fieldRef.getProject(),
//            decl.getContainingFile().getVirtualFile()
//        );
//        return ReferencesSearch.search(decl, scope, false).findAll();

        return L(PsiTreeUtil.findChildrenOfType(file, FieldReferenceImpl.class))
            .flt(ref -> name.equals(ref.getName()));
    }

    private boolean isCircularExpr(FieldReferenceImpl fieldRef)
    {
        L<PhpExpression> psiTrace = ctx.getSearch().psiTrace;
        // -1, because last always will be this PSI, not a circular reference
        for (int i = 0; i < psiTrace.size() - 1; ++i) {
            if (psiTrace.get(i).isEquivalentTo(fieldRef)) {
                return true;
            }
        }
        return false;
    }

    private L<Field> findReferenced(FieldReferenceImpl fieldRef)
    {
//         return opt(fieldRef.resolve()).fop(toCast(Field.class));
        return opt(fieldRef.getClassReference())
            .fap(obj -> new ArrCtorRes(ctx).resolveObjCls(obj))
            .fap(cls -> L(cls.getFields()))
            .flt(f -> f.getName().equals(fieldRef.getName()));
    }

    public MultiType resolve(FieldReferenceImpl fieldRef)
    {
        if (isCircularExpr(fieldRef)) {
            return MultiType.CIRCULAR_REFERENCE;
        }

        L<DeepType> result = list();
        findReferenced(fieldRef)
            .fch(resolved -> {
                FuncCtx implCtx = new FuncCtx(ctx.getSearch());
                Tls.cast(FieldImpl.class, resolved)
                    .map(fld -> fld.getDefaultValue())
                    .fop(toCast(PhpExpression.class))
                    .map(def -> implCtx.findExprType(def).types)
                    .thn(result::addAll);

                L<Assign> asses = opt(resolved.getContainingFile())
                    .map(file -> findReferences(file, fieldRef.getName()))
                    .def(L())
                    .fap(psi -> Tls.findParent(psi, Method.class, a -> true)
                        .flt(meth -> meth.getName().equals("__construct"))
                        .map(meth -> fieldRef.getClassReference())
                        .fop(toCast(PhpExpression.class))
                        .fop(ref -> ref.getText().equals("$this")
                            ? ctx.instGetter.map(g -> g.get())
                            : opt(ctx.findExprType(ref)))
                        .fap(mt -> mt.getArgsPassedToCtor())
                        .wap(ctxs -> ctxs.size() > 0 ? ctxs : list(implCtx))
                        .fop(methCtx -> (new AssRes(methCtx)).collectAssignment(psi, false)));

                List<DeepType> types = AssRes.assignmentsToTypes(asses);
                result.addAll(types);
            });

        return new MultiType(result);
    }

}
