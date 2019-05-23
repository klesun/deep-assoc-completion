package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.*;

public class ClosRes extends Lang
{
    final private IExprCtx ctx;

    public ClosRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    // for some reason this implementation is 100 ms slower than the one that returns a list
//    public static It<PhpReturnImpl> findFunctionReturns(PsiElement funcBody)
//    {
//        return It(funcBody.getChildren()).fap(child -> {
//            if (child instanceof Function) {
//                return It.non(); // anonymous function, don't go deeper
//            } else {
//                return It.cnc(
//                    Tls.cast(PhpReturnImpl.class, child),
//                    findFunctionReturns(child)
//                );
//            }
//        });
//    }

    public static L<PhpReturnImpl> findFunctionReturns(PsiElement funcBody)
    {
        L<PhpReturnImpl> result = list();
        for (PsiElement child: funcBody.getChildren()) {
            // anonymous functions
            if (child instanceof Function) continue;

            Tls.cast(PhpReturnImpl.class, child)
                .thn(result::add);

            findFunctionReturns(child).forEach(result::add);
        }
        return result;
    }

    private static L<PhpYield> findFunctionYields(PsiElement funcBody)
    {
        L<PhpYield> result = list();
        for (PsiElement child: funcBody.getChildren()) {
            // anonymous functions
            if (child instanceof Function) continue;

            Tls.cast(PhpYield.class, child)
                .thn(result::add);

            findFunctionYields(child).forEach(result::add);
        }
        return result;
    }

    private static It<DeepType> resolveYieldedIterator(PhpYield yld, IExprCtx ctx)
    {
        PhpType pst = PhpType.builder().add("\\Generator").build();
        // yield $pqId => $priceQuote;
        Opt<PhpExpression> keyPsi = non();
        // yield $priceQuote;
        Opt<PhpExpression> valPsi = non();
        // yield from $priceQuotes;
        Boolean gotFrom = false;

        for (PsiElement leaf: Tls.getChildrenWithLeaf(yld)) {
            Opt<PhpExpression> asExpr = Tls.cast(PhpExpression.class, leaf);
            if (leaf.getText().equals("from")) {
                gotFrom = true;
            } else if (asExpr.has()) {
                valPsi = asExpr;
            } else if (valPsi.has() && leaf.getText().equals("=>")) {
                keyPsi = valPsi;
                valPsi = non();
            }
        }
        It<DeepType> vit = valPsi.fap(val -> ctx.findExprType(val));
        if (gotFrom) {
            // could exclude non-iterable types here and set
            // \Generator as fqn type one of these not lazy day
            return vit;
        } else {
            Mt valMt = new Mt(vit);
            KeyType kt = keyPsi
                .map(key -> ctx.findExprType(key))
                .map(kit -> KeyType.mt(kit, yld))
                .def(KeyType.integer(yld));
            DeepType result = new DeepType(yld, pst);
            result.addKey(kt, yld)
                .addType(Tls.onDemand(() -> valMt), PhpType.MIXED);
            return It(som(result));
        }
    }

    public static It<DeepType> getReturnedValue(PsiElement funcBody, IExprCtx ctx)
    {
        return It.cnc(
            findFunctionReturns(funcBody)
                .fop(ret -> opt(ret.getArgument()))
                .fop(toCast(PhpExpression.class))
                .fap(val -> ctx.findExprType(val)),
            findFunctionYields(funcBody)
                .fap(yld -> resolveYieldedIterator(yld, ctx))
        );
    }

    public static It<Variable> getClosureVars(Function func)
    {
        return It(func.getChildren())
            .fop(toCast(PhpUseList.class))
            .fap(u -> It(u.getChildren()))
            .fop(toCast(Variable.class));
    }

    public DeepType resolve(FunctionImpl func)
    {
        DeepType result = new DeepType(func, func.getLocalType(true));
        result.returnTypeGetters.add((callCtx) -> {
            L<T2<String, S<MemIt<DeepType>>>> closureVars = getClosureVars(func)
                .map(closVar -> {
                    S<MemIt<DeepType>> sup = Tls.onDemand(() ->
                        ctx.findExprType(closVar).mem());
                    return T2(closVar.getName(), sup);
                })
                .arr();
            IExprCtx closCtx = callCtx.withClosure(closureVars, ctx);
            return new MemIt<>(getReturnedValue(func, closCtx));
        });
        return result;
    }
}
