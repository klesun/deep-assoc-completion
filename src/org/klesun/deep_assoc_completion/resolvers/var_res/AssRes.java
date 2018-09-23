package org.klesun.deep_assoc_completion.resolvers.var_res;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.Assign;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.KeyType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.*;

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

    private static It<DeepType> makeType(L<KeyType> keys, S<? extends Iterable<DeepType>> getType, PsiElement psi, PhpType briefType)
    {
        if (keys.size() == 0) {
            return It(getType.get());
        } else {
            DeepType arr = new DeepType(psi, PhpType.ARRAY);
            KeyType nextKey = keys.get(0);
            L<KeyType> furtherKeys = keys.sub(1);
            if (nextKey.keyType == KeyType.EKeyType.STRING) {
                S<Iterable<DeepType>> memoized = Tls.onDemand(() -> new MemoizingIterable<>(getType.get().iterator()));
                nextKey.getNameToMt().fch((types, name) ->
                    types.fch(t -> arr.addKey(name, t.definition).addType(() ->
                        makeType(furtherKeys, memoized, psi, briefType).wap(Mt::new), briefType)));
            } else  if (nextKey.keyType == KeyType.EKeyType.INTEGER) {
                arr.hasIntKeys = true;
                arr.listElTypes.add(Tls.onDemand(() -> new Mt(makeType(furtherKeys, getType, psi, briefType))));
            } else {
                arr.anyKeyElTypes.add(Tls.onDemand(() -> makeType(furtherKeys, getType, psi, briefType).wap(Mt::new)));
            }
            return It(list(arr));
        }
    }

    public static It<DeepType> assignmentsToTypes(Iterable<Assign> asses)
    {
        return It(asses).fap(ass -> makeType(L(ass.keys), ass.assignedType, ass.psi, ass.briefType));
    }

    // null in key chain means index (when it is number or variable, not named key)
    private Opt<T2<List<KeyType>, S<It<DeepType>>>> collectKeyAssignment(AssignmentExpressionImpl ass)
    {
        Opt<ArrayAccessExpressionImpl> nextKeyOpt = opt(ass.getVariable())
            .fop(toCast(ArrayAccessExpressionImpl.class));

        List<KeyType> reversedKeys = list();

        while (nextKeyOpt.has()) {
            ArrayAccessExpressionImpl nextKey = nextKeyOpt.def(null);

            KeyType name = opt(nextKey.getIndex())
                .map(index -> index.getValue())
                .fop(toCast(PhpExpression.class))
                .map(key -> ctx.findExprType(key))
                .map(tit -> KeyType.mt(new Mt(tit)))
                .def(KeyType.integer());
            reversedKeys.add(name);

            nextKeyOpt = opt(nextKey.getValue())
                .fop(toCast(ArrayAccessExpressionImpl.class));
        }

        List<KeyType> keys = list();
        for (int i = reversedKeys.size() - 1; i >= 0; --i) {
            keys.add(reversedKeys.get(i));
        }

        return opt(ass.getValue())
            .fop(toCast(PhpExpression.class))
            .map(value -> T2(keys, () -> ctx.findExprType(value)));
    }

    private static Opt<AssignmentExpressionImpl> findParentAssignment(PsiElement caretVar) {
        return opt(caretVar.getParent())
            .fop(parent -> Opt.fst(
                () -> Tls.cast(ArrayAccessExpression.class, parent)
                    .fop(acc -> findParentAssignment(acc)),
                () -> Tls.cast(AssignmentExpressionImpl.class, parent)
                    .flt(ass -> opt(ass.getVariable())
                        .map(assVar -> caretVar.isEquivalentTo(assVar))
                        .def(false))
            ));
    }

    /**
     * @param varRef - `$var` reference or `$this->field` reference
     */
    public Opt<Assign> collectAssignment(PsiElement varRef, Boolean didSurelyHappen)
    {
        return findParentAssignment(varRef)
            .fop(ass -> collectKeyAssignment(ass)
                .map(tup -> new Assign(tup.a, tup.b, didSurelyHappen, ass, Tls.getIdeaType(ass))));
    }
}
