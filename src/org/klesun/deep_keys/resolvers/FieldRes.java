package org.klesun.deep_keys.resolvers;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_keys.*;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

public class FieldRes extends Lang
{
    final private IFuncCtx ctx;

    public FieldRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public MultiType resolve(FieldReferenceImpl fieldRef)
    {
        L<DeepType> result = list();
        opt(fieldRef.resolve())
            .thn(resolved -> {
                IFuncCtx implCtx = ctx.subCtx(L());
                Tls.cast(FieldImpl.class, resolved)
                    .map(fld -> fld.getDefaultValue())
                    .fap(toCast(PhpExpression.class))
                    .map(def -> implCtx.findExprType(def).types)
                    .thn(result::addAll);

                opt(resolved.getOriginalElement())
                    .map(decl -> {
                        SearchScope scope = GlobalSearchScope.fileScope(
                            fieldRef.getProject(),
                            decl.getContainingFile().getVirtualFile()
                        );
                        return ReferencesSearch.search(decl, scope, false).findAll();
                    })
                    .map(usages -> L(usages).map(u -> u.getElement()))
                    .def(L())
                    .fop(psi -> opt(psi.getParent()))
                    .fop(toCast(AssignmentExpressionImpl.class))
                    .fop(ass -> opt(ass.getValue()))
                    .fop(toCast(PhpExpression.class))
                    .fap(expr -> implCtx.findExprType(expr).types)
                    .fch(t -> result.add(t));
            });

        return new MultiType(result);
    }

}
