package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocVarImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.completion_providers.VarNamePvdr;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.ScopeFinder;
import org.klesun.deep_assoc_completion.resolvers.var_res.ArgRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.*;
import org.klesun.lang.*;
import org.klesun.lang.iterators.RegexIterator;

import java.util.List;

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
        String pattern = "\\(\\?P<([a-zA-Z_0-9]+)>";
        return It(() -> new RegexIterator(pattern, regexText))
            .map(groups -> groups.get(1));
    }

    private static IIt<DeepType> makeRegexNameCaptureTypes(IIt<DeepType> regexTypes)
    {
        return regexTypes
            .fop(strt -> opt(strt.stringValue)
                .map(s -> parseRegexNameCaptures(s))
                .map(names -> new Build(strt.definition, PhpType.ARRAY)
                    .keys(names.map(n -> {
                        DeepType valt = new DeepType(strt.definition, PhpType.STRING);
                        return new KeyEntry(n, strt.definition)
                            .addType(() -> new Mt(list(valt)), PhpType.STRING);
                    }))
                    .get()));
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

    private Opt<S<IIt<DeepType>>> assertForeachElement(PsiElement varRef)
    {
        return opt(varRef.getParent())
            .cst(ForeachImpl.class)
            .fop(fch -> ForEach.parse(fch))
            .map(fchObj -> () -> {
                IIt<DeepType> artit = ctx.findExprType(fchObj.srcArr);
                if (opt(varRef).equals(fchObj.keyVar)) {
                    return artit.fap(t -> t.keys)
                        .fap(k -> k.keyType.getTypes())
                        .fap(t -> opt(t.stringValue)
                            .map(name -> new DeepType(t.definition, PhpType.STRING, name)));
                } else if (!fchObj.valVar.has()) {
                    String key = fchObj.listVars
                        .flt(t -> t.b.equals(varRef))
                        .map(t -> t.a).fst().def(null);
                    return artit.fap(Mt::getElSt)
                        .fap(elt -> Mt.getKeySt(elt, key));
                } else {
                    return artit.fap(Mt::getElSt);
                }
            });
    }

    private Opt<S<IIt<DeepType>>> assertDeclFromGlobal(PsiElement varRef)
    {
        return Tls.cast(Variable.class, varRef)
            .flt(varPsi -> VarNamePvdr.isGlobalContext(varPsi))
            .flt(varPsi -> !"".equals(varPsi.getName()))
            .map(varPsi -> () -> VarNamePvdr.resolveGlobalsMagicVar(ctx, varPsi)
                .fap(globt -> Mt.getKeySt(globt, varPsi.getName())));
    }

    private static Opt<String> assertListKey(PsiElement varRef)
    {
        return opt(varRef.getPrevSibling())
            .fop(s -> s instanceof PsiWhiteSpace ? opt(s.getPrevSibling()) : som(s))
            .fop(s -> s.getText().equals("=>") ? opt(s.getPrevSibling()) : non())
            .fop(s -> s instanceof PsiWhiteSpace ? opt(s.getPrevSibling()) : som(s))
            .cst(StringLiteralExpression.class)
            .map(s -> s.getContents())
        ;
    }

    private static Opt<T2<L<String>, PhpExpression>> parseTupleAssignment(MultiassignmentExpression multi, PsiElement varRef, Opt<String> keyOpt)
    {
        List<Variable> vars = L(multi.getVariables())
            // the var will be wrapped in PhpPsiElementImpl in case of [$a]
            .fop(v -> Tls.cast(Variable.class, v)
                .elf(() -> opt(v.getFirstPsiChild())
                    .cst(Variable.class)))
            .arr();
        int order = vars.indexOf(varRef);
        if (order < 0) {
            return non();
        }
        String key = keyOpt.def(order + "");
        return opt(multi.getValue())
            /** assigned value is wrapped inside an "expression impl" */
            .fop(v -> opt(v.getFirstPsiChild()))
            .fop(toCast(PhpExpression.class))
            .fop(assignedValue -> {
                // for now handling just simple list($a, $b) = ...
                // ignoring stuff like list($a, list($b)) = ...
                L<String> keyPath = list(key);
                return som(T2(keyPath, assignedValue));
            });
    }

    /** @return tuple: (keyPath, sourceArrayExpression) */
    private static Opt<T2<L<String>, PhpExpression>> parseAsTupleAssignment(PsiElement varRef)
    {
        return opt(varRef.getParent()).fop(p -> Opt.fst(
            // list($a, $b) = $tuple;
            () -> Tls.cast(MultiassignmentExpressionImpl.class, p)
                /** array creation is wrapped inside an "expression impl" */
                .fop(multi -> parseTupleAssignment(multi, varRef, assertListKey(varRef))),
            // [$a, $b] = $tuple;
            () -> Tls.cast(PhpPsiElementImpl.class, p)
                .fop(wrapperPsi -> opt(wrapperPsi.getParent()))
                .fop(valParent -> {
                    Opt<String> keyOpt = non();
                    if (valParent instanceof ArrayHashElement) {
                        keyOpt = opt(((ArrayHashElement) valParent).getKey())
                            .cst(StringLiteralExpression.class)
                            .map(lit -> lit.getContents());
                        valParent = valParent.getParent();
                    }
                    Opt<String> keyOptf = keyOpt;
                    return opt(valParent)
                        .cst(ArrayCreationExpression.class)
                        .fop(arrCtor -> opt(arrCtor.getParent()))
                        .cst(MultiassignmentExpression.class)
                        .fop(multi -> parseTupleAssignment(multi, varRef, keyOptf));
                })
        ));
    }

    private Opt<S<IIt<DeepType>>> assertTupleAssignment(PsiElement varRef)
    {
        return parseAsTupleAssignment(varRef)
            .map(tuple -> () -> tuple
                .nme((keyPath, arrVal) -> ctx.findExprType(arrVal)
                    .fap(arrt -> {
                        It<DeepType> takenTit = list(arrt).itr();
                        for (String key: keyPath) {
                            takenTit = takenTit.fap(t -> Mt.getKeySt(t, key));
                        }
                        return takenTit;
                    })));
    }

    private Opt<S<IIt<DeepType>>> assertPregMatchResult(PsiElement varRef)
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
                IIt<DeepType> tit = ctx.findExprType(regexPsi);
                return makeRegexNameCaptureTypes(tit);
            });
    }

    private static IIt<PsiElement> findDeclarations(Variable variable)
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
            .fap(refPsi -> It.cnc(non()
                , Tls.cast(PhpDocVarImpl.class, refPsi)
                    .fop(varDoc -> opt(varDoc.getParent()))
                    .fop(toCast(PhpDocTag.class))
                    .fap(docTag -> new DocParamRes(ctx).resolve(docTag))
                , Tls.cast(Variable.class, refPsi)
                    .fop(decl -> opt(decl.getDocComment()))
                    .fap(docComment -> PsalmRes.resolveVar(
                        docComment, variable.getName(), ctx)
                    )
            ));
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
                    S<IIt<DeepType>> mtg = () -> new ArgRes(ctx).resolveArg(param);
                    return new Assign(list(), mtg, true, refPsi, param.getType());
                })
            , () -> Tls.cast(Variable.class, refPsi)
                .fop(vari -> opt(vari.getParent()))
                .cst(UnaryExpression.class) // ++$i
                .map(una -> new Assign(list(), () -> It(som(DeepType.makeInt(una, null))), true, una, una.getType()))
            , () -> assertDeclFromGlobal(refPsi)
                .map(elTypes -> new Assign(list(), elTypes, didSurelyHappen, refPsi, PhpType.MIXED))
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
        IIt<PsiElement> references = findDeclarations(caretVar)
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
            .fap(vari -> {
                It<DeepType> thist = ctx.getThisType();
                // it's more convenient to type $this in
                // doc comments instead of new self or such
                if (ctx.isInComment()) {
                    thist = It.cnc(thist, ctx.getSelfType()
                        .map(pst -> new DeepType(caretVar, pst)));
                }
                return thist;
            });

        It<DeepType> closureType = ctx.getClosureVars().itr()
            .flt(t2 -> t2.a.equals(caretVar.getName()))
            .fap(t2 -> t2.b.get());

        It<DeepType> superGlobalTit = resolveSuperGlobal(caretVar);

        return It.cnc(
            docTypes, superGlobalTit,
            hasClassInfo(typeFromIdea.briefType)
                ? som(typeFromIdea) : non(),
            thisType, closureType,
            AssRes.assignmentsToTypes(asses)
        )   .orr(() -> assertDeclFromGlobal(caretVar)
                .fap(f -> f.get()).iterator())
            .orr(som(typeFromIdea));
    }

}
