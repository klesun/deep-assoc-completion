import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;
import org.klesun.lang.shortcuts.F;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * provides associative array keys autocomplete
 * using declaration inside the initial function
 * that created this array
 */
public class DeepKeys extends CompletionContributor
{
    /** i HATE writing "new " before every usage! */
    private static <T> Opt<T> opt(T value)
    {
        return new Opt(value);
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

    // ^^ just shortcuts, no logic ^^^
    // vv           logic          vvv

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

    private static List<String> findKeysInMethReturn(PsiElement meth, int depth)
    {
        List<String> result = new ArrayList<>();
        findFunctionReturns(meth)
            .forEach(ret -> opt(ret.getArgument())
            .thn(arg -> findKeysInExpr(arg, depth)
            .thn(keys -> keys.forEach(result::add))));

        return result;
    }

    private static List<String> findKeysInArrCtor(ArrayCreationExpressionImpl expr)
    {
        return StreamSupport.stream(expr.getHashElements().spliterator(), false)
            .map(el -> opt(el.getKey())
                .map(keyPsi -> Tls.cast(StringLiteralExpressionImpl.class, keyPsi)
                    .map(lit -> lit.getContents())
                    .def("Error: not literal: " + keyPsi.getText())
                )
                .def("Error: no key in expression")
            )
            .collect(Collectors.toList());
    }

    private static Opt<PsiElement> getParentScope(PsiElement psi)
    {
        PsiElement next = psi.getParent();
        while (next != null) {
            Opt<PsiElement> scopeMb = Tls.cast(GroupStatementImpl.class, next).map(v -> v);
            if (scopeMb.has()) {
                return scopeMb;
            }
            next = next.getParent();
        }
        return new Opt(null);
    }

    private static List<PsiElement> getParentScopes(PsiElement psi)
    {
        List<PsiElement> result = new ArrayList<>();
        Opt<PsiElement> next = getParentScope(psi);
        while (next.has()) {
            next.thn(result::add);
            next = getParentScope(next.def(null));
        }
        return result;
    }

    private static boolean isPartOf(PsiElement child, PsiElement grandParent)
    {
        PsiElement parent = child;
        while (parent != null) {
            if (parent.isEquivalentTo(grandParent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static Opt<ElseIfImpl> isInElseIfCondition(PsiElement varReference)
    {
        PsiElement parent = varReference;
        while (parent != null) {
            Opt<ElseIfImpl> elseIf = Tls.cast(ElseIfImpl.class, parent);
            if (elseIf.has()) {
                return elseIf.flt(v -> isPartOf(varReference, v.getCondition()));

            }
            parent = parent.getParent();
        }
        return opt(null);
    }

    /**
     * // and this will be true
     * $someVar = ['someKey' => 'dsa'];
     * if (...) {
     *     // and this will be false
     *     $someVar = 456;
     * }
     * if (...) {
     *     // and this will be false
     *     $someVar = 123;
     * } else {
     *     // this will be true
     *     $someVar = ['someKey' => 'asd'];
     *     // for this statement
     *     print($someVar['someKey']);
     * }
     */
    private static boolean didSurelyHappen(PsiElement reference, Variable usage)
    {
        if (usage.getTextOffset() < reference.getTextOffset()) {
            return false;
        }

        Opt<PsiElement> refScope = getParentScope(reference);
        List<PsiElement> varScopes = getParentScopes(usage);

        Opt<ElseIfImpl> elseIf = isInElseIfCondition(reference);
        if (elseIf.has()) {
            for (PsiElement part: elseIf.def(null).getChildren()) {
                if (part instanceof GroupStatement) {
                    return varScopes.stream().anyMatch(s -> s.isEquivalentTo(part));
                }
            }
            return false;
        }

        return refScope
            .map(declScope -> varScopes
                .stream()
                .anyMatch(scope -> scope.isEquivalentTo(declScope)))
            .def(false);
    }

    /**
     * // and this will be true
     * $someVar = ['someKey' => 'dsa'];
     * if (...) {
     *     // and this will be true
     *     $someVar = 456;
     * }
     * if (...) {
     *     // and this will be false
     *     $someVar = 123;
     * } else {
     *     // this will be true
     *     $someVar = ['someKey' => 'asd'];
     *     // for this statement
     *     print($someVar['someKey']);
     * }
     */
    private static boolean didPossiblyHappen(PsiElement reference, Variable usage)
    {
        if (usage.getTextOffset() < reference.getTextOffset()) {
            return false;
        }

        List<PsiElement> scopesL = getParentScopes(reference);
        List<PsiElement> scopesR = getParentScopes(usage);

        int l = 0;
        int r = 0;

        // 1. find deepest same scope
        while (l < scopesL.size() && r < scopesR.size()) {
            if (scopesL.get(l).getTextOffset() < scopesR.get(r).getTextOffset()) {
                ++r;
            } else if (scopesL.get(l).getTextOffset() > scopesR.get(r).getTextOffset()) {
                ++l;
            } else {
                // could check offset of parent/child to handle
                // cases when different psi have same offset
                // or use textrange
                break;
            }
        }

        // 2. if right inside it are (if|elseif) and (elseif|else) respectively, then false
        if (l < scopesL.size() && r < scopesR.size()) {
            if (l > 0 && r > 0) {
                boolean lIsIf = opt(scopesL.get(l - 1).getParent())
                    .map(v -> v instanceof If || v instanceof ElseIf)
                    .def(false);
                boolean rIsElse = opt(scopesR.get(r - 1).getParent())
                    .map(v -> v instanceof Else || v instanceof ElseIf)
                    .def(false);
                boolean incompatibleScopes = lIsIf && rIsElse;

                return !incompatibleScopes;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static List<String> findArrayKeysInVar(Variable variable, int depth)
    {
        // it is not perfect still, but should handle 99% cases

        List<String> result = new ArrayList<>();
        ResolveResult[] references = variable.multiResolve(false);

        for (int i = references.length - 1; i >= 0; --i) {
            ResolveResult res = references[i];
            Opt<PsiElement> expr = opt(res.getElement())
                .flt(v -> didPossiblyHappen(v, variable))
                .map(v -> v.getParent());

                // $record = ['initialKey' => 123];
            if (expr.fap(toCast(AssignmentExpressionImpl.class))
                    .map(v -> v.getValue())
                    .fap(v -> findKeysInExpr(v, depth))
                    .thn(v -> v.forEach(result::add))
                    .has()
            ) {
                // direct assignment, everything before it is meaningless
                if (didSurelyHappen(res.getElement(), variable)) {
                    break;
                }
            } else {
                // $record['dynamicKey2'] = 345;
                expr.fap(toCast(ArrayAccessExpressionImpl.class))
                    .flt(access -> access.getParent() instanceof AssignmentExpression)
                    .map(access -> access.getIndex())
                    .map(index -> index.getValue())
                    .fap(toCast(StringLiteralExpressionImpl.class))
                    .thn(lit -> result.add(lit.getContents()));
            }
        };
        return result;
    }

    private static Opt<List<String>> findKeysInExpr(PsiElement expr, int depth)
    {
        if (depth <= 0) {
            System.out.println("Keys search depth limit reached");
            return opt(null);
        }
        final int nextDepth = depth - 1;

        return Opt.fst(list(
            opt(null) // for coma formatting
            , Tls.cast(VariableImpl.class, expr)
                .map(v -> findArrayKeysInVar(v, nextDepth))
            , Tls.cast(ArrayCreationExpressionImpl.class, expr)
                .map(arr -> findKeysInArrCtor(arr))
            , Tls.cast(FunctionReferenceImpl.class, expr)
                .map(call -> call.resolve())
                .map(call -> findKeysInMethReturn(call, nextDepth))
            , Tls.cast(MethodReferenceImpl.class, expr)
                .map(call -> call.resolve())
                .map(call -> findKeysInMethReturn(call, nextDepth))
        )).els(() -> System.out.println("Unknown expression type " + expr.getText()));
    }

    @Override
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result)
    {
        // TODO: assigning keys outside declaration/nested arrays/etc...

        // TODO: support lists of associative arrays
        // (like when parser returns segments and you iterate through them)
        
        // TODO: go to definition
        
        // TODO: show error when user tries to access not existing key

        // TODO: use phpdocs:
        //   1. Reference to the function that provides such output
        //   2. Relaxed parse of description like ['k1' => 'v1', ...], array('k1' => 'v1', ...)

        // TODO: autocomplete of arrays resulted from lambdas?
        
        // TODO: autocomplete through yield?
        
        // TODO: autocomplete from XML file with payload example ($xml['body'][0]['main'][0][...])
        
        // TODO: (not sure) autocomplete of function arguments by usage

        opt(parameters.getPosition().getParent())
            .thn(literal -> opt(literal.getParent())
                .fap(toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fap(toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fap(srcExpr -> findKeysInExpr(srcExpr, 30))
                // TODO: provide element type information
                .thn(options -> {
                    if (!(literal instanceof StringLiteralExpression)) {
                        // add quotes if not inside quotes
                        options = options.stream()
                            .map(o -> "'" + o + "'")
                            .collect(Collectors.toList());
                    }

                    options.stream()
                        .distinct()
                        .map(LookupElementBuilder::create)
                        .map(v -> v.withTailText("(assoc.)"))
                        .map(v -> v.bold())
                        .map(o -> PrioritizedLookupElement.withPriority(o, 1000000)) // does not work
                        .forEach(result::addElement);

                    super.fillCompletionVariants(parameters, result);
                })
                .els(() -> System.out.println("Failed to find declared array keys")));
    }
}
