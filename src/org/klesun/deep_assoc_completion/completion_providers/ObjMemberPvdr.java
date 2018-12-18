package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.helpers.*;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.deep_assoc_completion.resolvers.KeyUsageResolver;
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

    private static LookupElement makeLookup(PhpClassMember member)
    {
        LookupElementBuilder base = LookupElementBuilder.create(member.getName())
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(member.getType().filterUnknown().toString());

        return Opt.fst(
            () -> Tls.cast(Method.class, member)
                .map(m -> base
                    .withInsertHandler(makeMethInsertHandler())
                    .withTailText("(" +
                        Tls.implode(", ", L(m.getParameters()).map(p -> p.getText())) +
                    ")", true))
            ,
            () -> Tls.cast(Field.class, member)
                .map(f -> base
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
            .fap(__get -> new KeyUsageResolver(ctx.subCtxEmpty(), 10)
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

    private It<? extends LookupElement> resolveObj(PhpExpression ref, FuncCtx funcCtx)
    {
        Mt mt = funcCtx.findExprType(ref).wap(Mt::new);
        return ArrCtorRes.resolveMtCls(mt, ref.getProject())
            .fap(cls -> list(
                It(cls.getMethods()).flt(m -> !m.getName().startsWith("__")),
                It(cls.getFields())
                ).fap(a -> a)
                    .flt(fld -> fld.getModifier().isPublic()
                        || ref.getText().equals("$this"))
                    .flt(fld -> !fld.getModifier().isStatic())
                    .map(m -> makeLookup(m))
                    .cct(getAssignedProps(mt).map(a -> a))
                    .cct(getMagicProps(cls, funcCtx)
                        .fap(t -> makeMagicLookup(t)))
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
        if (builtIns.size() == 0 || !parameters.isAutoPopup()) {
            FuncCtx funcCtx = new FuncCtx(search);
            options = opt(parameters.getPosition().getParent())
                .fop(toCast(MemberReference.class))
                .map(mem -> mem.getClassReference())
                // IDEA did not resolve the class on it's own - worth trying Deep resolution
                .fap(ref -> resolveObj(ref, funcCtx));
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
