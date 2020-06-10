package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstImpl;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.deep_assoc_completion.resolvers.UsageBasedTypeResolver;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.psalm.PsalmFuncInfo;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.*;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.lang.Lang.*;

/**
 * provides completion of method names `$someObj->`
 */
public class ObjMemberPvdr extends CompletionProvider<CompletionParameters>
{
    private boolean hadBuiltIns = false;
    private Opt<VariableImpl> strInjectVarOpt = non();

    private InsertHandler<LookupElement> makeMethInsertHandler(Method meth)
    {
        int shift = meth.getParameters().length > 0 ? 1 : 2;

        return (ctx, lookup) -> {
            int from = ctx.getStartOffset();
            int to = ctx.getTailOffset();
            // adding parentheses around caret
            ctx.getEditor().getDocument().insertString(to, "(");
            ctx.getEditor().getDocument().insertString(to + 1, ")");
            ctx.getEditor().getCaretModel().moveToOffset(to + shift);
            strInjectVarOpt.thn(varPsi -> {
                ctx.getEditor().getDocument().insertString(to + "()".length(), "}");
                ctx.getEditor().getDocument().insertString(varPsi.getTextOffset(), "{");
            });
        };
    }

    private LookupElementBuilder makeBase(String name, String psTypeStr)
    {
        LookupElementBuilder kup = LookupElementBuilder.create(name)
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(psTypeStr);
        return hadBuiltIns
            ? kup
                .withItemTextForeground(JBColor.GRAY)
                .withItemTextItalic(true)
            : kup.bold();
    }

    private LookupElementBuilder makeBase(String name, PhpType psType)
    {
        return makeBase(name, psType.filterUnknown().toString());
    }

    private LookupElement makeLookup(PhpClassMember member, boolean isStatic)
    {
        LookupElementBuilder base = makeBase(member.getName(), member.getType());
        Boolean isConst = member instanceof ClassConstImpl;
        String prefix = (isStatic && !isConst ? "$" : "");
        return Opt.fst(
            () -> Tls.cast(Method.class, member)
                .map(m -> base
                    .withInsertHandler(makeMethInsertHandler(m))
                    .withTailText("(" +
                        Tls.implode(", ", L(m.getParameters()).map(p -> p.getText())) +
                    ")", true))
            ,
            () -> Tls.cast(Field.class, member)
                .map(f -> makeBase(prefix + member.getName(), member.getType())
                    .withTailText(opt(f.getDefaultValue())
                        .map(def -> " = " + Tls.singleLine(def.getText(), 50)).def(""), true))
        ).def(base);
    }

    private static It<? extends LookupElement> getAssignedProps(Mt mt)
    {
        return mt.getAssignedProps()
            .fap(prop -> prop.keyType.getNames()
                .map(name -> LookupElementBuilder.create(name)
                    .withIcon(AssocKeyPvdr.getIcon())
                    .withTypeText(prop.getBriefTypes().wap(its -> {
                        PhpType ideaType = new PhpType();
                        its.forEach(ideaType::add);
                        return ideaType.toString();
                    }))))
            .unq(LookupElementBuilder::getLookupString)
            ;
    }

    public static It<DeepType> getMagicProps(PhpClass cls, FuncCtx funcCtx)
    {
        IExprCtx ctx = new ExprCtx(funcCtx, cls, 0);
        return It(cls.getMethods())
            .flt(m -> m.getName().equals("__get"))
            .fap(__get -> new UsageBasedTypeResolver(ctx.subCtxEmpty())
                .findArgTypeFromUsage(__get, 0, ctx));
    }

    private Opt<LookupElement> makeMagicLookup(DeepType t)
    {
        return opt(t.stringValue)
            .map(propName -> makeBase(propName, "from __get()"));
    }

    private It<LookupElement> getPubKups(PhpClass cls, FuncCtx funcCtx, boolean isStatic)
    {
        It<? extends PhpClassMember> mems = list(
            It(cls.getMethods()).flt(m -> !m.getName().startsWith("__")),
            It(cls.getFields())
        ).fap(a -> a);
        return mems
            .flt(fld -> fld.getModifier().isPublic())
            .flt(fld -> isStatic == fld.getModifier().isStatic())
            .map(m -> makeLookup(m, isStatic))
            .cct(getMagicProps(cls, funcCtx)
                .fap(t -> makeMagicLookup(t)));
    }

    private It<LookupElement> resolveObj(PhpExpression ref, FuncCtx funcCtx)
    {
        ExprCtx exprCtx = new ExprCtx(funcCtx, ref, 0);
        Mt mt = exprCtx.findExprType(ref).wap(Mt::mem);
        return It.cnc(
            ArrCtorRes.resolveMtInstCls(mt, ref.getProject())
                .fap(cls -> getPubKups(cls, funcCtx, false))
            ,
            ArrCtorRes.resolveMtClsRefCls(mt, ref.getProject())
                .fap(cls -> getPubKups(cls, funcCtx, true))
            ,
            getAssignedProps(mt).map(a -> a)
        );
    }

    private It<LookupElement> getCustomDocOptions(Opt<PhpExpression> clsRefOpt)
    {
        return clsRefOpt
            .fap(clsRef -> ArrCtorRes.filterObjPst(clsRef.getType())
                .fap(pst -> ArrCtorRes.resolveIdeaTypeCls(pst, clsRef.getProject())))
            .fap(clsPsi -> opt(clsPsi.getDocComment()))
            .map(doc -> PsalmFuncInfo.parseClsDoc(doc))
            .fap(clsInfo -> It.cnc(
                It(clsInfo.magicMethods.keySet()).map(name -> makeBase(name + "()", "from psalm")),
                It(clsInfo.magicProps.keySet()).map(name -> makeBase(name, "from psalm"))
            ));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        Project proj = parameters.getEditor().getProject();
        if (proj == null || !DeepSettings.inst(proj).enableMemberCompletion) {
            return;
        }

        long startTime = System.nanoTime();
        L<LookupElement> builtIns = list();
        result.runRemainingContributors(parameters, otherSourceResult -> {
            LookupElement kup = otherSourceResult.getLookupElement();
            builtIns.add(kup);
            result.addElement(kup);
        });
        Boolean hasBuiltIns = builtIns.any(b -> !"class".equals(b.getLookupString()));
        Set<String> suggested = new HashSet<>(builtIns.map(l -> l.getLookupString()).arr());
        SearchCtx search = new SearchCtx(proj)
            .setDepth(AssocKeyPvdr.getMaxDepth(parameters));

        Dict<String> times = new Dict<>(list());
        It<LookupElement> deepOptions = It(list());
        Opt<PhpExpression> clsRefOpt = opt(parameters.getPosition().getParent())
            .fop(toCast(MemberReference.class))
            .fop(mem -> opt(mem.getClassReference()));

        strInjectVarOpt = clsRefOpt
            .fop(ref -> opt(ref.getParent()))
            .cst(FieldReference.class) // because IntellijiIdeaRulezzz
            .fop(ref -> opt(ref.getParent()))
            .cst(VariableImpl.class) // dunno why AST is this way...
            .flt(ref -> opt(ref.getParent())
                .cst(StringLiteralExpression.class)
                .cst(StringLiteralExpression.class)
                .has());

        if (!hasBuiltIns || !parameters.isAutoPopup()) {
            FuncCtx funcCtx = new FuncCtx(search);
            hadBuiltIns = hasBuiltIns;
            // IDEA did not resolve the class on it's own - worth trying Deep resolution
            deepOptions = clsRefOpt
                // no point in deep resolution if it's an explicit class
                .flt(objRef -> !objRef.getText().equals("$this"))
                .fap(ref -> resolveObj(ref, funcCtx));
            times.put("iteratorDone", (System.nanoTime() - startTime) / 1000000000.0 + " " + search.getExpressionsResolved() + " ex.");
        }
        // IDEA is not able to parse multi-line psalm phpdoc, so always adding these options if any
        It<LookupElement> customDocOptions = getCustomDocOptions(clsRefOpt);
        It<? extends LookupElement> opts = It.cnc(deepOptions, customDocOptions);
        opts
            .flt(l -> !suggested.contains(l.getLookupString()))
            .fch((el, i) -> {
                if (i == 0) {
                    times.put("firstSuggested", (System.nanoTime() - startTime) / 1000000000.0 + " " + search.getExpressionsResolved() + " ex.");
                }
                result.addElement(el);
                suggested.add(el.getLookupString());
            });
        times.put("allSuggested", (System.nanoTime() - startTime) / 1000000000.0 + " " + search.getExpressionsResolved() + " ex.");
        result.addLookupAdvertisement("Resolved " + search.getExpressionsResolved() + " expressions: " +
            Tls.implode(", ", L(times.entrySet()).map(e -> e.getKey() + " in " + e.getValue())));
    }
}
