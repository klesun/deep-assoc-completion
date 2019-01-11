package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocVarImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.resolvers.var_res.ArgRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.Assign;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.*;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.klesun.lang.Lang.*;

public class VarRes
{
    final private IExprCtx ctx;

    public VarRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private static It<String> parseRegexNameCaptures(String regexText)
    {
        Pattern pattern = Pattern.compile("\\(\\?P<([a-zA-Z_0-9]+)>");
        Matcher matcher = pattern.matcher(regexText);
        boolean hasFirst = matcher.find();
        return It(() -> new Iterator<String>() {
            boolean hasNext = hasFirst;
            public boolean hasNext() {
                return hasNext;
            }
            public String next() {
                String value = matcher.group(1);
                hasNext = matcher.find();
                return value;
            }
        });
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
        keys.add(KeyType.integer(varRef));
        return opt(varRef.getParent())
            .fop(toCast(ParameterListImpl.class))
            .map(par -> par.getParent())
            .fop(toCast(FunctionReferenceImpl.class))
            .flt(call -> "array_unshift".equals(call.getName()) || "array_push".equals(call.getName()))
            .fop(call -> {
                L<PsiElement> args = L(call.getParameters());
                boolean isCaretArr = args.gat(0).map(arg -> arg.isEquivalentTo(varRef)).def(false);
                return args.gat(1)
                    .flt(asd -> isCaretArr)
                    .fop(Tls.toCast(PhpExpression.class))
                    .map(el -> new Assign(keys, () -> ctx.findExprType(el), false, el, Tls.getIdeaType(el)));
            });
    }

    private static boolean isInList(Variable lstVar)
    {
        PsiElement prev = lstVar.getPrevSibling();
        while (prev != null) {
            if (prev.getText().equals("list")) {
                return true;
            } else if (
                !list(",", "(").contains(prev.getText()) &&
                !(prev instanceof PsiWhiteSpaceImpl)
            ) {
                return false;
            } else {
                prev = prev.getPrevSibling();
            }
        }
        return false;
    }

    // array, key, value, list values
    public static Opt<T4<PhpExpression, Opt<Variable>, Opt<Variable>, L<Variable>>> parseForeach(ForeachImpl fch)
    {
        return opt(fch.getArray())
            .fop(toCast(PhpExpression.class))
            .map(arr -> {
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
                }
                // you could write list($a) or $list(,,$c), but
                // IDEA does not parse list in foreach well
                if (tuple.size() > 1 || tuple.fst().any(lstVar -> isInList(lstVar))) {
                    return T4(arr, keyVarOpt, non(), tuple);
                } else {
                    return T4(arr, keyVarOpt, tuple.fst(), list());
                }
            });
    }

    private Opt<S<It<DeepType>>> assertForeachElement(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .cst(ForeachImpl.class)
            .fop(fch -> parseForeach(fch))
            .map(tup -> () -> tup.nme((arr, keyVarOpt, valOpt, tuple) -> {
                It<DeepType> artit = ctx.findExprType(arr);
                if (opt(varRef).equals(keyVarOpt)) {
                    return artit.fap(t -> t.keys)
                        .fap(k -> k.keyType.getTypes.get())
                        .fap(t -> opt(t.stringValue)
                            .map(name -> new DeepType(t.definition, PhpType.STRING, name)));
                } else if (!valOpt.has()) {
                    return artit.fap(Mt::getElSt)
                        .fap(elt -> Mt.getKeySt(elt, tuple.indexOf(varRef) + ""));
                } else {
                    return artit.fap(Mt::getElSt);
                }
            }));
    }

    private Opt<S<It<DeepType>>> assertTupleAssignment(PsiElement varRef)
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
                    .map(i -> arrts
                        .fap(t -> t.keys)
                        .flt(k -> k.keyType.getNames().any(n -> n.equals(i + "")))
                        .fap(k -> k.getTypeGetters()))
                )
                .map(mtgs -> () -> mtgs.fap(g -> g.get().types))
            );
    }

    private Opt<S<It<DeepType>>> assertPregMatchResult(PsiElement varRef)
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
                It<DeepType> tit = ctx.findExprType(regexPsi);
                return makeRegexNameCaptureTypes(tit);
            });
    }

    private static It<PsiElement> findDeclarations(Variable variable)
    {
        // if this line is still here when you read this, that means I
        // decided to just do DumbService::isDumb() check in Type Provider
        return It(variable.multiResolve(false))
            .fop(res -> opt(res.getElement()));
    }

    public It<DeepType> getDocType(Variable variable)
    {
        return findDeclarations(variable)
            .flt(refPsi -> ScopeFinder.didPossiblyHappen(refPsi, variable))
            .fop(toCast(PhpDocVarImpl.class))
            .fop(varDoc -> opt(varDoc.getParent()))
            .fop(toCast(PhpDocTag.class))
            .fap(docTag -> new DocParamRes(ctx).resolve(docTag));
    }

    public Opt<Assign> resolveRef(PsiElement refPsi, boolean didSurelyHappen)
    {
        return Opt.fst(() -> non()
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
                    S<It<DeepType>> mtg = () -> new ArgRes(ctx).resolveArg(param);
                    return new Assign(list(), mtg, true, refPsi, param.getType());
                })
            , () -> Tls.cast(Variable.class, refPsi)
                .fop(vari -> opt(vari.getParent()))
                .cst(UnaryExpression.class) // ++$i
                .map(una -> new Assign(list(), () -> It(som(DeepType.makeInt(una, null))), true, una, una.getType()))
        );
    }

    // add idea type only if it has class or we have no other info, since it does not
    // include string values - we may mistakenly conclude that we could not resolve type
    private static boolean hasClassInfo(PhpType ideaType)
    {
        PhpType filtered = ideaType.filterUnknown().filterMixed().filterNull().filterPrimitives();
        // array and object types should be left
        return !filtered.isEmpty();
    }

    public It<DeepType> resolve(Variable caretVar)
    {
        It<PsiElement> references = findDeclarations(caretVar)
            .flt(refPsi -> ScopeFinder.didPossiblyHappen(refPsi, caretVar))
            ;

        // @var docs are a special case since they give type
        // info from any position (above/below/right of/left of the var declaration)
        It<DeepType> docTypes = getDocType(caretVar);
        Opt<Function> caretScope = Tls.findParent(caretVar, Function.class, a -> true)
            .fop(func -> caretVar.getParent() instanceof PhpUseList
                ? Tls.findParent(func, Function.class, a -> true) : som(func));
        Opt<PsiFile> caretFile = opt(caretVar.getContainingFile());

        L<Assign> asses = references
            .fap(refPsi -> {
                Opt<Function> declScope = Tls.findParent(refPsi, Function.class, a -> true);
                Opt<PsiFile> declFile = opt(refPsi.getContainingFile());
                if (declFile.equals(caretFile) && !declScope.equals(caretScope) && ctx.getClosureVars().has()) {
                    return non(); // refPsi is outside the function, a closure, handled manually
                }
                return resolveRef(refPsi, ScopeFinder.didSurelyHappen(refPsi, caretVar));
            })
            .arr();

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

        DeepType typeFromIdea = new DeepType(caretVar);
        It<DeepType> thisType = opt(caretVar)
            .flt(vari -> vari.getText().equals("$this"))
            .fap(vari -> ctx.getThisType());

        It<DeepType> closureType = ctx.getClosureVars().itr()
            .flt(t2 -> t2.a.equals(caretVar.getName()))
            .fap(t2 -> t2.b.get());

        return It.cnc(
            docTypes,
            hasClassInfo(typeFromIdea.briefType)
                ? list(typeFromIdea) : list(),
            thisType, closureType,
            AssRes.assignmentsToTypes(asses)
        ).def(list(typeFromIdea));
    }
}
