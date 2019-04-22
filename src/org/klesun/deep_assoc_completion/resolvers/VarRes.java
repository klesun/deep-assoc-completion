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
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.ScopeFinder;
import org.klesun.deep_assoc_completion.resolvers.var_res.ArgRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.Assign;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.klesun.deep_assoc_completion.structures.Mkt.*;
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
                        .fap(k -> k.keyType.getTypes())
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

    public It<DeepType> resolveSuperGlobal(Variable caretVar)
    {
        if (caretVar.getName().equals("_SERVER")) {
            return assoc(caretVar, list(
                T2("LC_PAPER", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("LC_ADDRESS", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("XDG_SESSION_ID", str(caretVar, "2766").mt()),
                T2("LC_MONETARY", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("TERM", str(caretVar, "xterm").mt()),
                T2("SHELL", str(caretVar, "/bin/bash").mt()),
                T2("SSH_CLIENT", str(caretVar, "188.64.183.230 50033 22").mt()),
                T2("LC_NUMERIC", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("SSH_TTY", str(caretVar, "/dev/pts/9").mt()),
                T2("USER", str(caretVar, "klesun").mt()),
                T2("LS_COLORS", str(caretVar, "rs=0:di=01;34:ln=01;36:mh=00:pi=40;33:so=01;35:do=01;35:bd=40;33;01:cd=40;33;01:or=40;31;01:mi=00:su=37;41:sg=30;43:ca=30;41:tw=30;42:ow=34;42:st=37;44:ex=01;32:*.tar=01;31:*.tgz=01;31:*.arc=01;31:*.arj=01;31:*.taz=01;31:*.lha=01;31:*.lz4=01;31:*.lzh=01;31:*.lzma=01;31:*.tlz=01;31:*.txz=01;31:*.tzo=01;31:*.t7z=01;31:*.zip=01;31:*.z=01;31:*.Z=01;31:*.dz=01;31:*.gz=01;31:*.lrz=01;31:*.lz=01;31:*.lzo=01;31:*.xz=01;31:*.bz2=01;31:*.bz=01;31:*.tbz=01;31:*.tbz2=01;31:*.tz=01;31:*.deb=01;31:*.rpm=01;31:*.jar=01;31:*.war=01;31:*.ear=01;31:*.sar=01;31:*.rar=01;31:*.alz=01;31:*.ace=01;31:*.zoo=01;31:*.cpio=01;31:*.7z=01;31:*.rz=01;31:*.cab=01;31:*.jpg=01;35:*.jpeg=01;35:*.gif=01;35:*.bmp=01;35:*.pbm=01;35:*.pgm=01;35:*.ppm=01;35:*.tga=01;35:*.xbm=01;35:*.xpm=01;35:*.tif=01;35:*.tiff=01;35:*.png=01;35:*.svg=01;35:*.svgz=01;35:*.mng=01;35:*.pcx=01;35:*.mov=01;35:*.mpg=01;35:*.mpeg=01;35:*.m2v=01;35:*.mkv=01;35:*.webm=01;35:*.ogm=01;35:*.mp4=01;35:*.m4v=01;35:*.mp4v=01;35:*.vob=01;35:*.qt=01;35:*.nuv=01;35:*.wmv=01;35:*.asf=01;35:*.rm=01;35:*.rmvb=01;35:*.flc=01;35:*.avi=01;35:*.fli=01;35:*.flv=01;35:*.gl=01;35:*.dl=01;35:*.xcf=01;35:*.xwd=01;35:*.yuv=01;35:*.cgm=01;35:*.emf=01;35:*.ogv=01;35:*.ogx=01;35:*.aac=00;36:*.au=00;36:*.flac=00;36:*.m4a=00;36:*.mid=00;36:*.midi=00;36:*.mka=00;36:*.mp3=00;36:*.mpc=00;36:*.ogg=00;36:*.ra=00;36:*.wav=00;36:*.oga=00;36:*.opus=00;36:*.spx=00;36:*.xspf=00;36:").mt()),
                T2("LC_TELEPHONE", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("MAIL", str(caretVar, "/var/mail/klesun").mt()),
                T2("PATH", str(caretVar, "/home/klesun/bin:/home/klesun/.local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin").mt()),
                T2("LC_IDENTIFICATION", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("PWD", str(caretVar, "/home/klesun").mt()),
                T2("LANG", str(caretVar, "en_US.UTF-8").mt()),
                T2("LC_MEASUREMENT", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("SHLVL", str(caretVar, "1").mt()),
                T2("HOME", str(caretVar, "/home/klesun").mt()),
                T2("LOGNAME", str(caretVar, "klesun").mt()),
                T2("XDG_DATA_DIRS", str(caretVar, "/usr/local/share:/usr/share:/var/lib/snapd/desktop").mt()),
                T2("SSH_CONNECTION", str(caretVar, "188.64.183.230 50033 192.168.0.105 22").mt()),
                T2("LESSOPEN", str(caretVar, "| /usr/bin/lesspipe %s").mt()),
                T2("XDG_RUNTIME_DIR", str(caretVar, "/run/user/1000").mt()),
                T2("LESSCLOSE", str(caretVar, "/usr/bin/lesspipe %s %s").mt()),
                T2("LC_TIME", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("LC_NAME", str(caretVar, "lv_LV.UTF-8").mt()),
                T2("_", str(caretVar, "/usr/bin/php").mt()),
                T2("PHP_SELF", str(caretVar, "Standard input code").mt()),
                T2("SCRIPT_NAME", str(caretVar, "Standard input code").mt()),
                T2("SCRIPT_FILENAME", str(caretVar, "").mt()),
                T2("PATH_TRANSLATED", str(caretVar, "").mt()),
                T2("DOCUMENT_ROOT", str(caretVar, "").mt()),
                T2("REQUEST_TIME_FLOAT", floate(caretVar, 1555923431.315248).mt()),
                T2("REQUEST_TIME", floate(caretVar, 1555923431).mt()),
                T2("argv", arr(caretVar, new DeepType(caretVar, PhpType.STRING).mt()).mt()),
                T2("argc", inte(caretVar, 1).mt())
            )).mt().types.itr();
        } else {
            return It.non();
        }
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
                Boolean isClosureVar = declFile.equals(caretFile)
                    && !declScope.equals(caretScope)
                    && ctx.getClosureVars().has();
                if (isClosureVar) {
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

        It<DeepType> superGlobalTit = resolveSuperGlobal(caretVar);

        return It.cnc(
            docTypes, superGlobalTit,
            hasClassInfo(typeFromIdea.briefType)
                ? list(typeFromIdea) : list(),
            thisType, closureType,
            AssRes.assignmentsToTypes(asses)
        ).def(list(typeFromIdea));
    }
}
