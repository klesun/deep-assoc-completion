package org.klesun.deep_keys;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;
import org.klesun.lang.shortcuts.F;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides mechanism to determine expression type.
 * Unlike original jetbrain's type resolver, this
 * includes associative array key information
 */
public class DeepTypeResolver
{
    /** i HATE writing "new " before every usage! */
    private static <T> Opt<T> opt(T value)
    {
        return new Opt(value);
    }

    private static <T> Opt<T> getKey(Map<String, T> dict, String key)
    {
        if (dict.containsKey(key)) {
            return opt(dict.get(key));
        } else {
            return opt(null);
        }
    }

    private static <T> ArrayList<T> list(T... args)
    {
        ArrayList<T> result = new ArrayList<>();
        Collections.addAll(result, args);
        return result;
    }

    private static <T, Tin extends PsiElement> F<Tin, Opt<T>> toCast(Class<T> cls)
    {
        return obj -> Tls.cast(cls, obj);
    }

    private static List<DeepType> findVarType(Variable variable, int depth)
    {
        List<DeepType> possibleTypes = list();
        LinkedHashMap<String, List<DeepType>> assignedKeys = new LinkedHashMap<>();

        ResolveResult[] references = variable.multiResolve(false);

        for (int i = references.length - 1; i >= 0; --i) {
            ResolveResult res = references[i];
            Opt<PsiElement> expr = opt(res.getElement())
                .flt(v -> ScopeFinder.didPossiblyHappen(v, variable))
                .map(v -> v.getParent());

            // $record = ['initialKey' => 123];
            if (expr.fap(toCast(AssignmentExpressionImpl.class))
                .map(v -> v.getValue())
                .map(v -> findExprType(v, depth))
                .thn(possibleTypes::addAll)
                .has()
                ) {
                // direct assignment, everything before it is meaningless
                if (ScopeFinder.didSurelyHappen(res.getElement(), variable)) {
                    break;
                }
            } else {
                // $record['dynamicKey2'] = 345;
                expr.fap(toCast(ArrayAccessExpressionImpl.class))
                    .flt(access -> access.getParent() instanceof AssignmentExpression)
                    .thn(access -> {
                        String key = opt(access.getIndex())
                            .map(index -> index.getValue())
                            .fap(toCast(StringLiteralExpressionImpl.class))
                            .map(lit -> lit.getContents())
                            .def("Error: failed to key");

                        opt(access.getParent())
                            .fap(toCast(AssignmentExpressionImpl.class))
                            .map(v -> findExprType(v, depth))
                            .thn(types -> assignedKeys.put(key, types));
                    });
            }
        }

        // relying on fact they are not supposed to be immutable
        // ... maybe creating new instance would be better after all?
        assignedKeys.entrySet().forEach(
            e -> possibleTypes.forEach(
            t -> t.keys.put(e.getKey(), e.getValue())));

        return possibleTypes;
    }

    private static List<PhpReturnImpl> findFunctionReturns(PsiElement func)
    {
        List<PhpReturnImpl> result = new ArrayList<>();
        for (PsiElement child: func.getChildren()) {
            // anonymous functions
            if (child instanceof Function) continue;

            Tls.cast(PhpReturnImpl.class, child)
                .thn(result::add);

            findFunctionReturns(child).forEach(result::add);
        }
        return result;
    }

    private static List<DeepType> findFuncRetType(PsiElement meth, int depth)
    {
        List<DeepType> possibleTypes = list();

        findFunctionReturns(meth)
            .forEach(ret -> opt(ret.getArgument())
            .map(arg -> findExprType(arg, depth))
            .thn(possibleTypes::addAll));

        return possibleTypes;
    }

    private static List<DeepType> findKeyType(ArrayAccessExpressionImpl keyAccess, int depth)
    {
        return opt(keyAccess.getIndex())
            .map(v -> v.getValue())
            .fap(toCast(StringLiteralExpressionImpl.class))
            .map(lit -> lit.getContents())
            .map(key -> findExprType(keyAccess.getValue(), depth).stream()
                .map(type -> getKey(type.keys, key))
                .filter(v -> v.has())
                .map(v -> v.def(null))
                .flatMap(v -> v.stream())
                .collect(Collectors.toList())
            )
            .def(list());
    }

    private static DeepType findArrCtorType(ArrayCreationExpressionImpl expr, int depth)
    {
        DeepType arrayType = new DeepType(expr);

        expr.getHashElements().forEach(keyRec -> {
            String key = opt(keyRec.getKey())
                .map(keyPsi -> Tls.cast(StringLiteralExpressionImpl.class, keyPsi)
                    .map(lit -> lit.getContents())
                    .def("Error: not literal: " + keyPsi.getText())
                )
                .def("Error: no key in expression");

            opt(keyRec.getValue())
                .map(v -> findExprType(v, depth))
                .thn(types -> arrayType.keys.put(key, types));
        });

        return arrayType;
    }

    public static List<DeepType> findExprType(PsiElement expr, int depth)
    {
        if (depth <= 0) {
            System.out.println("Keys search depth limit reached");
            return new ArrayList<>();
        }
        final int nextDepth = depth - 1;

        return Opt.fst(list(
            opt(null) // for coma formatting
            , Tls.cast(VariableImpl.class, expr)
                .map(v -> findVarType(v, nextDepth))
            , Tls.cast(ArrayCreationExpressionImpl.class, expr)
                .map(arr -> list(findArrCtorType(arr, nextDepth)))
            , Tls.cast(FunctionReferenceImpl.class, expr)
                .map(call -> call.resolve())
                .map(call -> findFuncRetType(call, nextDepth))
            , Tls.cast(MethodReferenceImpl.class, expr)
                .map(call -> call.resolve())
                .map(call -> findFuncRetType(call, nextDepth))
            , Tls.cast(ArrayAccessExpressionImpl.class, expr)
                .map(keyAccess -> findKeyType(keyAccess, nextDepth))
            , Tls.cast(PhpTypedElement.class, expr)
                .map(t -> list(new DeepType(t)))
        ))
            .els(() -> System.out.println("Unknown expression type " + expr.getText() + " " + expr.getClass()))
            .def(new ArrayList<>());
    }
}
