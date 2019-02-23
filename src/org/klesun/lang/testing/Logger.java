package org.klesun.lang.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ReadTask;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.klesun.lang.L;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.awt.*;

import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.*;

public class Logger
{
    private boolean flushed = false;
    private String wholeText = "";
    private int caret = 0;
    private int sucCnt = 0;
    private int errCnt = 0;
    private long startTime = System.nanoTime();
    private float balloonUpdatedAt = 0.000f;
    private Balloon currentBalloon;
    private Opt<CaseContext> caseContext = non();

    public Logger()
    {
    }

    public void setCaseContext(CaseContext caseContext)
    {
        this.caseContext = som(caseContext);
    }

    private void updateBalloon()
    {
        ApplicationManager.getApplication().invokeLater(() -> {
            Balloon prev = currentBalloon;
            double seconds = (System.nanoTime() - startTime) / 1000000000.0;
            String summary = "ERRs: " + this.errCnt + "; OKs: " + this.sucCnt + " in " + seconds + " s. \n";
            if (caseContext.has()) {
                CaseContext ctx = caseContext.unw();
                summary += ctx.dataProviderName + "#" + ctx.testNumber + "\n";
            }
            currentBalloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("<pre>" + summary + this.wholeText + "</pre>", MessageType.INFO, null)
                .setShowCallout(false)
                .setFadeoutTime(0)
                .setAnimationCycle(0)
                .setRequestFocus(true)
                .createBalloon();
            currentBalloon.show(RelativePoint.fromScreen(new Point(200, 200)), Balloon.Position.atRight);
            if (prev != null) {
                prev.hide();
            }
        }, ModalityState.any());
    }

    private void logMsg(String msg)
    {
        System.out.println(msg);
        wholeText += msg + "\n";
        caret = 0;
    }

    private void printWrapped(String text)
    {
        System.out.print(text);
        wholeText += text;
        L<String> lines = Lang.L(text.split("/\n/"));
        if (lines.size() > 1) {
            caret = 0;
        }
        caret += lines.lst().unw().length();
        if (caret > 90) {
            logMsg("");
        }
        float timeSec = System.nanoTime() / 1000f / 1000f / 1000f;

        if (timeSec - balloonUpdatedAt > 0.2) {
            updateBalloon();
            balloonUpdatedAt = System.nanoTime() / 1000f / 1000f / 1000f;
        }
    }

    public void logErr(Error err)
    {
        String msg = "Error in " + err.dataProviderName + " #" + err.testNumber + " " +
            Lang.L(err.keyChain).rdc((a, b) -> a + ", " + b, "") + " " + err.message;
        if (!flushed) {
            msg = "\n" + msg;
        }
        logMsg(msg);
    }

    public void logErrShort()
    {
        printWrapped("E");
        ++errCnt;
    }

    public void logSucShort()
    {
        printWrapped(".");
        ++sucCnt;
    }

    private void showResultPopup(Iterable<Error> errors)
    {
        L<Error> msgs = L(errors);
        this.flushed = true;
        this.logMsg("");
        msgs.fch(this::logErr);
        double seconds = (System.nanoTime() - startTime) / 1000000000.0;
        this.logMsg("Done testing with " + this.errCnt + " errors and " + this.sucCnt + " OK-s in " + seconds + " s. \n");
        updateBalloon();
    }

    public void scheduleBg(Lang.S<? extends Iterable<Error>> process)
    {
        ProgressIndicatorUtils.scheduleWithWriteActionPriority(new ReadTask() {
            public void computeInReadAction(ProgressIndicator ind) {
                logMsg("Searching for \"UnitTest\" class in project...");
                Iterable<Error> msgs = process.get();
                showResultPopup(msgs);
                ind.stop();
            }
            public void onCanceled(@NotNull ProgressIndicator progressIndicator) {
                logMsg("Process cancelled");
            }
        });
    }
}
