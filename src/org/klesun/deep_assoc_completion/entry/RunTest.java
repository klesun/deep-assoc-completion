package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.lang.Opt;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.klesun.lang.Lang.*;

public class RunTest extends AnAction
{
    private static Opt<List<Method>> findTestDataPvdrFuncs(PsiFile psiFile)
    {
        List<Method> meths = list();

        L(PhpIndex.getInstance(psiFile.getProject()).getClassesByName("UnitTest"))
            .forEach(cls -> meths.addAll(L(cls.getMethods())
                .flt(m -> m.getName().startsWith("provide"))));

        return meths.size() > 0 ? opt(meths) : opt(null);
    }

    @Override
    public void actionPerformed(AnActionEvent e)
    {
        long startTime = System.nanoTime();
        Logger logger = new Logger();
        logger.logMsg("Searching for \"UnitTest\" class in project...");
        List<Error> errors = opt(e.getData(LangDataKeys.PSI_FILE))
            .fop(file -> findTestDataPvdrFuncs(file))
            .map(funcs -> L(funcs)
                .fap(func -> ClosRes.findFunctionReturns(func)
                    .map(ret -> ret.getArgument())
                    .fop(toCast(PhpExpression.class))
                    .map(retVal -> {
                        SearchContext search = new SearchContext()
                            .setDepth(DeepKeysPvdr.getMaxDepth(false, retVal.getProject()));
                        FuncCtx funcCtx = new FuncCtx(search);
                        return funcCtx.findExprType(retVal).types;
                    })
                    .fap(a -> a)
                    .fap(ltype -> L(ltype.getElemTypes())
                        .fop((rett, i) -> {
                            CaseContext ctx = new CaseContext(logger);
                            ctx.dataProviderName = func.getName();
                            ctx.testNumber = i;
                            return opt(rett.keys.get("0"))
                                .fop(input -> opt(rett.keys.get("1"))
                                    .map(output -> {
                                        try {
                                            return ctx.testCase(list(input), output);
                                        } catch (RuntimeException exc) {
                                            String msg = "Exception was thrown: " + exc.getClass() + " " + exc.getMessage()
                                                // + "\n" + Tls.getStackTrace(exc)
                                                ;
                                            return list(new Error(ctx, msg));
                                        }
                                    }));
                        }).fap(v -> v)
                    )
                ))
            .els(() -> System.out.println("Failed to find data-providing functions"))
            .def(list());

        double seconds = (System.nanoTime() - startTime) / 1000000000.0;
        logger.logMsg("");
        errors.forEach(logger::logErr);
        logger.logMsg("Done testing with " + errors.size() + " errors and " + logger.sucCnt + " OK-s in " + seconds + " s. \n");
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("<pre>" + logger.wholeText + "</pre>", MessageType.INFO, null)
            .setFadeoutTime(300 * 1000)
            .createBalloon()
            .show(RelativePoint.fromScreen(new Point(200, 200)), Balloon.Position.atRight);
    }

    private static class CaseContext
    {
        Logger logger;

        String dataProviderName;
        List<String> keyChain = list();
        int testNumber;

        public CaseContext(Logger logger)
        {
            this.logger = logger;
        }

        private List<Error> testCase(List<DeepType.Key> actual, DeepType.Key expected)
            throws AssertionError // in case input does not have some of output keys
        {
            List<Error> errors = list();

            DeepType expectedt = expected.getTypes().get(0);
            expectedt.keys.forEach((subKey, subExpected) -> {
                L<DeepType.Key> havingKey = L(actual)
                    .fap(krecs -> L(krecs.getTypes())
                        .fop(t -> opt(t.keys.get(subKey))).s);

                if (havingKey.s.size() == 0) {
                    logger.logErrShort();
                    errors.add(new Error(this, "No such key: " + subKey));
                } else {
                    logger.logSucShort();
                    keyChain.add(subKey);
                    testCase(havingKey.s, subExpected);
                    keyChain.remove(keyChain.size() - 1);
                }
            });

            return errors;
        }
    }

    private static class Error
    {
        String message;
        String dataProviderName;
        List<String> keyChain;
        int testNumber;

        Error(CaseContext ctx, String msg)
        {
            this.dataProviderName = ctx.dataProviderName;
            this.keyChain = new ArrayList(ctx.keyChain);
            this.testNumber = ctx.testNumber;
            this.message = msg;
        }
    }

    private static class Logger
    {
        String wholeText = "";
        int caret = 0;
        int sucCnt = 0;

        void logMsg(String msg)
        {
            System.out.println(msg);
            wholeText += msg + "\n";
            caret = 0;
        }

        void printWrapped(String text)
        {
            System.out.print(text);
            wholeText += text;
            L<String> lines = L(text.split("/\n/"));
            if (lines.size() > 1) {
                caret = 0;
            }
            caret += lines.lst().unw().length();
            if (caret > 90) {
                logMsg("");
            }
        }

        void logErr(Error err)
        {
            String msg = "Error in " + err.dataProviderName + " #" + err.testNumber + " " +
                L(err.keyChain).rdc((a,b) -> a + ", " + b, "") + " " + err.message;
            logMsg(msg);
        }

        void logErrShort()
        {
            printWrapped("E");
        }

        void logSucShort()
        {
            printWrapped(".");
            ++sucCnt;
        }
    }
}
