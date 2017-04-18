package org.klesun.deep_keys.entry;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.ui.awt.RelativePoint;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ParameterImpl;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.klesun.lang.Lang.opt;

public class ShowDocs extends AnAction
{
    public static List<DeepType> findPsiType(PsiElement psi)
    {
        return Opt.fst(Lang.list(
            Tls.cast(PhpExpression.class, psi)
                .map(expr -> DeepTypeResolver.findExprType(expr, 20)),
            Tls.cast(ParameterImpl.class, psi)
                .fap(param -> DeepTypeResolver.findParamType(param, 20))
                .map(assign -> assign.assignedType)
        )).def(Lang.list());
    }

    public void actionPerformed(AnActionEvent e)
    {
        opt(e.getData(LangDataKeys.PSI_ELEMENT))
            .thn(psi -> {
                List<DeepType> types = findPsiType(psi);
                String doc = DeepType.toJson(types, 0);
                System.out.println(doc);
                JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder("<pre>" + doc + "</pre>", MessageType.INFO, null)
                    .setFadeoutTime(300 * 1000)
                    .createBalloon()
                    .show(RelativePoint.fromScreen(new Point(200, 200)), Balloon.Position.atRight);
            });
    }
}
