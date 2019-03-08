package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.klesun.deep_assoc_completion.completion_providers.AssocKeyPvdr;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;
import org.klesun.lang.testing.CaseContext;
import org.klesun.lang.testing.Error;
import org.klesun.lang.testing.Logger;

import static org.klesun.lang.Lang.*;

public class RunTest extends AnAction
{
    private static Opt<It<Method>> findTestDataPvdrFuncs(PsiFile psiFile)
    {
        It<Method> meths = It(PhpIndex.getInstance(psiFile.getProject()).getClassesByName("UnitTest"))
            .fap(cls -> cls.getMethods())
            .flt(m -> m.getName().startsWith("provide"));

        return meths.has() ? opt(meths) : opt(null);
    }

    private static Opt<It<Method>> findExactKeysTestDataPvdrFuncs(PsiFile psiFile)
    {
        It<Method> meths = It(PhpIndex.getInstance(psiFile.getProject()).getClassesByName("ExactKeysUnitTest"))
            .fap(cls -> cls.getMethods())
            .flt(m -> m.getName().startsWith("provide"));

        return meths.has() ? opt(meths) : opt(null);
    }

    private static It<T3<CaseContext, DeepType.Key, DeepType.Key>> parseReturnedTestCase(Method func, Logger logger)
    {
        return ClosRes.findFunctionReturns(func)
            .map(ret -> ret.getArgument())
            .fop(toCast(PhpExpression.class))
            .fap(retVal -> {
                SearchCtx search = new SearchCtx(retVal.getProject())
                    .setDepth(AssocKeyPvdr.getMaxDepth(false, retVal.getProject()));
                FuncCtx funcCtx = new FuncCtx(search);
                return funcCtx.findExprType(retVal);
            })
            .fap(t -> Mt.getKeySt(t, null)).arr()
            .fap((rett, i) -> {
                CaseContext ctx = new CaseContext(logger);
                ctx.dataProviderName = func.getName();
                ctx.testNumber = i;
                return rett.keys.flt(k -> k.keyType.getNames().any(n -> n.equals("0")))
                    .fap(actual -> rett.keys.flt(k -> k.keyType.getNames().any(n -> n.equals("1")))
                        .map(expected -> T3(ctx, actual, expected)));
            });
    }

    @Override
    public void actionPerformed(AnActionEvent e)
    {
        Logger logger = new Logger();
        logger.scheduleBg(() -> {
            It<Error> exactKeyErrors = opt(e.getData(LangDataKeys.PSI_FILE))
                .fop(file -> findExactKeysTestDataPvdrFuncs(file))
                .els(() -> System.out.println("Failed to find data-providing functions"))
                .fap(funcs -> funcs.fap(f -> parseReturnedTestCase(f, logger)))
                .fap(tu -> tu.nme((ctx, actual, expected) -> {
                    L<String> expectedKeys = new Mt(expected.getTypes())
                        .getKey(null).getStringValues().arr();
                    It<String> actualKeys = actual.getTypes()
                        .fap(t -> t.keys).fap(k -> k.keyType.getNames());
                    if (expectedKeys.size() == 0 && !expected.definition.getText().equals("[]")) {
                        logger.logErrShort(non());
                        return list(new Error(ctx, "Expected keys are empty"));
                    }
                    try {
                        //logger.logMsg("doing " + tuple.a.dataProviderName + " #" + tuple.a.testNumber);
                        return ctx.testCaseExact(actualKeys, expectedKeys);
                    } catch (Throwable exc) {
                        String msg = "Exception was thrown: " + exc.getClass() + " " + exc.getMessage()
                            + "\n" + Tls.getStackTrace(exc)
                            ;
                        return list(new Error(ctx, msg));
                    }
                }));
            It<Error> errors = opt(e.getData(LangDataKeys.PSI_FILE))
                .fop(file -> findTestDataPvdrFuncs(file))
                .els(() -> System.out.println("Failed to find data-providing functions"))
                .fap(funcs -> funcs.fap(f -> parseReturnedTestCase(f, logger)))
                .fap(tuple -> {
                    try {
                        //logger.logMsg("doing " + tuple.a.dataProviderName + " #" + tuple.a.testNumber);
                        return tuple.a.testCasePartial(list(tuple.b), tuple.c);
                    } catch (Throwable exc) {
                        String msg = "Exception was thrown: " + exc.getClass() + " " + exc.getMessage()
                            + "\n" + Tls.getStackTrace(exc)
                            ;
                        return list(new Error(tuple.a, msg));
                    }
                });

            return It.cnc(errors, exactKeyErrors);
        });
    }
}
