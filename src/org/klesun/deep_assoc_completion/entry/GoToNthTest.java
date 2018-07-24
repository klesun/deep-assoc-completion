package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.Function;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import javax.swing.*;

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
        SearchContext search = new SearchContext().setDepth(20);
        FuncCtx funcCtx = new FuncCtx(search);
        Opt<PsiFile> psiFileOpt = opt(e.getData(LangDataKeys.PSI_FILE));
        Opt<Caret> caretOpt = opt(e.getData(LangDataKeys.CARET));

        String msg = "Enter the index of PHPUnit test in the current function";
        askForText(msg, (testNumStr) -> opt(toInt(testNumStr).def(0))
            .fop(testNum -> psiFileOpt
                .fop(psiFile -> caretOpt
                    .map(caret -> opt(psiFile.findElementAt(caret.getOffset()))
                        .fop(psi -> Tls.findParent(psi, Function.class, a -> true))
                        .map(func -> ClosRes.getReturnedValue(func, funcCtx))
                        .map(mt -> mt.getEl().getKey("0")) // first arg passed to the testing function
                        .fop(argMt -> argMt.types.gat(testNum))
                        .thn(test -> {
                            caret.moveToOffset(test.definition.getTextOffset());
                            caret.getEditor().getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                        })
                        .els(() -> {
                            // TODO: show properly to the user with a pop-up or something
                            System.out.println("Failed to find " + testNum + "-th test in this function");
                        })))));
    }
}
