package org.klesun.deep_assoc_completion.resolvers.var_res;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_assoc_completion.Assign;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;

/**
 * provides functions to collect keys of an assignment and to
 * join multiple assignments into an array structure type
 */
public class AssRes extends Lang
{
    private FuncCtx ctx;

    public AssRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    // null in key chain means index (when it is number or variable, not named key)
    private Opt<T2<List<String>, S<MultiType>>> collectKeyAssignment(AssignmentExpressionImpl ass)
    {
        Opt<ArrayAccessExpressionImpl> nextKeyOpt = opt(ass.getVariable())
            .fop(toCast(ArrayAccessExpressionImpl.class));

        List<String> reversedKeys = list();

        while (nextKeyOpt.has()) {
            ArrayAccessExpressionImpl nextKey = nextKeyOpt.def(null);

            String name = opt(nextKey.getIndex())
                .map(index -> index.getValue())
                .fop(toCast(PhpExpression.class))
                .map(key -> ctx.findExprType(key))
                .map(t -> t.getStringValue())
                .def(null);
            reversedKeys.add(name);

            nextKeyOpt = opt(nextKey.getValue())
                .fop(toCast(ArrayAccessExpressionImpl.class));
        }

        List<String> keys = list();
        for (int i = reversedKeys.size() - 1; i >= 0; --i) {
            keys.add(reversedKeys.get(i));
        }

        return opt(ass.getValue())
            .fop(toCast(PhpExpression.class))
            .map(value -> T2(keys, S(() -> {
                MultiType mt = ctx.findExprType(value);
                return mt;
            })));
    }

    private static Opt<AssignmentExpressionImpl> findParentAssignment(PsiElement caretVar) {
        return opt(caretVar.getParent())
            .fop(parent -> Opt.fst(list(
                Tls.cast(ArrayAccessExpression.class, parent)
                    .fop(acc -> findParentAssignment(acc)),
                Tls.cast(AssignmentExpressionImpl.class, parent)
                    .flt(ass -> opt(ass.getVariable())
                        .map(assVar -> caretVar.isEquivalentTo(assVar))
                        .def(false))
            )));
    }

    /**
     * @param varRef - `$var` reference or `$this->field` reference
     */
    public Opt<Assign> collectAssignment(PsiElement varRef, Boolean didSurelyHappen)
    {
        return findParentAssignment(varRef)
            .fop(ass -> collectKeyAssignment(ass)
                .map(tup -> new Assign(tup.a, tup.b, didSurelyHappen, ass, ass.getType())));
    }
}
