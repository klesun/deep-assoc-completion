package org.klesun.deep_keys;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides mechanism to determine expression type.
 * Unlike original jetbrain's type resolver, this
 * includes associative array key information
 */
public class DeepTypeResolver extends Lang
{
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

    private static String dumpPsi(PsiElement psi, Integer level)
    {
        String result = "";
        result += "" + level + " - " + psi.getClass() + "\n";
        for (PsiElement subPsi: psi.getChildren()) {
            result += dumpPsi(subPsi, level + 1);
        }
        return result;
    }

    private static Opt<List<DeepType>> parseExpression(String expr, int depth, Project project)
    {
        expr = "<?php\n" + expr + ";";
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr);

        return opt(psiFile.getFirstChild())
            .fap(toCast(GroupStatement.class))
            .map(gr -> gr.getFirstPsiChild())
            .fap(toCast(Statement.class))
            .map(st -> st.getFirstChild())
            .fap(toCast(PhpExpression.class))
            .map(ex -> findExprType(ex, depth));
    }

    public static Opt<List<DeepType>> parseDoc(String descr, int depth, Project project)
    {
        Pattern pattern = Pattern.compile("^\\s*=\\s*(.+)$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(descr);
        if (matcher.matches()) {
            return parseExpression(matcher.group(1), depth, project);
        } else {
            return opt(null);
        }
    }

    private static List<String> parseRegexNameCaptures(String regexText)
    {
        List<String> result = list();
        Pattern pattern = Pattern.compile("\\(\\?P<([a-zA-Z_0-9]+)>");
        Matcher matcher = pattern.matcher(regexText);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static List<DeepType> makeRegexNameCaptureTypes(List<DeepType> regexTypes)
    {
        return L(regexTypes)
            .fop(strt -> opt(strt.stringValue)
                .map(s -> parseRegexNameCaptures(s))
                .map(names -> {
                    DeepType t = new DeepType(strt.definition, PhpType.ARRAY);
                    names.forEach(n -> t.addKey(n, strt.definition)
                        .types.add(new DeepType(strt.definition, PhpType.STRING)));
                    return t;
                }))
            .s;
    }

    public static Opt<Assign> findParamType(ParameterImpl param, int depth)
    {
        return opt(param.getDocComment())
            .map(doc -> doc.getParamTagByName(param.getName()))
            .fap(doc -> opt(doc.getTagValue())
                .fap(descr -> parseDoc(descr, depth, param.getProject()))
                .map(parsed -> new Assign(list(), parsed, true, doc)));
    }

    /**
     * @return type of array element from array type
     */
    private static List<DeepType> makeArrElType(List<DeepType> arrTypes)
    {
        List<DeepType> result = list();
        arrTypes.forEach(arrt -> {
            result.addAll(arrt.indexTypes);
            arrt.keys.forEach((k,v) -> result.addAll(v.types));
        });
        return result;
    }

    /**
     * @return type of array containing multiple instances of passed element
     */
    private static DeepType makeElArrType(L<DeepType> elTypes, PsiElement call)
    {
        DeepType result = new DeepType(call, PhpType.ARRAY);
        result.indexTypes.addAll(elTypes);
        return result;
    }

    private static Opt<List<DeepType>> assertForeachElement(PsiElement varRef, int depth)
    {
        return opt(varRef.getParent())
            .fap(toCast(ForeachImpl.class))
            .flt(fch -> fch.getValue().isEquivalentTo(varRef))
            .map(fch -> fch.getArray())
            .map(arr -> findExprType(arr, depth))
            .map(arrTypes -> makeArrElType(arrTypes));
    }

    private static Opt<List<DeepType>> assertTupleAssignment(PsiElement varRef, int depth)
    {
        return opt(varRef.getParent())
            .fap(toCast(MultiassignmentExpressionImpl.class))
            .fap(multi -> opt(multi.getValue())
                /** array creation is wrapped inside an "expression impl" */
                .map(v -> v.getFirstPsiChild())
                .map(val -> findExprType(val, depth))
                .flt(types -> types.size() > 0)
                .fap(arrts -> opt(multi.getVariables())
                    .fap(vars -> {
                        for (Integer i = 0; i < vars.size(); ++i) {
                            if (vars.get(i).isEquivalentTo(varRef)) {
                                return opt(i);
                            }
                        }
                        return opt(null);
                    })
                    .map(i -> L(arrts)
                        .fop(t -> opt(t.keys.get(i + ""))
                            .map(k -> k.types))
                        .fap(v -> v).s
                    )
                )
            );
    }

    private static Opt<List<DeepType>> assertPregMatchResult(PsiElement varRef, int depth)
    {
        return opt(varRef.getParent())
            .fap(toCast(ParameterListImpl.class))
            .flt(lst -> L(lst.getParameters()).gat(2)
                .map(psi -> psi.isEquivalentTo(varRef))
                .def(false))
            .map(lst -> lst.getParent())
            .fap(toCast(FunctionReferenceImpl.class))
            .flt(fun -> fun.getName().equals("preg_match"))
            .fap(fun -> L(fun.getParameters()).fst())
            .map(regexPsi -> findExprType(regexPsi, depth))
            .map(regexTypes -> makeRegexNameCaptureTypes(regexTypes));
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
                .thn(refPsi -> opt(refPsi)
                    .flt(v -> ScopeFinder.didPossiblyHappen(v, variable))
                    .fap(varRef -> {
                        boolean didSurelyHappen = ScopeFinder.didSurelyHappen(res.getElement(), variable);
                        return Tls.findParent(varRef, AssignmentExpressionImpl.class, par -> par instanceof ArrayAccessExpression)
                            .fap(ass -> collectKeyAssignment(ass, depth)
                                .map(tup -> new Assign(tup.a, tup.b, didSurelyHappen, ass)))
                            .elf(() -> assertForeachElement(varRef, depth)
                                .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, varRef)))
                            .elf(() -> assertTupleAssignment(varRef, depth)
                                .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, varRef)))
                            .elf(() -> assertPregMatchResult(varRef, depth)
                                .map(varTypes -> new Assign(list(), varTypes, didSurelyHappen, varRef)))
                            .thn(assign -> {
                                reversedAssignments.add(assign);
                                if (didSurelyHappen && assign.keys.size() == 0) {
                                    // direct assignment, everything before it is meaningless
                                    finished.set(true);
                                }
                            });
                    })
                    .els(() -> Tls.cast(ParameterImpl.class, refPsi)
                        .fap(param -> findParamType(param, depth))
                        .thn(assign -> reversedAssignments.add(assign))));
        }

        List<Assign> assignments = list();
        for (int i = reversedAssignments.size() - 1; i >= 0; --i) {
            assignments.add(reversedAssignments.get(i));
        }

        return assignmentsToTypes(assignments);
    }

    /**
     * similar to built-in functions. by "Util" i mean some custom
     * functions that do general stuff, like map/filter/sort/etc...
     * currently hardcoded with one of my team project functions, in future
     * should be customizable either in plugin settings or by a separate plugin
     */
    private static List<DeepType> findUtilMethCallTypes(MethodReferenceImpl call, int depth)
    {
        List<DeepType> resultTypes = list();
        PsiElement[] params = call.getParameters();
        String cls = opt(call.getClassReference()).map(c -> c.getName()).def("");
        String met = call.getName();

        if (cls.equals("Fp")) {
            if (met.equals("map")) {
                if (params.length >= 2) {
                    DeepType result = new DeepType(call);
                    PsiElement callback = params[0];
                    PsiElement array = params[1];
                    for (DeepType callbackType: findExprType(callback, depth)) {
                        result.indexTypes.addAll(callbackType.returnTypes);
                    }
                    resultTypes.add(result);
                }
            } else if (met.equals("filter")) {
                if (params.length >= 2) {
                    resultTypes.addAll(findExprType(params[1], depth));
                }
            } else if (met.equals("flatten")) {
                if (params.length >= 1) {
                    resultTypes.addAll(makeArrElType(findExprType(params[0], depth)));
                }
            } else if (met.equals("groupBy")) {
                if (params.length >= 2) {
                    resultTypes.add(makeElArrType(findExprType(params[1], depth), call));
                }
            }
        } else if (cls.equals("ArrayUtil")) {
            if (met.equals("getFirst") || met.equals("getLast")) {
                if (params.length >= 1) {
                    resultTypes.addAll(makeArrElType(findExprType(params[0], depth)));
                }
            }
        }
        return resultTypes;
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
                        result.indexTypes.addAll(callbackType.returnTypes);
                    }
                }
                return opt(list(result));
            } else if (name.equals("array_filter") || name.equals("array_reverse")
                    || name.equals("array_splice") || name.equals("array_slice")
                    || name.equals("array_values")
            ) {
                return params.length > 0
                    ? opt(findExprType(params[0], depth))
                    : opt(list());
            } else if (name.equals("array_combine")) {
                // if we store all literal values of a key, we could even recreate the
                // associative array here, but for now just merging all possible values together
                return params.length > 1
                    ? opt(findExprType(params[1], depth))
                    : opt(list());
            } else if (name.equals("array_pop") || name.equals("array_shift")) {
                return params.length > 0
                    ? opt(findExprType(params[0], depth))
                        .map(arrt -> makeArrElType(arrt))
                    : opt(list());
            } else if (name.equals("array_merge")) {
                List<DeepType> types = list();
                for (PsiElement paramPsi: params) {
                    types.addAll(findExprType(paramPsi, depth));
                }
                return opt(types);
            } else if (name.equals("array_column")) {
                if (params.length > 1) {
                    L<DeepType> elType = L(makeArrElType(findExprType(params[0], depth)));
                    return Tls.cast(StringLiteralExpression.class, params[1])
                        .map(lit -> lit.getContents())
                        .map(keyName -> elType
                            .fop(type -> getKey(type.keys, keyName))
                            .fap(keyRec -> keyRec.types))
                            .map(lTypes -> lTypes.s)
                        .flt(types -> types.size() > 0)
                        .map(types -> {
                            DeepType type = new DeepType(call);
                            type.indexTypes.addAll(types);
                            return list(type);
                        });
                } else {
                    return opt(list());
                }
            } else if (name.equals("array_chunk")) {
                if (params.length > 0) {
                    return opt(list(makeElArrType(findExprType(params[0], depth), call)));
                } else {
                    return opt(list());
                }
            } else if (name.equals("array_intersect_key") || name.equals("array_diff_key")
                    || name.equals("array_intersect_assoc") || name.equals("array_diff_assoc")
                ) {
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

    public static List<DeepType> findFuncRetType(PsiElement meth, int depth)
    {
        List<DeepType> possibleTypes = list();

        findFunctionReturns(meth)
            .forEach(ret -> opt(ret.getArgument())
            .map(arg -> findExprType(arg, depth))
            .thn(possibleTypes::addAll));

        return possibleTypes;
    }

    private static L<Method> findOverridingMethods(Method meth)
    {
        return opt(PhpIndex.getInstance(meth.getProject()))
            .map(idx -> idx.getAllSubclasses(meth.getContainingClass().getFQN()))
            .map(clses -> L(clses))
            .def(L())
            .fop(cls -> opt(cls.findMethodByName(meth.getName())));
    }

    private static List<DeepType> findMethRetType(Method meth, int depth)
    {
        if (meth.isAbstract()) {
            // return all implementations
            return findOverridingMethods(meth).fap(m -> findFuncRetType(m, depth)).s;
        } else {
            return findFuncRetType(meth, depth);
        }
    }

    private static DeepType findLambdaType(FunctionImpl lambda, int depth)
    {
        DeepType result = new DeepType(lambda, lambda.getLocalType(true));
        findFuncRetType(lambda, depth).forEach(result.returnTypes::add);
        return result;
    }

    private static List<DeepType> findKeyType(ArrayAccessExpressionImpl keyAccess, int depth)
    {
        List<DeepType> dictTypes = findExprType(keyAccess.getValue(), depth);

        return opt(keyAccess.getIndex())
            .map(v -> v.getValue())
            .map(v -> findExprType(v, depth))
            .fap(keyTypes -> L(keyTypes).fst())
            .map(t -> t.stringValue)
            .map(key -> dictTypes.stream()
                .map(type -> getKey(type.keys, key))
                .filter(v -> v.has())
                .map(v -> v.def(null))
                .flatMap(v -> v.types.stream())
                .collect(Collectors.toList())
            )
            .flt(types -> types.size() > 0)
            .def(dictTypes.stream()
                .map(type -> type.indexTypes)
                .flatMap(v -> v.stream())
                .collect(Collectors.toList()));
    }

    private static DeepType findArrCtorType(ArrayCreationExpressionImpl expr, int depth)
    {
        DeepType arrayType = new DeepType(expr);

        L<PsiElement> orderedParams = L(expr.getChildren())
            .flt(psi -> !(psi instanceof ArrayHashElement));

        resolveMethodFromArray(orderedParams)
            .map(meth -> findMethRetType(meth, depth))
            .thn(arrayType.returnTypes::addAll);

        // indexed elements
        orderedParams
            .fch((valuePsi, i) -> Tls.cast(PhpExpression.class, valuePsi)
                // currently each value is wrapped into a plane Psi element
                // i believe this is likely to change in future - so we try both cases
                .elf(() -> opt(valuePsi.getFirstChild()).fap(toCast(PhpExpression.class)))
                .thn(val -> arrayType.addKey(i + "", val).types.addAll(findExprType(val, depth))));

        // keyed elements
        L(expr.getHashElements()).fch((keyRec) -> opt(keyRec.getValue())
            .map(v -> findExprType(v, depth))
            .thn(types -> opt(keyRec.getKey())
                .map(keyPsi -> findExprType(keyPsi, depth))
                .map(keyTypes -> L(keyTypes).fop(t -> opt(t.stringValue)))
                .thn(keyTypes -> {
                    if (keyTypes.s.size() > 0) {
                        keyTypes.fch(key -> arrayType.addKey(key, keyRec).types.addAll(types));
                    } else {
                        arrayType.indexTypes.addAll(types);
                    }
                })));

        return arrayType;
    }

    private static List<Method> resolveMethodsNoNs(String clsName, String func, Project proj)
    {
        List<Method> meths = list();
        L(PhpIndex.getInstance(proj).getClassesByName(clsName)).s
            .forEach(cls -> meths.addAll(L(cls.getMethods())
                .flt(m -> Objects.equals(m.getName(), func)).s));
        return meths;
    }

    /** like in [Ns\Employee::class, 'getSalary'] */
    private static Opt<Method> resolveMethodFromArray(L<PsiElement> refParts)
    {
        return refParts.gat(1)
            .map(psi -> psi.getFirstChild())
            .fap(toCast(StringLiteralExpression.class))
            .map(lit -> lit.getContents())
            .fap(met -> refParts.gat(0)
                .map(psi -> psi.getFirstChild())
                .fap(toCast(ClassConstantReferenceImpl.class))
                .flt(cst -> Objects.equals(cst.getName(), "class"))
                .map(cst -> cst.getClassReference())
                .fap(toCast(ClassReferenceImpl.class))
                .map(clsRef -> clsRef.resolve())
                .fap(toCast(PhpClass.class))
                .map(cls -> cls.findMethodByName(met)));
    }

    private static Opt<L<Method>> resolveMethodFromCall(MethodReferenceImpl call)
    {
        return Opt.fst(list(opt(null)
            , opt(L(call.multiResolve(false)))
                .map(l -> l.map(v -> v.getElement()))
                .map(l -> l.fop(toCast(Method.class)))
                .flt(l -> l.s.size() > 0)
            , opt(call.getClassReference())
                .map(cls -> resolveMethodsNoNs(cls.getName(), call.getName(), call.getProject()))
                .map(meths -> L(meths))
                .flt(l -> l.s.size() > 0)
        ));
    }

    private static Opt<List<DeepType>> findFieldType(FieldReferenceImpl fieldRef, int depth)
    {
        List<DeepType> result = list();
        opt(fieldRef.resolve())
            .thn(resolved -> {
                Tls.cast(FieldImpl.class, resolved)
                    .map(fld -> fld.getDefaultValue())
                    .map(def -> findExprType(def, depth))
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
                    .fap(expr -> findExprType(expr, depth))
                    .fch(t -> result.add(t));
            });

        return opt(result);
    }

    public static L<DeepType> findExprType(PsiElement expr, int depth)
    {
        if (depth <= 0) {
            return list();
        }
        final int nextDepth = --depth;

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
                .fap(call -> resolveMethodFromCall(call)
                    .map(funcs -> funcs.fap(func -> findMethRetType(func, nextDepth)))
                    .map(retTypes -> retTypes.cct(findUtilMethCallTypes(call, nextDepth))))
            , Tls.cast(ArrayAccessExpressionImpl.class, expr)
                .map(keyAccess -> findKeyType(keyAccess, nextDepth))
            , Tls.cast(StringLiteralExpressionImpl.class, expr)
                .map(lit -> list(new DeepType(lit)))
            , Tls.cast(PhpExpressionImpl.class, expr)
                .map(v -> v.getFirstChild())
                .fap(toCast(FunctionImpl.class))
                .map(lambda -> list(findLambdaType(lambda, nextDepth)))
            , Tls.cast(PhpExpressionImpl.class, expr)
                .fap(casted -> opt(casted.getText())
                    .flt(text -> Tls.regex("^\\d+$", text).has())
                    .map(Integer::parseInt)
                    .map(num -> list(new DeepType(casted, num))))
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
            , Tls.cast(FieldReferenceImpl.class, expr)
                .fap(fieldRef -> findFieldType(fieldRef, nextDepth))
            , Tls.cast(PhpExpression.class, expr)
                .map(t -> list(new DeepType(t)))
//            , Tls.cast(ConstantReferenceImpl.class, expr)
//                .map(cnst -> list(new DeepType(cnst)))
        ))
            .map(types -> L(types))
            .els(() -> System.out.println("Unknown expression type " + expr.getText() + " " + expr.getClass()))
            .def(list());
    }

}
