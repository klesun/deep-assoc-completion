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

    private static MultiType makeType(L<KeyType> keys, S<MultiType> getType, PsiElement psi, PhpType briefType)
    {
        if (keys.size() == 0) {
            return getType.get();
        } else {
            DeepType arr = new DeepType(psi, PhpType.ARRAY);
            KeyType nextKey = keys.get(0);
            L<KeyType> furtherKeys = keys.sub(1);
            if (nextKey.keyType == KeyType.EKeyType.STRING) {
                nextKey.getNameToMt().fch((types, name) ->
                    types.fch(t -> arr.addKey(name, t.definition).addType(() ->
                        makeType(furtherKeys, getType, psi, briefType), briefType)));
            } else  if (nextKey.keyType == KeyType.EKeyType.INTEGER) {
                arr.hasIntKeys = true;
                arr.listElTypes.add(() -> makeType(furtherKeys, getType, psi, briefType));
            } else {
                arr.anyKeyElTypes.add(() -> makeType(furtherKeys, getType, psi, briefType));
            }
            return new MultiType(list(arr));
        }
    }

    public static L<DeepType> assignmentsToTypes(List<Assign> asses)
    {
        L<DeepType> resultTypes = list();
        for (Assign ass: asses) {
            resultTypes.addAll(makeType(L(ass.keys), ass.assignedType, ass.psi, ass.briefType).types);
        }
        return resultTypes;
    }

    // null in key chain means index (when it is number or variable, not named key)
    private Opt<T2<List<KeyType>, S<MultiType>>> collectKeyAssignment(AssignmentExpressionImpl ass)
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
                .map(mt -> KeyType.mt(mt))
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
            .map(value -> T2(keys, S(() -> {
                MultiType mt = ctx.findExprType(value);
                return mt;
            })));
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
