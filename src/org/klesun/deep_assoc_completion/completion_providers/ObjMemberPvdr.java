package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.MemberReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClassMember;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
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
            .withIcon(DeepKeysPvdr.getIcon())
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

    private static It<LookupElement> getDynamicProps(Mt mt)
    {
        return mt.getProps().arr().dct(p -> T2(p.name, p)).vls()
            .map(prop -> LookupElementBuilder.create(prop.name)
                .bold()
                .withIcon(PhpIcons.FIELD)
                .withTypeText(prop.getBriefTypes().wap(its -> {
                    PhpType ideaType = new PhpType();
                    its.fch(ideaType::add);
                    return ideaType.toString();
                })));
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        L<LookupElement> builtIns = list();
        result.runRemainingContributors(parameters, otherSourceResult -> {
            builtIns.add(otherSourceResult.getLookupElement());
        });
        SearchContext search = new SearchContext(parameters)
            .setDepth(DeepKeysPvdr.getMaxDepth(parameters));
        Dict<Long> times = new Dict<>(list());
        It<? extends PhpClassMember> members = It(list());
        if (builtIns.size() == 0) {
            FuncCtx funcCtx = new FuncCtx(search);
            members = opt(parameters.getPosition().getParent())
                .fop(toCast(MemberReference.class))
                .map(mem -> mem.getClassReference())
                .fap(ref -> {
                    // IDEA did not resolve the class on it's own - worth trying Deep resolution
                    Mt mt = funcCtx.findExprType(ref).wap(Mt::new);
                    getDynamicProps(mt).fch(el -> result.addElement(el));
                    return ArrCtorRes.resolveMtCls(mt, ref.getProject())
                        .fap(cls -> list(
                            It(cls.getMethods()).flt(m -> !m.getName().startsWith("__")),
                            It(cls.getFields())
                        ).fap(a -> a)
                            .flt(fld -> fld.getModifier().isPublic()
                                || ref.getText().equals("$this"))
                            .flt(fld -> !fld.getModifier().isStatic())
                        );
                });
            times.put("iteratorDone", System.nanoTime() - startTime);
        }
        Set<String> suggested = new HashSet<>(builtIns.map(l -> l.getLookupString()).arr());
        members.map(m -> makeLookup(m))
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
