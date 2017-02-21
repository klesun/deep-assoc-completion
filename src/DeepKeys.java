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
            Opt<PsiElement> scopeMb = Opt.fst(list(
                opt(null) // for coma formatting
                , Tls.cast(GroupStatementImpl.class, next).map(v -> v)
                // elseif ($result = anotherFunction())
                , Tls.cast(ElseIfImpl.class, next).map(v -> v)
            ));
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

    /**
     * // and this will be true
     * $someVar = ['someKey' => 'dsa'];
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
    private static boolean isReachable(Variable usage, PsiElement declaration)
    {
        if (usage.getTextOffset() < declaration.getTextOffset()) {
            return false;
        }
        return getParentScope(declaration)
            .map(declScope -> getParentScopes(usage)
                .stream()
                .anyMatch(scope -> scope.isEquivalentTo(declScope)))
            .def(false);
    }

    private static List<String> findArrayKeysInVar(Variable variable, int depth)
    {
        // it is not perfect still, but should handle 99% cases

        List<String> result = new ArrayList<>();
        ResolveResult[] references = variable.multiResolve(false);

        for (int i = references.length - 1; i >= 0; --i) {
            ResolveResult res = references[i];
            if (opt(res.getElement())
                // to filter out further assignments
                .flt(v -> v.getTextOffset() < variable.getTextOffset())
                .map(v -> v.getParent())
                .fap(toCast(AssignmentExpressionImpl.class))
                .map(v -> v.getValue())
                .fap(v -> findKeysInExpr(v, depth))
                .thn(v -> v.forEach(result::add))
                .has()
            ) {
                // direct assignment, everything before it is meaningless
                if (isReachable(variable, res.getElement())) {
                    break;
                }
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
