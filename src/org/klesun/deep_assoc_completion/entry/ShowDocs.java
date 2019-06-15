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
import org.klesun.deep_assoc_completion.resolvers.UsageBasedTypeResolver;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.completion_providers.AssocKeyPvdr;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.lang.It;
import org.klesun.lang.Tls;

import javax.swing.*;
import java.awt.*;

import static org.klesun.lang.Lang.*;

public class ShowDocs extends AnAction
{
    public static It<DeepType> findPsiType(PsiElement psi)
    {
        SearchCtx search = new SearchCtx(psi.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(false, psi.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);
        IExprCtx exprCtx = new ExprCtx(funcCtx, psi, 0);

        return It.cnc(
            Tls.cast(PhpExpression.class, psi)
                .fap(expr -> funcCtx.findExprType(expr)),
            Tls.cast(Parameter.class, psi)
                .fap(par -> {
                    int order = opt(par.getParent()).fop(toCast(ParameterList.class))
                        .map(lst -> L(lst.getParameters()).indexOf(par)).def(-1);
                    return opt(par.getParent())
                        .map(lst -> lst.getParent())
                        .fop(toCast(Function.class))
                        .fap(func -> {
                            DeepType arrt = new DeepType(par, PhpType.ARRAY);
                            It<String> keys = new UsageBasedTypeResolver(exprCtx.subCtxEmpty())
                                .findArgTypeFromUsage(func, order, exprCtx).fap(t -> t.keys).fap(k -> k.keyType.getNames()).unq();
                            keys.fch(k -> arrt.addKey(k, psi));
                            return list(arrt);
                        });
                })
        );
    }

    public void actionPerformed(AnActionEvent e)
    {
        String doc = opt(e.getData(LangDataKeys.PSI_FILE))
            .fop(psiFile -> opt(e.getData(LangDataKeys.CARET))
                .map(caret -> psiFile.findElementAt(caret.getOffset())))
            .map(psi -> psi instanceof LeafPsiElement ? psi.getParent() : psi)
            .map(psi -> DeepType.varExport(findPsiType(psi).arr()))
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
