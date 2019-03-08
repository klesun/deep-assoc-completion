package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.deep_assoc_completion.resolvers.UsageResolver;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.*;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.lang.Lang.*;

/**
 * provides completion of method names in `[Some\ClassName::class, '']`
 */
public class ObjMemberPvdr extends CompletionProvider<CompletionParameters>
{
    private static InsertHandler<LookupElement> makeMethInsertHandler()
    {
        return (ctx, lookup) -> {
            int to = ctx.getTailOffset();
            // adding parentheses around caret
            ctx.getEditor().getDocument().insertString(to, "(");
            ctx.getEditor().getDocument().insertString(to + 1, ")");
            ctx.getEditor().getCaretModel().moveToOffset(to + 1);
        };
    }

    private static LookupElementBuilder makeBase(String name, PhpType psType)
    {
        return LookupElementBuilder.create(name)
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(psType.filterUnknown().toString());
    }

    private static LookupElement makeLookup(PhpClassMember member, boolean isStatic)
    {
        LookupElementBuilder base = makeBase(member.getName(), member.getType());
        Boolean isConst = member instanceof ClassConstImpl;
        String prefix = (isStatic && !isConst ? "$" : "");
        return Opt.fst(
            () -> Tls.cast(Method.class, member)
                .map(m -> base
                    .withInsertHandler(makeMethInsertHandler())
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
                    .bold()
                    .withIcon(AssocKeyPvdr.getIcon())
                    .withTypeText(prop.getBriefTypes().wap(its -> {
                        PhpType ideaType = new PhpType();
                        its.fch(ideaType::add);
                        return ideaType.toString();
                    }))))
            .unq(l -> l.getLookupString())
            ;
    }

    public static It<DeepType> getMagicProps(PhpClass cls, FuncCtx funcCtx)
    {
        IExprCtx ctx = new ExprCtx(funcCtx, cls, 0);
        return It(cls.getMethods())
            .flt(m -> m.getName().equals("__get"))
            .fap(__get -> new UsageResolver(ctx.subCtxEmpty(), 10)
                .findArgTypeFromUsage(__get, 0, ctx));
    }

    private static Opt<LookupElement> makeMagicLookup(DeepType t)
    {
        return opt(t.stringValue)
            .map(propName -> LookupElementBuilder.create(propName)
                .bold()
                .withIcon(AssocKeyPvdr.getIcon())
                .withTypeText("from __get()"));
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

    private It<? extends LookupElement> resolveObj(PhpExpression ref, FuncCtx funcCtx)
    {
        Mt mt = funcCtx.findExprType(ref).wap(Mt::new);
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

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        L<LookupElement> builtIns = list();
        result.runRemainingContributors(parameters, otherSourceResult -> {
            builtIns.add(otherSourceResult.getLookupElement());
        });
        SearchCtx search = new SearchCtx(parameters)
            .setDepth(AssocKeyPvdr.getMaxDepth(parameters));

        Dict<Long> times = new Dict<>(list());
        It<? extends LookupElement> options = It(list());
        Boolean hasBuiltIns = builtIns.any(b -> !"class".equals(b.getLookupString()));
        if (!hasBuiltIns || !parameters.isAutoPopup()) {
            FuncCtx funcCtx = new FuncCtx(search);
            // IDEA did not resolve the class on it's own - worth trying Deep resolution
            options = opt(parameters.getPosition().getParent())
                .fop(toCast(MemberReference.class))
                .fap(mem -> opt(mem.getClassReference())
                    // no point in deep resolution if it's an explicit class
                    .flt(objRef -> !objRef.getText().equals("$this"))
                    .fap(ref -> resolveObj(ref, funcCtx)));
            times.put("iteratorDone", System.nanoTime() - startTime);
        }
        Set<String> suggested = new HashSet<>(builtIns.map(l -> l.getLookupString()).arr());
        options
            .flt(l -> !suggested.contains(l.getLookupString()))
            .fch((el, i) -> {
                if (i == 0) {
                    times.put("firstSuggested", System.nanoTime() - startTime);
                }
                result.addElement(el);
            });
        times.put("allSuggested", System.nanoTime() - startTime);
        result.addLookupAdvertisement("Resolved " + search.getExpressionsResolved() + " expressions: " +
            Tls.implode(", ", L(times.entrySet()).map(e -> e.getKey() + " in " + e.getValue() / 1000000000.0 + " sec")));
        result.addAllElements(builtIns); // add built-in after ours, this is important
    }
}
