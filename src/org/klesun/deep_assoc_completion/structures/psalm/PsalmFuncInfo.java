package org.klesun.deep_assoc_completion.structures.psalm;

import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.*;

import static org.klesun.lang.Lang.*;

public class PsalmFuncInfo {
    final public L<GenericDef> classGenerics;
    final public L<GenericDef> funcGenerics;
    final public Map<String, IType> params;
    final public Opt<IType> returnType;

    private PsalmFuncInfo(
        L<GenericDef> classGenerics,
        L<GenericDef> funcGenerics,
        Map<String, IType> params,
        Opt<IType> returnType
    ) {
        this.classGenerics = classGenerics;
        this.funcGenerics = funcGenerics;
        this.params = params;
        this.returnType = returnType;
    }

    private static It<RawDocTag> getRawTags(String docCommentText)
    {
        List<RawDocTag> tags = new ArrayList<>();
        Opt<RawDocTag> current = non();
        for (String line: docCommentText.split("\\n")) {
            Opt<L<String>> matchOpt = Tls.regexWithFull("\\s*@([\\w-]+)\\s*(.*)", line, 0);
            if (matchOpt.has()) {
                String tagName = matchOpt.unw().get(1);
                String textLeft = matchOpt.unw().get(2);
                if (current.has()) {
                    tags.add(current.unw());
                }
                current = som(new RawDocTag(tagName, textLeft));
            } else if (current.has()) {
                String tagName = current.unw().tagName;
                String textLeft = current.unw().textLeft + "\n" + line;
                current = som(new RawDocTag(tagName, textLeft));
            }
        }
        if (current.has()) {
            tags.add(current.unw());
        }
        return It(tags);
    }

    private static L<PsalmDocTag> getPsalmTags(PhpDocComment docComment)
    {
        return DocParamRes.getDocCommentText(docComment)
            .fap(txt -> getRawTags(txt))
            .fap(rawTag -> PsalmTypeExprParser.parse(rawTag.textLeft)
                .map(t -> t.nme((psalmType, textLeft) ->
                    new PsalmDocTag(rawTag.tagName, psalmType, textLeft)
                ))).arr();
    }

    private static Opt<GenericDef> assertTplTag(RawDocTag tag)
    {
        return opt(tag)
            .flt(t -> t.tagName.equals("template"))
            .fop(t -> Tls.regex("\\s*(\\w+?)\\s*(?:of\\s+(\\w+))?\\s*", t.textLeft))
            .map(match -> {
                String name = match.get(0);
                Opt<IType> ofType = match.gat(1)
                    .fop(str -> PsalmTypeExprParser.parse(str))
                    .map(t2 -> t2.a);
                return new GenericDef(name, ofType);
            });
    }

    private static L<GenericDef> getGenerics(PhpDocComment docComment)
    {
        return DocParamRes.getDocCommentText(docComment)
            .fap(txt -> getRawTags(txt))
            .fap(t -> assertTplTag(t)).arr();
    }

    public static PsalmFuncInfo parse(PhpDocComment docComment)
    {
        Opt<Function> funcOpt = opt(docComment.getNextSibling()).cst(Function.class);
        Opt<PhpClass> clsOpt = funcOpt.fop(f -> opt(f.getParent())).cst(PhpClass.class);

        L<GenericDef> classGenerics = clsOpt
            .fop(clsPsi -> opt(clsPsi.getDocComment()))
            .fap(doc -> getGenerics(doc)).arr();

        L<PsalmDocTag> psalmTags = getPsalmTags(docComment);
        L<GenericDef> funcGenerics = getGenerics(docComment);

        Map<String, IType> params = new HashMap<>();
        psalmTags
            .flt(t -> t.tagName.equals("param")
                    || t.tagName.equals("psalm-param")
                    || t.tagName.equals("var"))
            .fch(t -> Tls.regex("\\s*\\$(\\w+).*", t.textLeft)
                .map(m -> m.get(0))
                .thn(varName -> params.put(varName, t.psalmType)));

        Opt<IType> returnType = psalmTags
            .flt(t -> t.tagName.equals("return"))
            .map(t -> t.psalmType).fst();

        return new PsalmFuncInfo(classGenerics, funcGenerics, params, returnType);
    }

    public static class GenericDef {
        final public String name;
        final public Opt<IType> ofType;
        public GenericDef(String name, Opt<IType> ofType) {
            this.name = name;
            this.ofType = ofType;
        }
    }

    public static class RawDocTag {
        final public String tagName;
        final public String textLeft;
        public RawDocTag(String tagName, String textLeft) {
            this.tagName = tagName;
            this.textLeft = textLeft;
        }
    }

    public static class PsalmDocTag {
        final public String tagName;
        final public IType psalmType;
        final public String textLeft;
        public PsalmDocTag(String tagName, IType psalmType, String textLeft) {
            this.tagName = tagName;
            this.psalmType = psalmType;
            this.textLeft = textLeft;
        }
    }
}
