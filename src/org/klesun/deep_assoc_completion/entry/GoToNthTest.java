package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocCommentImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocRefImpl;
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.tags.PhpDocDataProviderImpl;
import com.jetbrains.php.lang.psi.elements.Method;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import javax.swing.*;

import java.util.HashSet;
import java.util.Set;

import static org.klesun.lang.Lang.*;

public class GoToNthTest extends AnAction
{
    private static void askForText(String msg, C<String> then)
    {
        JFormattedTextField input = new JFormattedTextField();
        DialogBuilder dialog = new DialogBuilder().title(msg).centerPanel(input);
        dialog.setPreferredFocusComponent(input);
        dialog.setOkOperation(() -> {
            dialog.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
            then.accept(input.getText());
        });
        dialog.show();
        input.requestFocus();
    }

    private static Opt<Integer> toInt(String numStr)
    {
        try {
            return opt(Integer.parseInt(numStr));
        } catch (NumberFormatException numExc) {
            return opt(null);
        }
    }

    public void actionPerformed(AnActionEvent e)
    {
        SearchContext search = new SearchContext(opt(e.getData(LangDataKeys.EDITOR)).map(ed -> ed.getProject()).def(null))
            .setDepth(20);
        FuncCtx funcCtx = new FuncCtx(search);
        Opt<PsiFile> psiFileOpt = opt(e.getData(LangDataKeys.PSI_FILE));
        Opt<Caret> caretOpt = opt(e.getData(LangDataKeys.CARET));

        String msg = "Enter the index of PHPUnit test in the current function";
        askForText(msg, (testNumStr) -> opt(toInt(testNumStr).def(0))
            .fop(testNum -> psiFileOpt
                .fop(psiFile -> caretOpt
                    .map(caret -> {
                        L<Method> allMeths = It(PsiTreeUtil.findChildrenOfType(psiFile, Method.class)).arr();
                        Set<String> dataProviders = allMeths
                            .fop(meth -> opt(meth.getDocComment()))
                            .fop(toCast(PhpDocCommentImpl.class))
                            .fap(doc -> doc.getDocTagByClass(PhpDocDataProviderImpl.class))
                            .fap(tag -> It(tag.getChildren()))
                            .fop(toCast(PhpDocRefImpl.class))
                            .map(ref -> ref.getText())
                            .wap(sit -> new HashSet<>(sit.arr()))
                            ;
                        L<Method> pvdrMeths = allMeths.flt(m -> dataProviders.contains(m.getName())).arr();

                        Opt<Method> caretFuncOpt = opt(psiFile.findElementAt(caret.getOffset()))
                            .fop(psi -> Tls.findParent(psi, Method.class, a -> true));
                        return caretFuncOpt
                            .fap(func -> pvdrMeths.flt(m -> m.equals(func))).arr()
                            .cct(pvdrMeths).cct(caretFuncOpt)
                            .fap(func -> ClosRes.getReturnedValue(func, funcCtx))
                            .fap(t -> Mt.getElSt(t)) // go to first arg passed to the testing function, not
                            .fap(t -> Mt.getKeySt(t, "0")) // whole set since there may be some wrapping magic
                            .arr().gat(testNum)
                            .thn(test -> {
                                caret.moveToOffset(test.definition.getTextOffset());
                                caret.getEditor().getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                            })
                            .els(() -> {
                                // TODO: show properly to the user with a pop-up or something
                                System.out.println("Failed to find " + testNum + "-th test in this function");
                            });
                    }))));
    }
}
