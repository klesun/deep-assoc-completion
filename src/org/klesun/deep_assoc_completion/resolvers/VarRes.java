package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocVarImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.*;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.resolvers.var_res.ArgRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.List;
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
                    DeepType.Key keyRec = type.keys.get(nextKey);
                    PhpType briefType = assign.keys.size() > 0
                        ? PhpType.ARRAY : assign.briefType;
                    keyRec.addType(() -> new MultiType(L()), briefType);
                    addAssignment(keyRec.getTypeGetters(), assign, true);
                }
            });
            assign.keys.add(0, nextKey);
        }
    }

    /**
     * this function should be rewritten so that it did
     * not demand the type of actual variable, cuz it
     * causes recursion, side effects... such nasty stuff
     */
    private static L<DeepType> assignmentsToTypes(List<Assign> assignments)
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
                        .addType(() -> new MultiType(list(new DeepType(strt.definition, PhpType.STRING))), PhpType.STRING));
                    return t;
                }));
    }

    // array_unshift($itin, ['from' => 'KIV', 'to' => 'RIX']);
    private Opt<Assign> assertArrayUnshift(PsiElement varRef)
    {
        L<String> keys = list();
        keys.add(null);
        return opt(varRef.getParent())
            .fop(toCast(ParameterListImpl.class))
            .map(par -> par.getParent())
            .fop(toCast(FunctionReferenceImpl.class))
            .flt(call -> "array_unshift".equals(call.getName()))
            .fop(call -> {
                L<PsiElement> args = L(call.getParameters());
                boolean isCaretArr = args.gat(0).map(arg -> arg.isEquivalentTo(varRef)).def(false);
                return args.gat(1)
                    .flt(asd -> isCaretArr)
                    .fop(Tls.toCast(PhpExpression.class))
                    .map(el -> new Assign(keys, () -> ctx.findExprType(el), false, el, el.getType()));
            });
    }

    private Opt<S<MultiType>> assertForeachElement(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .fop(toCast(ForeachImpl.class))
            .flt(fch -> opt(fch.getValue()).map(v -> v.isEquivalentTo(varRef)).def(false))
            .map(fch -> fch.getArray())
            .fop(toCast(PhpExpression.class))
            .map(arr -> () -> ctx.findExprType(arr).getEl());
    }

    private Opt<S<MultiType>> assertTupleAssignment(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .fop(toCast(MultiassignmentExpressionImpl.class))
            .fop(multi -> opt(multi.getValue())
                /** array creation is wrapped inside an "expression impl" */
                .map(v -> v.getFirstPsiChild())
                .fop(toCast(PhpExpression.class))
                .map(val -> ctx.findExprType(val).types)
                .flt(types -> types.size() > 0)
                .fop(arrts -> opt(multi.getVariables())
                    .fop(vars -> {
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
            .fop(toCast(ParameterListImpl.class))
            .flt(lst -> L(lst.getParameters()).gat(2)
                .map(psi -> psi.isEquivalentTo(varRef))
                .def(false))
            .map(lst -> lst.getParent())
            .fop(toCast(FunctionReferenceImpl.class))
            .flt(fun -> opt(fun.getName()).def("").equals("preg_match"))
            .fop(fun -> L(fun.getParameters()).fst())
            .fop(toCast(PhpExpression.class))
            .map(regexPsi -> () -> {
                MultiType mt = ctx.findExprType(regexPsi);
                L<DeepType> matchesType = makeRegexNameCaptureTypes(mt.types);
                return new MultiType(matchesType);
            });
    }

    private static L<PsiElement> findDeclarations(Variable variable)
    {
        // if this line is still here when you read this, that means I
        // decided to just do DumbService::isDumb() check in Type Provider
        return L(variable.multiResolve(false))
            .fop(res -> opt(res.getElement()));
    }

    public List<DeepType> resolve(Variable variable)
    {
        List<Assign> revAsses = list();
        L<PsiElement> references = findDeclarations(variable)
            .flt(refPsi -> ScopeFinder.didPossiblyHappen(refPsi, variable));

        // @var docs are a special case since they give type
        // info from any position (above/below/right of/left of the var declaration)
        L<DeepType> docTypes = references
            .tkw(toCast(PhpDocVarImpl.class))
            .mop(varDoc -> varDoc.getParent())
            .fop(toCast(PhpDocTag.class))
            .fop(docTag -> new DocParamRes(ctx).resolve(docTag))
            .fap(mt -> mt.types);

        for (int i = references.size() - 1; i >= 0; --i) {
            // TODO: make sure closure `use`-s don't incorrectly use wrong context argument generics
            PsiElement refPsi = references.get(i);
            boolean didSurelyHappen = ScopeFinder.didSurelyHappen(refPsi, variable);
            Opt<Assign> assignOpt = Opt.fst(list(opt(null)
                , (new AssRes(ctx)).collectAssignment(refPsi, didSurelyHappen)
                , assertArrayUnshift(refPsi)
                , assertForeachElement(refPsi)
                    .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, refPsi, PhpType.MIXED))
                , assertTupleAssignment(refPsi)
                    .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, refPsi, PhpType.MIXED))
                , assertPregMatchResult(refPsi)
                    .map(varTypes -> new Assign(list(), varTypes, didSurelyHappen, refPsi, PhpType.ARRAY))
                , Tls.cast(ParameterImpl.class, refPsi)
                    .map(param -> {
                        S<MultiType> mtg = () -> new ArgRes(ctx).resolveArg(param);
                        return new Assign(list(), mtg, true, refPsi, param.getType());
                    })
            ));
            if (assignOpt.has()) {
                Assign ass = assignOpt.unw();
                revAsses.add(ass);
                if (didSurelyHappen && ass.keys.size() == 0) {
                    // direct assignment, everything before it is meaningless
                    break;
                }
            }
        }

        List<Assign> assignments = list();
        for (int i = revAsses.size() - 1; i >= 0; --i) {
            assignments.add(revAsses.get(i));
        }
        DeepType typeFromIdea = new DeepType(variable);
        return assignmentsToTypes(assignments)
            .cct(docTypes).cct(list(typeFromIdea));
    }
}
