package org.klesun.deep_keys.resolvers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_keys.*;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.deep_keys.resolvers.var_res.ArgRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VarRes extends Lang
{
    final private IFuncCtx ctx;

    public VarRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    /**
     * extends type with the key assignment information
     * @TODO: make it immutable! Don't rely on side effects... if you remove Tls.onDemand() it will not work
     */
    private static void addAssignment(L<S<MultiType>> dest, Assign assign, boolean overwritingAssignment)
    {
        if (assign.keys.size() == 0) {
            if (assign.didSurelyHappen && overwritingAssignment) dest.clear();
            dest.add(assign.assignedType);
        } else {
            if (dest.fap(g -> g.get().types).size() == 0) {
                dest.add(Tls.onDemand(() -> new MultiType(list(new DeepType(assign.psi, PhpType.ARRAY)))));
            }
            String nextKey = assign.keys.remove(0);
            dest.fap(g -> g.get().types).fch(type -> {
                if (nextKey == null) {
                    // index key
                    L<S<MultiType>> getters = list(Tls.onDemand(() -> new MultiType(L(type.indexTypes))));
                    addAssignment(getters, assign, false);
                    type.indexTypes = getters.fap(g -> g.get().types);
                } else {
                    // associative key
                    if (!type.keys.containsKey(nextKey)) {
                        type.addKey(nextKey, assign.psi);
                    }
                    addAssignment(type.keys.get(nextKey).getTypeGetters(), assign, true);
                }
            });
            assign.keys.add(0, nextKey);
        }
    }

    private static List<DeepType> assignmentsToTypes(List<Assign> assignments)
    {
        L<S<MultiType>> resultTypes = list();

        // assignments are supposedly in chronological order
        assignments.forEach(ass -> addAssignment(resultTypes, ass, true));

        return resultTypes.fap(g -> g.get().types);
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

    private static L<DeepType> makeRegexNameCaptureTypes(List<DeepType> regexTypes)
    {
        return L(regexTypes)
            .fop(strt -> opt(strt.stringValue)
                .map(s -> parseRegexNameCaptures(s))
                .map(names -> {
                    DeepType t = new DeepType(strt.definition, PhpType.ARRAY);
                    names.forEach(n -> t.addKey(n, strt.definition)
                        .addType(() -> new MultiType(list(new DeepType(strt.definition, PhpType.STRING)))));
                    return t;
                }));
    }

    // null in key chain means index (when it is number or variable, not named key)
    private Opt<T2<List<String>, S<MultiType>>> collectKeyAssignment(AssignmentExpressionImpl ass)
    {
        Opt<ArrayAccessExpressionImpl> nextKeyOpt = opt(ass.getVariable())
            .fap(toCast(ArrayAccessExpressionImpl.class));

        List<String> reversedKeys = list();

        while (nextKeyOpt.has()) {
            ArrayAccessExpressionImpl nextKey = nextKeyOpt.def(null);

            String name = opt(nextKey.getIndex())
                .map(index -> index.getValue())
                .fap(toCast(PhpExpression.class))
                .map(key -> ctx.findExprType(key))
                .map(t -> t.getStringValue())
                .def(null);
            reversedKeys.add(name);

            nextKeyOpt = opt(nextKey.getValue())
                .fap(toCast(ArrayAccessExpressionImpl.class));
        }

        List<String> keys = list();
        for (int i = reversedKeys.size() - 1; i >= 0; --i) {
            keys.add(reversedKeys.get(i));
        }

        return opt(ass.getValue())
            .fap(toCast(PhpExpression.class))
            .map(value -> T2(keys, S(() -> {
                MultiType mt = ctx.findExprType(value);
                return mt;
            })));
    }

    private Opt<S<MultiType>> assertForeachElement(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .fap(toCast(ForeachImpl.class))
            .flt(fch -> opt(fch.getValue()).map(v -> v.isEquivalentTo(varRef)).def(false))
            .map(fch -> fch.getArray())
            .fap(toCast(PhpExpression.class))
            .map(arr -> () -> ctx.findExprType(arr).getEl());
    }

    private Opt<S<MultiType>> assertTupleAssignment(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .fap(toCast(MultiassignmentExpressionImpl.class))
            .fap(multi -> opt(multi.getValue())
                /** array creation is wrapped inside an "expression impl" */
                .map(v -> v.getFirstPsiChild())
                .fap(toCast(PhpExpression.class))
                .map(val -> ctx.findExprType(val).types)
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
                            .map(k -> k.getTypeGetters()))
                        .fap(v -> v)
                    )
                )
                .map(mtgs -> () -> new MultiType(mtgs.fap(g -> g.get().types)))
            );
    }

    private Opt<S<MultiType>> assertPregMatchResult(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .fap(toCast(ParameterListImpl.class))
            .flt(lst -> L(lst.getParameters()).gat(2)
                .map(psi -> psi.isEquivalentTo(varRef))
                .def(false))
            .map(lst -> lst.getParent())
            .fap(toCast(FunctionReferenceImpl.class))
            .flt(fun -> opt(fun.getName()).def("").equals("preg_match"))
            .fap(fun -> L(fun.getParameters()).fst())
            .fap(toCast(PhpExpression.class))
            .map(regexPsi -> () -> {
                MultiType mt = ctx.findExprType(regexPsi);
                L<DeepType> matchesType = makeRegexNameCaptureTypes(mt.types);
                return new MultiType(matchesType);
            });
    }

    public List<DeepType> resolve(Variable variable)
    {
        List<Assign> revAsses = list();
        ResolveResult[] references = variable.multiResolve(false);

        Mutable<Boolean> finished = new Mutable<>(false);
        for (int i = references.length - 1; i >= 0; --i) {
            if (finished.get()) break;
            // TODO: make sure closure `use`-s don't incorrectly use wrong context argument generics
            ResolveResult res = references[i];
            opt(res.getElement())
                .thn(refPsi -> opt(refPsi)
                    .flt(v -> ScopeFinder.didPossiblyHappen(v, variable))
                    .fap(varRef -> {
                        boolean didSurelyHappen = ScopeFinder.didSurelyHappen(res.getElement(), variable);
                        return Tls.findParent(varRef, AssignmentExpressionImpl.class, par -> par instanceof ArrayAccessExpression)
                            .fap(ass -> collectKeyAssignment(ass)
                                .map(tup -> new Assign(tup.a, tup.b, didSurelyHappen, ass)))
                            .elf(() -> assertForeachElement(varRef)
                                .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, varRef)))
                            .elf(() -> assertTupleAssignment(varRef)
                                .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, varRef)))
                            .elf(() -> assertPregMatchResult(varRef)
                                .map(varTypes -> new Assign(list(), varTypes, didSurelyHappen, varRef)))
                            .thn(assign -> {
                                revAsses.add(assign);
                                if (didSurelyHappen && assign.keys.size() == 0) {
                                    // direct assignment, everything before it is meaningless
                                    finished.set(true);
                                }
                            });
                    })
                    .els(() -> Tls.cast(ParameterImpl.class, refPsi)
                        .map(param -> {
                            S<MultiType> mtg = () -> new ArgRes(ctx).resolveArg(param);
                            return new Assign(list(), mtg, true, refPsi);
                        })
                        .thn(revAsses::add)));
        }

        List<Assign> assignments = list();
        for (int i = revAsses.size() - 1; i >= 0; --i) {
            assignments.add(revAsses.get(i));
        }

        return assignmentsToTypes(assignments);
    }

}
