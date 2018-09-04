package org.klesun.deep_assoc_completion.entry;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.KeyUsageResolver;
import org.klesun.lang.Tls;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.klesun.lang.Lang.*;

public class ShowDocs extends AnAction
{
    public static List<DeepType> findPsiType(PsiElement psi)
    {
        SearchContext search = new SearchContext(psi.getProject())
            .setDepth(DeepKeysPvdr.getMaxDepth(false, psi.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return list(
            Tls.cast(PhpExpression.class, psi)
                .fap(expr -> funcCtx.findExprType(expr).types),
            Tls.cast(Parameter.class, psi)
                .fap(par -> {
                    int order = opt(par.getParent()).fop(toCast(ParameterList.class))
                        .map(lst -> L(lst.getParameters()).indexOf(par)).def(-1);
                    return opt(par.getParent())
                        .map(lst -> lst.getParent())
                        .fop(toCast(Function.class))
                        .fap(func -> {
                            DeepType arrt = new DeepType(par, PhpType.ARRAY);
                            L<String> keys = new KeyUsageResolver(funcCtx, 3)
                                .resolveArgUsedKeys(func, order).getKeyNames();
                            keys.fch(k -> arrt.addKey(k, psi));
                            return list(arrt);
                        });
                })
        ).fap(a -> a);
    }

    public void actionPerformed(AnActionEvent e)
    {
        String doc = opt(e.getData(LangDataKeys.PSI_FILE))
            .fop(psiFile -> opt(e.getData(LangDataKeys.CARET))
                .map(caret -> psiFile.findElementAt(caret.getOffset())))
            .map(psi -> psi instanceof LeafPsiElement ? psi.getParent() : psi)
            .map(psi -> DeepType.varExport(findPsiType(psi)))
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
