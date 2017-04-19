package org.klesun.deep_keys;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
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
import org.mozilla.javascript.ast.StringLiteral;

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

    private static Opt<List<DeepType>> parseExpression(String expr, int depth)
    {
        expr = "<?php\n" + expr + ";";
        Project project = ProjectManager.getInstance().getDefaultProject();
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr);

        return opt(psiFile.getFirstChild())
            .fap(toCast(GroupStatement.class))
            .map(gr -> gr.getFirstPsiChild())
            .fap(toCast(Statement.class))
            .map(st -> st.getFirstChild())
            .fap(toCast(PhpExpression.class))
            .map(ex -> findExprType(ex, depth));
    }

    public static Opt<List<DeepType>> parseDoc(String descr, int depth)
    {
        Pattern pattern = Pattern.compile("^\\s*=\\s*(.+)$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(descr);
        if (matcher.matches()) {
            return parseExpression(matcher.group(1), depth);
        } else {
            return opt(null);
        }
    }

    public static Opt<Assign> findParamType(ParameterImpl param, int depth)
    {
        return opt(param.getDocComment())
            .map(doc -> doc.getParamTagByName(param.getName()))
            .fap(doc -> opt(doc.getTagValue())
                .fap(descr -> parseDoc(descr, depth))
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
                                .map(tup -> new Assign(tup.a, tup.b, didSurelyHappen, ass))
                                .thn(assign -> {
                                    reversedAssignments.add(assign);
                                    if (didSurelyHappen && assign.keys.size() == 0) {
                                        // direct assignment, everything before it is meaningless
                                        finished.set(true);
                                    }
                                }))
                            .elf(() -> opt(varRef.getParent())
                                .fap(toCast(ForeachImpl.class))
                                .flt(fch -> fch.getValue().isEquivalentTo(varRef))
                                .map(fch -> fch.getArray())
                                .map(arr -> findExprType(arr, depth))
                                .map(arrTypes -> makeArrElType(arrTypes))
                                .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, varRef))
                                .thn(assign -> reversedAssignments.add(assign)));
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
        DeepType result = new DeepType(lambda, lambda.getInferredType());
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
            .flt(types -> types.size() > 0)
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

    private static List<Method> resolveMethodsNoNs(String clsName, String func, Project proj)
    {
        List<Method> meths = list();
        L(PhpIndex.getInstance(proj).getClassesByName(clsName)).s
            .forEach(cls -> meths.addAll(L(cls.getMethods())
                .flt(m -> Objects.equals(m.getName(), func)).s));
        return meths;
    }

    private static Opt<PsiElement> resolveMethod(MethodReferenceImpl call)
    {
        return Opt.fst(list(opt(null)
            , opt(call.resolve())
            , opt(call.getClassReference())
                .map(cls -> resolveMethodsNoNs(cls.getName(), call.getName(), call.getProject()))
                .fap(meths -> L(meths).fst())
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
                    .fch(result::add);
            });

        return opt(result);
    }

    public static List<DeepType> findExprType(PsiElement expr, int depth)
    {
        if (depth <= 0) {
            return new ArrayList<>();
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
                .fap(call -> resolveMethod(call))
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
            , Tls.cast(FieldReferenceImpl.class, expr)
                .fap(fieldRef -> findFieldType(fieldRef, nextDepth))
            , Tls.cast(PhpExpression.class, expr)
                .map(t -> list(new DeepType(t)))
//            , Tls.cast(ConstantReferenceImpl.class, expr)
//                .map(cnst -> list(new DeepType(cnst)))
        ))
            .els(() -> System.out.println("Unknown expression type " + expr.getText() + " " + expr.getClass()))
            .def(list());
    }

    /**
     * contains info related to assignment to a
     * specific associative array variable key
     */
    public static class Assign
    {
        final public List<String> keys;
        // list?
        final public List<DeepType> assignedType;
        // when true, that means this assignment happens _always_,
        // i.e. it is not inside an "if" branch or a loop
        final public boolean didSurelyHappen;
        // where to look in GO TO navigation
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
