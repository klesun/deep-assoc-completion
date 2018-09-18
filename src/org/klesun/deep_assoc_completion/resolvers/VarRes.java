package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocVarImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.*;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.KeyType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.var_res.ArgRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VarRes extends Lang
{
    final private FuncCtx ctx;

    public VarRes(FuncCtx ctx)
    {
        this.ctx = ctx;
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

    private static It<DeepType> makeRegexNameCaptureTypes(It<DeepType> regexTypes)
    {
        return regexTypes
            .fop(strt -> opt(strt.stringValue)
                .map(s -> parseRegexNameCaptures(s))
                .map(names -> {
                    DeepType t = new DeepType(strt.definition, PhpType.ARRAY);
                    names.forEach(n -> t.addKey(n, strt.definition)
                        .addType(() -> new Mt(list(new DeepType(strt.definition, PhpType.STRING))), PhpType.STRING));
                    return t;
                }));
    }

    // array_unshift($itin, ['from' => 'KIV', 'to' => 'RIX']);
    private Opt<Assign> assertArrayUnshift(PsiElement varRef)
    {
        L<KeyType> keys = list();
        keys.add(KeyType.integer());
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
                    .map(el -> new Assign(keys, () -> ctx.findExprType(el).wap(Mt::new), false, el, Tls.getIdeaType(el)));
            });
    }

    private Opt<S<Mt>> assertForeachElement(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .fop(toCast(ForeachImpl.class))
            .fop(fch -> opt(fch.getArray())
                .fop(toCast(PhpExpression.class))
                .map(arr -> () -> {
                    Mt arrt = ctx.findExprType(arr).wap(Mt::new);
                    L<Variable> tuple = L(fch.getVariables());
                    Opt<Variable> keyVarOpt = opt(fch.getKey())
                        // IDEA breaks on list() - should help her
                        .flt(keyVar -> Opt.fst(
                            () -> opt(keyVar.getNextSibling())
                                .fop(toCast(PsiWhiteSpace.class))
                                .map(ws -> ws.getNextSibling()),
                            () -> opt(keyVar.getNextSibling())
                        ).flt(par -> par.getText().equals("=>")).has());
                    if (keyVarOpt.has()) {
                        tuple = tuple.sub(1); // key was included
                        if (varRef.isEquivalentTo(keyVarOpt.unw())) {
                            return arrt.types.fap(t -> L(t.keys.values()))
                                .map(keyObj -> new DeepType(keyObj.definition, PhpType.STRING, keyObj.name))
                                .wap(Mt::new);
                        }
                    }
                    if (tuple.size() > 1) {
                        return arrt.getEl()
                            .getKey(tuple.indexOf(varRef) + "");
                    } else {
                        // this is actually not correct since you could write list($a)
                        // or $list(,,$c), but IDEA does not parse list in foreach well
                        return arrt.getEl();
                    }
                }));
    }

    private Opt<S<Mt>> assertTupleAssignment(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .fop(toCast(MultiassignmentExpressionImpl.class))
            .fop(multi -> opt(multi.getValue())
                /** array creation is wrapped inside an "expression impl" */
                .map(v -> v.getFirstPsiChild())
                .fop(toCast(PhpExpression.class))
                .map(val -> ctx.findExprType(val))
                .flt(types -> types.has())
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
                .map(mtgs -> () -> new Mt(mtgs.fap(g -> g.get().types)))
            );
    }

    private Opt<S<Mt>> assertPregMatchResult(PsiElement varRef)
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
                It<DeepType> mt = ctx.findExprType(regexPsi);
                It<DeepType> matchesType = makeRegexNameCaptureTypes(mt);
                return new Mt(matchesType);
            });
    }

    private static L<PsiElement> findDeclarations(Variable variable)
    {
        // if this line is still here when you read this, that means I
        // decided to just do DumbService::isDumb() check in Type Provider
        return L(variable.multiResolve(false))
            .fop(res -> opt(res.getElement()));
    }

    public It<DeepType> getDocType(Variable variable)
    {
        return findDeclarations(variable).itr()
            .flt(refPsi -> ScopeFinder.didPossiblyHappen(refPsi, variable))
            .fop(toCast(PhpDocVarImpl.class))
            .fop(varDoc -> opt(varDoc.getParent()))
            .fop(toCast(PhpDocTag.class))
            .fap(docTag -> new DocParamRes(ctx).resolve(docTag));
    }

    public It<DeepType> resolve(Variable variable)
    {
        L<PsiElement> references = findDeclarations(variable)
            .flt(refPsi -> ScopeFinder.didPossiblyHappen(refPsi, variable));

        // @var docs are a special case since they give type
        // info from any position (above/below/right of/left of the var declaration)
        It<DeepType> docTypes = getDocType(variable);

        L<Assign> asses = references
            .fop(refPsi -> {
                boolean didSurelyHappen = ScopeFinder.didSurelyHappen(refPsi, variable);
                return Opt.fst(() -> opt(null)
                    , () -> (new AssRes(ctx)).collectAssignment(refPsi, didSurelyHappen)
                    , () -> assertArrayUnshift(refPsi)
                    , () -> assertForeachElement(refPsi)
                        .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, refPsi, PhpType.MIXED))
                    , () -> assertTupleAssignment(refPsi)
                        .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, refPsi, PhpType.MIXED))
                    , () -> assertPregMatchResult(refPsi)
                        .map(varTypes -> new Assign(list(), varTypes, didSurelyHappen, refPsi, PhpType.ARRAY))
                    , () -> Tls.cast(ParameterImpl.class, refPsi)
                        .map(param -> {
                            S<Mt> mtg = () -> new ArgRes(ctx).resolveArg(param);
                            return new Assign(list(), mtg, true, refPsi, param.getType());
                        }));
            });
        int lastDeclPos = -1;
        for (int i = 0; i < asses.size(); ++i) {
            if (asses.get(i).didSurelyHappen &&
                asses.get(i).keys.size() == 0
            ) {
                lastDeclPos = i;
            }
        }
        if (lastDeclPos > -1) {
            asses = asses.sub(lastDeclPos);
        }

        DeepType typeFromIdea = new DeepType(variable);
        Opt<Mt> thisType = opt(variable)
            .flt(vari -> vari.getText().equals("$this"))
            .fop(vari -> ctx.instGetter.map(g -> g.get()));
        return It.cnc(
            docTypes, list(typeFromIdea),
            thisType.itr().fap(a -> a.types),
            AssRes.assignmentsToTypes(asses)
        );
    }
}
