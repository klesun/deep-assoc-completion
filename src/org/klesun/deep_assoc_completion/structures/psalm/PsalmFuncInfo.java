package org.klesun.deep_assoc_completion.structures.psalm;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.It;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.ArrayList;
import java.util.List;

import static org.klesun.lang.Lang.*;

public class PsalmFuncInfo {
    final public PhpDocComment psi;
    final public L<GenericDef> classGenerics;
    final public L<GenericDef> funcGenerics;
    final public L<ArgDef> params;
    final public Opt<IType> returnType;

    private PsalmFuncInfo(
        PhpDocComment psi,
        L<GenericDef> classGenerics,
        L<GenericDef> funcGenerics,
        L<ArgDef> params,
        Opt<IType> returnType
    ) {
        this.psi = psi;
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
            .fop(t -> Tls.regex("\\s*(\\w+)(?:\\s+(?:of|as)\\s+(\\w+))?[\\s\\S]*", t.textLeft))
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
        Opt<PsiElement> clsMemOpt = opt(docComment.getNextPsiSibling());
        Opt<Function> funcOpt = clsMemOpt.cst(Function.class);
        Opt<PhpClass> clsOpt = clsMemOpt.fop(f -> opt(f.getParent())).cst(PhpClass.class);

        L<GenericDef> classGenerics = clsOpt
            .fop(clsPsi -> opt(clsPsi.getDocComment()))
            .fap(doc -> getGenerics(doc)).arr();

        L<PsalmDocTag> psalmTags = getPsalmTags(docComment);
        L<GenericDef> funcGenerics = getGenerics(docComment);

        L<Parameter> argPsis = funcOpt.fap(f -> It(f.getParameters())).arr();
        L<ArgDef> params = psalmTags
            .flt(t -> t.tagName.equals("param")
                    || t.tagName.equals("psalm-param")
                    || t.tagName.equals("var"))
            .map(t -> {
                String varName = Tls.regex("\\s*\\$(\\w+).*", t.textLeft)
                    .map(m -> m.get(0)).def("");
                Opt<Integer> order = argPsis.fap((psi, i) -> {
                    return psi.getName().equals(varName) ? som(i) : non();
                }).fst();
                return new ArgDef(varName, order, som(t.psalmType));
            }).arr();

        Opt<IType> returnType = psalmTags
            .flt(t -> t.tagName.equals("return"))
            .map(t -> t.psalmType).fst();

        return new PsalmFuncInfo(docComment, classGenerics, funcGenerics, params, returnType);
    }

    public static class ArgDef {
        final public String name;
        final public Opt<Integer> order;
        final public Opt<IType> psalmType;
        public ArgDef(String name, Opt<Integer> order, Opt<IType> psalmType) {
            this.name = name;
            this.order = order;
            this.psalmType = psalmType;
        }
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
