package org.klesun.deep_keys;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides mechanism to determine expression type.
 * Unlike original jetbrain's type resolver, this
 * includes associative array key information
 */
public class DeepTypeResolver extends Lang
{
    private static <T extends PsiElement> F<PsiElement, Opt<T>> toFindParent(Class<T> cls, Predicate<PsiElement> continuePred)
    {
        return (psi) -> {
            PsiElement parent = psi.getParent();
            while (parent != null) {
                Opt<T> matching = Tls.cast(cls, parent);
                if (matching.has()) {
                    return matching;
                } else if (!continuePred.test(parent)) {
                    break;
                }
                parent = parent.getParent();
            }
            return opt(null);
        };
    }

    /**
     * extends type with the key assignment information
     */
    private static void addAssignment(List<DeepType> dest, Assign assign, boolean overwritingAssignment)
    {
        if (assign.keys.size() == 0) {
            if (assign.didSurelyHappen && !overwritingAssignment) {
                dest.clear();
            }
            assign.assignedType.forEach(dest::add);
        } else {
            if (dest.size() == 0) {
                dest.add(new DeepType(assign.psi, PhpType.ARRAY));
            }
            String nextKey = assign.keys.remove(0);
            dest.forEach(type -> {
                if (nextKey == null) {
                    // index key
                    addAssignment(type.indexTypes, assign, true);
                } else {
                    // associative key
                    if (!type.keys.containsKey(nextKey)) {
                        type.addKey(nextKey, assign.psi);
                    }
                    addAssignment(type.keys.get(nextKey).types, assign, false);
                }
            });
            assign.keys.add(0, nextKey);
        }
    }

    private static List<DeepType> assignmentsToTypes(List<Assign> assignments)
    {
        List<DeepType> resultTypes = list();

        // assignments are supposedly in chronological order
        assignments.forEach(ass -> addAssignment(resultTypes, ass, true));

        return resultTypes;
    }

    // null in key chain means index (when it is number or variable, not named key)
    private static Opt<T2<List<String>, List<DeepType>>> collectKeyAssignment(AssignmentExpressionImpl ass, int depth)
    {
        Opt<ArrayAccessExpressionImpl> nextKeyOpt = opt(ass.getVariable())
            .fap(toCast(ArrayAccessExpressionImpl.class));

        List<String> reversedKeys = list();

        while (nextKeyOpt.has()) {
            ArrayAccessExpressionImpl nextKey = nextKeyOpt.def(null);

            opt(nextKey.getIndex())
                .map(index -> index.getValue())
                .thn(key -> Tls.cast(StringLiteralExpressionImpl.class, key)
                    .thn(lit -> reversedKeys.add(lit.getContents()))
                    .els(() -> reversedKeys.add(null))) // index, not named key
                .els(() -> reversedKeys.add(null));

            nextKeyOpt = opt(nextKey.getValue())
                .fap(toCast(ArrayAccessExpressionImpl.class));
        }

        List<String> keys = list();
        for (int i = reversedKeys.size() - 1; i >= 0; --i) {
            keys.add(reversedKeys.get(i));
        }

        return opt(ass.getValue())
            .map(value -> new T2(keys, findExprType(value, depth)));
    }

    private static List<DeepType> findVarType(Variable variable, int depth)
    {
        List<Assign> reversedAssignments = list();
        ResolveResult[] references = variable.multiResolve(false);

        Mutable<Boolean> finished = new Mutable<>(false);
        for (int i = references.length - 1; i >= 0; --i) {
            if (finished.get()) break;

            ResolveResult res = references[i];
            opt(res.getElement())
                .flt(v -> ScopeFinder.didPossiblyHappen(v, variable))
                .fap(toFindParent(AssignmentExpressionImpl.class, par -> par instanceof ArrayAccessExpression))
                .fap(ass -> collectKeyAssignment(ass, depth)
                    .thn(tup -> {
                        boolean didSurelyHappen = ScopeFinder.didSurelyHappen(res.getElement(), variable);
                        reversedAssignments.add(new Assign(tup.a, tup.b, didSurelyHappen, ass));
                        if (didSurelyHappen && tup.a.size() == 0) {
                            // direct assignment, everything before it is meaningless
                            finished.set(true);
                        }
                    }));
        }

        List<Assign> assignments = list();
        for (int i = reversedAssignments.size() - 1; i >= 0; --i) {
            assignments.add(reversedAssignments.get(i));
        }

        return assignmentsToTypes(assignments);
    }

    private static Opt<List<DeepType>> findBuiltInFuncCallType(FunctionReferenceImpl call, int depth)
    {
        return opt(call.getName()).fap(name -> {
            PsiElement[] params = call.getParameters();
            if (name.equals("array_map")) {
                DeepType result = new DeepType(call);
                if (params.length > 1) {
                    PsiElement callback = params[0];
                    PsiElement array = params[1];
                    for (DeepType callbackType: findExprType(callback, depth)) {
                        for (DeepType indexType: callbackType.returnTypes) {
                            result.indexTypes.add(indexType);
                        }
                    }
                }
                return opt(list(result));
            } else if (name.equals("array_filter")) {
                return params.length > 0
                    ? opt(findExprType(params[0], depth))
                    : opt(list());
            } else if (name.equals("array_merge")) {
                List<DeepType> types = list();
                for (PsiElement paramPsi: params) {
                    types.addAll(findExprType(paramPsi, depth));
                }
                return opt(types);
            } else if (name.equals("array_intersect_key")) {
                // do something more clever?
                return params.length > 0
                    ? opt(findExprType(params[0], depth))
                    : opt(list());
            } else {
                return opt(null);
            }
        });
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

    private static DeepType findLambdaType(FunctionImpl lambda, int depth)
    {
        DeepType result = new DeepType(lambda, lambda.getInferredType(false));
        findFuncRetType(lambda, depth).forEach(result.returnTypes::add);
        return result;
    }

    private static List<DeepType> findKeyType(ArrayAccessExpressionImpl keyAccess, int depth)
    {
        List<DeepType> dictTypes = findExprType(keyAccess.getValue(), depth);

        return opt(keyAccess.getIndex())
            .map(v -> v.getValue())
            .fap(toCast(StringLiteralExpressionImpl.class))
            .map(lit -> lit.getContents())
            .map(key -> dictTypes.stream()
                .map(type -> getKey(type.keys, key))
                .filter(v -> v.has())
                .map(v -> v.def(null))
                .flatMap(v -> v.types.stream())
                .collect(Collectors.toList())
            )
            .def(dictTypes.stream()
                .map(type -> type.indexTypes)
                .flatMap(v -> v.stream())
                .collect(Collectors.toList()));
    }

    private static DeepType findArrCtorType(ArrayCreationExpressionImpl expr, int depth)
    {
        DeepType arrayType = new DeepType(expr);

        expr.getHashElements().forEach(keyRec -> opt(keyRec.getValue())
            .map(v -> findExprType(v, depth))
            .thn(types -> opt(keyRec.getKey())
                .fap(keyPsi -> Tls.cast(StringLiteralExpressionImpl.class, keyPsi))
                .map(lit -> lit.getContents())
                .uni(
                    key -> arrayType.addKey(key, keyRec).types.addAll(types),
                    () -> arrayType.indexTypes.addAll(types)
                )));

        return arrayType;
    }

    public static List<DeepType> findExprType(PsiElement expr, int depth)
    {
        if (depth <= 0) {
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
                .fap(call -> findBuiltInFuncCallType(call, nextDepth))
            , Tls.cast(FunctionReferenceImpl.class, expr)
                .map(call -> call.resolve())
                .map(func -> findFuncRetType(func, nextDepth))
            , Tls.cast(MethodReferenceImpl.class, expr)
                .map(call -> call.resolve())
                .map(func -> findFuncRetType(func, nextDepth))
            , Tls.cast(ArrayAccessExpressionImpl.class, expr)
                .map(keyAccess -> findKeyType(keyAccess, nextDepth))
            , Tls.cast(StringLiteralExpressionImpl.class, expr)
                .map(lit -> list(new DeepType(lit)))
            , Tls.cast(PhpExpressionImpl.class, expr)
                .map(v -> v.getFirstChild())
                .fap(toCast(FunctionImpl.class))
                .map(lambda -> list(findLambdaType(lambda, nextDepth)))
            , Tls.cast(TernaryExpressionImpl.class, expr)
                .map(tern -> Stream.concat(
                    findExprType(tern.getTrueVariant(), nextDepth).stream(),
                    findExprType(tern.getFalseVariant(), nextDepth).stream()
                ).collect(Collectors.toList()))
            , Tls.cast(BinaryExpressionImpl.class, expr)
                // found this dealing with null coalescing, but
                // i suppose this rule will apply for all operators
                .map(bin -> Stream.concat(
                    findExprType(bin.getLeftOperand(), nextDepth).stream(),
                    findExprType(bin.getRightOperand(), nextDepth).stream()
                ).collect(Collectors.toList()))
            , Tls.cast(PhpExpressionImpl.class, expr)
                .map(mathExpr -> list(new DeepType(mathExpr)))
            , Tls.cast(PhpExpressionImpl.class, expr)
                .map(t -> list(new DeepType(t)))
        ))
            .els(() -> System.out.println("Unknown expression type " + expr.getText() + " " + expr.getClass()))
            .def(new ArrayList<>());
    }

    /**
     * contains info related to assignment to a
     * specific associative array variable key
     */
    private static class Assign
    {
        final public List<String> keys;
        // list?
        final public List<DeepType> assignedType;
        // when true, that means this assignment happens _always_,
        // i.e. it is not inside an "if" branch or a loop
        final public boolean didSurelyHappen;
        final public PsiElement psi;

        public Assign(List<String> keys, List<DeepType> assignedType, boolean didSurelyHappen, PsiElement psi)
        {
            this.keys = keys;
            this.assignedType = assignedType;
            this.didSurelyHappen = didSurelyHappen;
            this.psi = psi;
        }
    }
}
