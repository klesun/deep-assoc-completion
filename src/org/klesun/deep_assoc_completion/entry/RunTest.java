package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.StatementWithArgumentImpl;
import org.klesun.deep_assoc_completion.completion_providers.AssocKeyPvdr;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.ClosRes;
import org.klesun.deep_assoc_completion.resolvers.UsageBasedTypeResolver;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyEntry;
import org.klesun.lang.*;
import org.klesun.lang.testing.CaseContext;
import org.klesun.lang.testing.Error;
import org.klesun.lang.testing.Logger;

import static org.klesun.lang.Lang.*;

public class RunTest extends AnAction
{
    private static Opt<It<Method>> findTestDataPvdrFuncs(PsiFile psiFile)
    {
        It<Method> meths = It(PhpIndex.getInstance(psiFile.getProject()).getClassesByName("UnitTest"))
            .fap(PhpClass::getMethods)
            .flt(m -> m.getName().startsWith("provide"));

        return meths.has() ? opt(meths) : opt(null);
    }

    private static Opt<It<Method>> findExactKeysTestDataPvdrFuncs(PsiFile psiFile)
    {
        It<Method> meths = It(PhpIndex.getInstance(psiFile.getProject()).getClassesByName("ExactKeysUnitTest"))
            .fap(PhpClass::getMethods)
            .flt(m -> m.getName().startsWith("provide"));

        return meths.has() ? opt(meths) : opt(null);
    }

    private static Opt<It<Method>> findUsageTestDataPvdrFuncs(PsiFile psiFile)
    {
        It<Method> meths = It(PhpIndex.getInstance(psiFile.getProject()).getClassesByName("UsageResolverUnitTest"))
            .fap(PhpClass::getMethods)
            .flt(m -> m.getName().startsWith("provide"));

        return meths.has() ? opt(meths) : opt(null);
    }

    private static IExprCtx makeNewExprCtx(PsiElement psi)
    {
        SearchCtx search = new SearchCtx(psi.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(false, psi.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);
        ExprCtx exprCtx = new ExprCtx(funcCtx, psi, 0);
        return exprCtx;
    }

    private static It<DeepType> getReturnType(Method func)
    {
        return ClosRes.findFunctionReturns(func)
            .map(StatementWithArgumentImpl::getArgument)
            .fop(toCast(PhpExpression.class))
            .fap(retVal -> makeNewExprCtx(retVal).findExprType(retVal));
    }

    private static It<T3<CaseContext, KeyEntry, KeyEntry>> parseReturnedTestCase(Method func, Logger logger)
    {
        return getReturnType(func)
            .fap(t -> Mt.getKeySt(t, null))
            .fap((rett, i) -> {
                CaseContext ctx = new CaseContext(logger);
                ctx.dataProviderName = func.getName();
                ctx.testNumber = i;
                return rett.keys.flt(k -> k.keyType.getNames().any(n -> n.equals("0")))
                    .fap(actual -> rett.keys.flt(k -> k.keyType.getNames().any(n -> n.equals("1")))
                        .map(expected -> T3(ctx, actual, expected)));
            });
    }

    private static It<T3<CaseContext, Mt, Mt>> parseArgTestCase(Method func, Logger logger)
    {
        It<DeepType> retit = getReturnType(func);
        L<String> funcArgNames = It(func.getParameters())
            .map(Parameter::getName).arr();
        return retit.fap(rett -> {
            It<String> testArgNames = rett.keys.fap(k -> k.keyType.getNames()).unq();
            return testArgNames.map((argName, i) -> {
                int argOrder = funcArgNames.indexOf(argName);
                IExprCtx exprCtx = makeNewExprCtx(func);
                Mt actual = new UsageBasedTypeResolver(exprCtx, 10)
                    .findArgTypeFromUsage(func, argOrder, exprCtx.subCtxEmpty())
                    .wap(Mt::mem);
                Mt expected = Mt.getKeySt(rett, argName).wap(Mt::mem);
                CaseContext caseCtx = new CaseContext(logger);
                caseCtx.dataProviderName = func.getName();
                caseCtx.testNumber = i;
                return T3(caseCtx, actual, expected);
            });
        });
    }

    @Override
    public void actionPerformed(AnActionEvent e)
    {
        Logger logger = new Logger();
        logger.scheduleBg(() -> {
            It<Error> exactKeyErrors = opt(e.getData(LangDataKeys.PSI_FILE))
                .fop(RunTest::findExactKeysTestDataPvdrFuncs)
                .els(() -> System.out.println("Failed to find data-providing functions"))
                .fap(funcs -> funcs.fap(f -> parseReturnedTestCase(f, logger)))
                .fap(tu -> tu.nme((ctx, actual, expected) -> {
                    L<String> expectedKeys = Mt.mem(expected.getValueTypes())
                        .getEl().getStringValues().arr();
                    It<String> actualKeys = actual.getValueTypes()
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
                .fop(RunTest::findTestDataPvdrFuncs)
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
            It<Error> usageErrors = opt(e.getData(LangDataKeys.PSI_FILE))
                .fop(RunTest::findUsageTestDataPvdrFuncs)
                .els(() -> System.out.println("Failed to find usage data-providing functions"))
                .fap(funcs -> funcs.fap(f -> {
                    It<T3<CaseContext, Mt, Mt>> tests = parseArgTestCase(f, logger);
                    if (!tests.has()) {
                        CaseContext ctx = new CaseContext(logger);
                        String msg = "No tests in provide* function " + f.getName();
                        logger.logErrShort(som(f.getName()));
                        return list(new Error(ctx, msg));
                    } else {
                        return tests.fap(tuple -> tuple.nme((ctx, actual, expected)-> {
                            try {
                                //logger.logMsg("doing " + tuple.a.dataProviderName + " #" + tuple.a.testNumber);
                                return ctx.testCaseExactFull(actual, expected);
                            } catch (Throwable exc) {
                                String msg = "Exception was thrown: " + exc.getClass() + " " + exc.getMessage()
                                    + "\n" + Tls.getStackTrace(exc)
                                    ;
                                return list(new Error(ctx, msg));
                            }
                        }));
                    }
                }));

            return It.cnc(usageErrors, errors, exactKeyErrors);
        });
    }
}
