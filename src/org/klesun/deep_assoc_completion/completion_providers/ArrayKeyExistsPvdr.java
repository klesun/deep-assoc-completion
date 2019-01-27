package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.lang.It;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;

public class ArrayKeyExistsPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElementBuilder makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
                .bold()
                .withIcon(AssocKeyPvdr.getIcon())
                .withTypeText(type);
    }

    private static It<? extends LookupElement> makeOptions(It<DeepType> tit)
    {
        Mutable<Boolean> hadArrt = new Mutable<>(false);
        return tit.fap(type -> It.cnc(
            type.keys.fap(keyEntry -> {
                String typeStr = keyEntry.getTypes().fst().map(t -> t.briefType.toString()).def("unknown");
                return keyEntry.keyType.getNames()
                    .map(keyName -> makeLookupBase(keyName, typeStr));
            }),
            It(type.getListElemTypes()).fst().flt(t -> type.keys.size() == 0).fap(t -> {
                if (!hadArrt.get()) {
                    hadArrt.set(true);
                    return Tls.range(0, 5).map(k -> makeLookupBase(k + "",
                        t.briefType.toString()).withBoldness(false));
                } else {
                    return list();
                }
            })
        ))
            .unq(l -> l.getLookupString())
            ;
    }

    private static It<DeepType> resolveArrayKeyExists(StringLiteralExpression literal, FuncCtx funcCtx)
    {
        return opt(literal.getParent())
            .map(argList -> argList.getParent())
            .fop(toCast(FunctionReferenceImpl.class))
            .flt(call -> list("array_key_exists", "key_exists").contains(call.getName()))
            .fop(call -> L(call.getParameters()).gat(1))
            .fop(toCast(PhpExpression.class))
            .fap(arr -> funcCtx.findExprType(arr));
    }

    private static It<DeepType> resolve(StringLiteralExpression lit, boolean isAutoPopup)
    {
        SearchCtx search = new SearchCtx(lit.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(isAutoPopup, lit.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return It.cnc(
            resolveArrayKeyExists(lit, funcCtx)
        );
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        long startTime = System.nanoTime();
        It<DeepType> tit = opt(parameters.getPosition().getParent()) // StringLiteralExpressionImpl
            .fop(toCast(StringLiteralExpression.class))
            .fap(lit -> resolve(lit, parameters.isAutoPopup()));

        makeOptions(tit).fch(result::addElement);
        long elapsed = System.nanoTime() - startTime;
    }

    // ================================
    //  GotoDeclarationHandler part follows
    // ================================

    public static It<PsiElement> resolveDeclPsis(@NotNull PsiElement psi, int mouseOffset)
    {
        return opt(psi.getParent())
            .fop(toCast(StringLiteralExpressionImpl.class))
            .fap(lit -> resolve(lit, false)
                .fap(t -> t.keys)
                .fap(k -> k.keyType.getTypes())
                .flt(kt -> lit.getContents().equals(kt.stringValue))
                .map(t -> t.definition));
    }
}
