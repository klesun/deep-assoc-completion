package org.klesun.deep_keys.entry;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ParameterImpl;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.deep_keys.helpers.FuncCtx;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.SearchContext;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
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
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        int offset = e.getData(LangDataKeys.CARET).getOffset();
        PsiElement psi = psiFile.findElementAt(offset);

        if (psi instanceof LeafPsiElement) {
            psi = psi.getParent();
        }

        List<DeepType> types = findPsiType(psi);
        String doc = DeepType.toJson(types, 0);
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
            .createComponentPopupBuilder(scroll, new JLabel("Hello World"))
            .createPopup()
            .show(new RelativePoint(MouseInfo.getPointerInfo().getLocation()));
    }
}
