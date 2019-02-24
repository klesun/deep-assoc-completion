package org.klesun.lang.testing;

import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.klesun.lang.Lang.*;

public class CaseContext
{
    Logger logger;

    public String dataProviderName;
    /** used only in legacy test that tests keys recursively */
    public List<String> keyChain = list();
    public int testNumber;

    public CaseContext(Logger logger)
    {
        this.logger = logger;
    }

    public L<Error> testCasePartial(List<DeepType.Key> actual, DeepType.Key expected) {
        logger.setCaseContext(this);
        L<Error> errors = list();

        DeepType expectedt = expected.getTypes().fst().unw();
        expectedt.keys.fch((subExpected) -> subExpected.keyType.getNames().fch(subKey -> {
            It<DeepType.Key> havingKey = Lang.It(actual)
                .fap(krecs -> krecs.getTypes())
                .fap(t -> t.keys)
                .flt(k -> k.keyType.getNames().any(n -> n.equals(subKey)));

            if (!havingKey.has()) {
                logger.logErrShort(som(subKey));
                errors.add(new Error(this, "No such key: " + subKey));
            } else {
                logger.logSucShort(som(subKey));
            }
        }));

        return errors;
    }

    public List<Error> testCaseExact(It<String> actual, L<String> expected) {
        List<Error> errors = list();
        Set<String> expectedAll = new LinkedHashSet<>(expected);
        Set<String> absentKeys = new LinkedHashSet<>(expected);
        Set<String> unexpectedKeys = new LinkedHashSet<>();
        for (String actualKey: actual) {
            if (expectedAll.contains(actualKey)) {
                if (absentKeys.contains(actualKey)) {
                    absentKeys.remove(actualKey);
                    logger.logSucShort(som(actualKey));
                }
            } else {
                if (!unexpectedKeys.contains(actualKey)) {
                    unexpectedKeys.add(actualKey);
                    logger.logErrShort(som(actualKey));
                }
            }
        }
        if (!absentKeys.isEmpty()) {
            errors.add(new Error(this, "Result does not have expected keys: " + Tls.implode(", ", Lang.L(absentKeys))));
            logger.logErrShort(som(It(absentKeys).str(", ")));
        }
        if (!unexpectedKeys.isEmpty()) {
            errors.add(new Error(this, "Result has unexpected keys: " + Tls.implode(", ", Lang.L(unexpectedKeys))));
        }
        return errors;
    }
}
