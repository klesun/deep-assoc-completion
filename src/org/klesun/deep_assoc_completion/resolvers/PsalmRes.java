package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Mkt;
import org.klesun.deep_assoc_completion.structures.psalm.IType;
import org.klesun.deep_assoc_completion.structures.psalm.PsalmParser;
import org.klesun.deep_assoc_completion.structures.psalm.TAssoc;
import org.klesun.deep_assoc_completion.structures.psalm.TClass;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.*;

import java.util.ArrayList;
import java.util.List;

import static org.klesun.lang.Lang.*;

public class PsalmRes {
    final private IExprCtx ctx;

    public PsalmRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private static It<DeepType> psalmToDeep(IType psalmType, PsiElement goToPsi)
    {
        return It.cnc(
            non()
            , Tls.cast(TAssoc.class, psalmType)
                .map(assoc -> Mkt.assoc(goToPsi, Lang.It(assoc.keys.entrySet()).map(e -> {
                    String keyName = e.getKey();
                    IType psalmVal = e.getValue();
                    It<DeepType> valTit = psalmToDeep(psalmVal, goToPsi);
                    return T2(keyName, new Mt(valTit));
                })))
            , Tls.cast(TClass.class, psalmType)
                .map(cls -> {
                    PhpType phpType = new PhpType().add(cls.fqn);
                    DeepType deep = new DeepType(goToPsi, phpType, false);
                    deep.generics = Lang.It(cls.generics)
                        .map(psalm -> psalmToDeep(psalm, goToPsi))
                        .map(tit -> new Mt(tit))
                        .arr();
                    return deep;
                })
        );
    }

    private static It<PsalmDocTag> getRawTags(String docCommentText)
    {
        List<PsalmDocTag> tags = new ArrayList<>();
        Opt<PsalmDocTag> current = non();
        for (String line: docCommentText.split("\\n")) {
            Opt<L<String>> matchOpt = Tls.regexWithFull("\\s*@(\\w+)\\s*(.*)", line, 0);
            if (matchOpt.has()) {
                String tagName = matchOpt.unw().get(1);
                String textLeft = matchOpt.unw().get(2);
                if (current.has()) {
                    tags.add(current.unw());
                }
                current = som(new PsalmDocTag(tagName, It.non(), textLeft));
            } else if (current.has()) {
                String tagName = current.unw().tagName;
                String textLeft = current.unw().textLeft + "\n" + line;
                current = som(new PsalmDocTag(tagName, It.non(), textLeft));
            }
        }
        if (current.has()) {
            tags.add(current.unw());
        }
        return It(tags);
    }

    /**
     * returns the parsed type, and the following comment text if any (preceded by $varName if any)
     */
    private static It<PsalmDocTag> parseDocTags(PhpDocComment docComment)
    {
        return DocParamRes.getDocCommentText(docComment)
            .fap(txt -> getRawTags(txt))
            .fap(rawTag -> PsalmParser.parse(rawTag.textLeft)
                .map(t -> t.nme((psalmType, textLeft) -> {
                    It<DeepType> tit = psalmToDeep(psalmType, docComment);
                    return new PsalmDocTag(rawTag.tagName, tit, textLeft);
                })));
    }

    public static It<DeepType> resolveReturn(PhpDocReturnTag docTag)
    {
        return opt(docTag.getParent())
            .cst(PhpDocComment.class)
            .fap(docComment -> parseDocTags(docComment))
            .flt(psalmTag -> psalmTag.tagName.equals("return"))
            .fap(psalmTag -> psalmTag.tit);
    }

    public static It<DeepType> resolveParam(Function func, String argName)
    {
        return opt(func.getDocComment())
            .fap(docComment -> parseDocTags(docComment))
            .flt(psalmTag ->
                psalmTag.tagName.equals("param") &&
                psalmTag.textLeft.trim()
                    .startsWith("$" + argName))
            .fap(psalmTag -> psalmTag.tit);
    }

    private static class PsalmDocTag {
        final public String tagName;
        final public It<DeepType> tit;
        final public String textLeft;
        private PsalmDocTag(String tagName, It<DeepType> tit, String textLeft) {
            this.tagName = tagName;
            this.tit = tit;
            this.textLeft = textLeft;
        }
    }
}
