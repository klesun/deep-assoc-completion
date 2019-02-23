package org.klesun.lang.testing;

import java.util.ArrayList;
import java.util.List;

public class Error
{
    String message;
    String dataProviderName;
    List<String> keyChain;
    int testNumber;

    public Error(CaseContext ctx, String msg)
    {
        this.dataProviderName = ctx.dataProviderName;
        this.keyChain = new ArrayList(ctx.keyChain);
        this.testNumber = ctx.testNumber;
        this.message = msg;
    }
}
