package org.klesun.deep_keys.entry;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.DeepTypeResolver;
import org.klesun.lang.Lang;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ShowDocs extends AnAction
{
    @Override
    public void actionPerformed(AnActionEvent e)
    {
        Lang.opt(e.getData(LangDataKeys.PSI_ELEMENT))
            .thn(psi -> {
                List<DeepType> types = DeepTypeResolver.findExprType(psi, 20);
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
