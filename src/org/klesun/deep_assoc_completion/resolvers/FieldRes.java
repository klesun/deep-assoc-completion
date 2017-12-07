package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.*;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.List;

public class FieldRes extends Lang
{
    final private IFuncCtx ctx;

    public FieldRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private static MultiType makeType(L<String> keys, S<MultiType> getType, PsiElement psi)
    {
        if (keys.size() == 0) {
            return getType.get();
        } else {
            DeepType arr = new DeepType(psi, PhpType.ARRAY);
            String nextKey = keys.get(0);
            L<String> furtherKeys = keys.sub(1);
            if (nextKey == null) {
                arr.indexTypes = makeType(furtherKeys, getType, psi).types;
            } else {
                arr.addKey(nextKey, psi).addType(() -> makeType(furtherKeys, getType, psi));
            }
            return new MultiType(list(arr));
        }
    }

    private static List<DeepType> assignmentsToTypes(List<Assign> asses)
    {
        List<DeepType> resultTypes = list();
        for (Assign ass: asses) {
            resultTypes.addAll(makeType(L(ass.keys), ass.assignedType, ass.psi).types);
        }
        return resultTypes;
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

    public MultiType resolve(FieldReferenceImpl fieldRef)
    {
        if (isCircularExpr(fieldRef)) {
            return MultiType.CIRCULAR_REFERENCE;
        }

        L<DeepType> result = list();
        opt(fieldRef.resolve())
            .thn(resolved -> {
                IFuncCtx implCtx = ctx.subCtx(L());
                Tls.cast(FieldImpl.class, resolved)
                    .map(fld -> fld.getDefaultValue())
                    .fap(toCast(PhpExpression.class))
                    .map(def -> implCtx.findExprType(def).types)
                    .thn(result::addAll);

                L<Assign> asses = opt(resolved.getOriginalElement())
                    .map(decl -> {
                        SearchScope scope = GlobalSearchScope.fileScope(
                            fieldRef.getProject(),
                            decl.getContainingFile().getVirtualFile()
                        );
                        return ReferencesSearch.search(decl, scope, false).findAll();
                    })
                    .map(usages -> L(usages).map(u -> u.getElement()))
                    .def(L())
                    .fop(psi -> (new AssRes(implCtx)).collectAssignment(psi, false));

                List<DeepType> types = assignmentsToTypes(asses);
                result.addAll(types);
            });

        return new MultiType(result);
    }

}
