package org.klesun.deep_assoc_completion.entry.transpile;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.klesun.lang.L;
import org.klesun.lang.Lang;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class TranspiledPhpBuiltIns extends AnAction
{
    @Override
    public void actionPerformed(AnActionEvent e)
    {
        BufferedReader txtReader = new BufferedReader(
            new InputStreamReader(getClass().getResourceAsStream("php.js"))
        );
        L<String> lines = Lang.L(txtReader.lines().collect(Collectors.toList()));

        System.out.println(lines.str("\n"));
        TranspileToNodeJs.openAsScratchFile(lines.str("\n"), e, "php.js");
    }
}
