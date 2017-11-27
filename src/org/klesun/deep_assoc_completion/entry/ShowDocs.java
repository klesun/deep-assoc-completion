package org.klesun.deep_assoc_completion.entry;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.opt;

public class ShowDocs extends AnAction
{
    public static List<DeepType> findPsiType(PsiElement psi)
    {
        SearchContext search = new SearchContext().setDepth(20);
        IFuncCtx funcCtx = new FuncCtx(search, L());

        return Tls.cast(PhpExpression.class, psi)
            .map(expr -> funcCtx.findExprType(expr).types)
            .def(Lang.list());
    }

    public void actionPerformed(AnActionEvent e)
    {
        String doc = opt(e.getData(LangDataKeys.PSI_FILE))
            .fap(psiFile -> opt(e.getData(LangDataKeys.CARET))
                .map(caret -> psiFile.findElementAt(caret.getOffset())))
            .map(psi -> psi instanceof LeafPsiElement ? psi.getParent() : psi)
            .map(psi -> DeepType.toJson(findPsiType(psi), 0))
            .def("There is no file opened to describe a php variable");

        System.out.println(doc);
        // see https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/editor_components.html
        EditorTextField textArea = new EditorTextField(doc, e.getProject(), JsonFileType.INSTANCE);
        textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        textArea.setEnabled(true); // to make text editable (to navigate with arrows)
        textArea.setOneLineMode(false);
        JBScrollPane scroll = new JBScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(400, 400));
        scroll.setMaximumSize(new Dimension(800, 800));

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scroll, new JLabel("deep-assoc-completion var description"))
            .createPopup()
            .show(new RelativePoint(MouseInfo.getPointerInfo().getLocation()));
    }
}
