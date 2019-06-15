package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.*;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.go_to_decl_providers.impl.AssocKeyGoToDecl;
import org.klesun.deep_assoc_completion.go_to_decl_providers.impl.DeepObjMemberGoToDecl;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;

import java.util.Objects;

import static org.klesun.lang.Lang.non;
import static org.klesun.lang.Lang.opt;

/**
 * encapsulates the logic of iterable options interactivity and formatting
 * IDEA SDK only allows to return array of options, not iterator - so we instantly
 * go to the first option on the first _Ctrl + B_ and if user requests same key
 * for the second time, we perform complete search and return all values
 */
public class MainGoToDecl implements GotoDeclarationHandler {
    private Opt<PsiElement> lastCaretPsi = non();
    private boolean lastFinished = true;

    private static boolean areSamePsi(PsiElement declPsi, PsiElement caretPsi)
    {
        if (Objects.equals(declPsi, caretPsi)) {
            return true;
        } else if (!Objects.equals(declPsi.getContainingFile(), caretPsi.getContainingFile())) {
            return false;
        } else {
            PsiFile file = declPsi.getContainingFile();
            int declLine = file.getText().substring(0, declPsi.getTextOffset()).split("\n").length;
            int caretLine = file.getText().substring(0, caretPsi.getTextOffset()).split("\n").length;
            return declLine == caretLine;
        }
    }

    private It<? extends PsiElement> resolveDeclPsis(@NotNull PsiElement psiElement, int mouseOffset, FuncCtx funcCtx)
    {
        return It.cnc(non()
            , AssocKeyGoToDecl.resolveDeclPsis(psiElement, mouseOffset, funcCtx)
            , DeepObjMemberGoToDecl.resolveDeclPsis(psiElement, mouseOffset, funcCtx)
            , ArrayColumnPvdr.resolveDeclPsis(psiElement, mouseOffset)
            , ArrayKeyExistsPvdr.resolveDeclPsis(psiElement, mouseOffset)
            , VarNamePvdr.resolveDeclPsis(psiElement, mouseOffset)
            , opt(psiElement.getParent()) // [self::class, 'soSomeStuff']
                .cst(StringLiteralExpressionImpl.class)
                .fap(lit -> It.cnc(It.non()
                    , ArrFuncRefNamePvdr.resolve(lit, false)
                        .map(t -> t.a)
                        .flt(meth -> meth.getName().equals(lit.getContents()))
                        .map(a -> a)
                    , StrValsPvdr.resolve(lit, false)
                        .flt(t -> lit.getContents().equals(t.stringValue))
                        .map(t -> t.definition)
                ))
        ).flt(declPsi -> !areSamePsi(declPsi, psiElement));
    }

    private static PsiElement truncateOnLineBreak(PsiElement psi)
    {
        PsiElement truncated = psi.getFirstChild();
        while (psi.getText().contains("\n") && truncated != null) {
            psi = truncated;
            truncated = psi.getFirstChild();
        }
        return psi;
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement nullPsi, int mouseOffset, Editor editor)
    {
        Boolean prevFinished = lastFinished;
        lastFinished = false;
        Boolean isSecondAttempt = lastCaretPsi.map(last -> last.equals(nullPsi)).def(false);
        It<PsiElement> psiit = opt(nullPsi)
            .fap(psiElement -> {
                SearchCtx search = new SearchCtx(psiElement.getProject())
                    .setDepth(AssocKeyPvdr.getMaxDepth(false, psiElement.getProject()));
                FuncCtx funcCtx = new FuncCtx(search);
                return resolveDeclPsis(psiElement, mouseOffset, funcCtx)
                    .map(psi -> truncateOnLineBreak(psi));
            });
        psiit = psiit.end((itpsi) -> !isSecondAttempt || !prevFinished).unq();
        L<PsiElement> arr = psiit.arr();
        lastCaretPsi = opt(nullPsi);
        lastFinished = true;
        return arr.toArray(new PsiElement[arr.size()]);
    }

    // need for pre-183.5153.4
    public @Nullable String getActionText(DataContext dataContext) {
        return null;
    }
}
