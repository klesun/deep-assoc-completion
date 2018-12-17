package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.go_to_decl_providers.impl.DeepKeysGoToDecl;
import org.klesun.deep_assoc_completion.go_to_decl_providers.impl.DeepObjMemberGoToDecl;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;

import static org.klesun.lang.Lang.*;

/**
 * encapsulates the logic of iterable options interactivity and formatting
 * IDEA SDK only allows to return array of options, not iterator - so we instantly
 * go to the first option on the first _Ctrl + B_ and if user requests same key
 * for the second time, we perform complete search and return all values
 */
public class MainGoToDecl implements GotoDeclarationHandler {
    private Opt<PsiElement> lastCaretPsi = non();
    private boolean lastFinished = true;

    private It<? extends PsiElement> resolveDeclPsis(@NotNull PsiElement psiElement, int mouseOffset, FuncCtx funcCtx)
    {
        return It.cnc(non()
            , DeepKeysGoToDecl.resolveDeclPsis(psiElement, mouseOffset, funcCtx).map(a -> a)
            , DeepObjMemberGoToDecl.resolveDeclPsis(psiElement, mouseOffset, funcCtx).map(a -> a)
        );
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
                SearchContext search = new SearchContext(psiElement.getProject())
                    .setDepth(DeepKeysPvdr.getMaxDepth(false, psiElement.getProject()));
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
}
