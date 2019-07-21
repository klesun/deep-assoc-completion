package org.klesun.deep_assoc_completion.resolvers.var_res;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.Statement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.other_plugin_integration.DeepAssocApi;
import org.klesun.deep_assoc_completion.structures.Build;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Key;
import org.klesun.lang.*;

import java.util.ArrayList;
import java.util.List;

public class DocParamRes extends Lang
{
    private IExprCtx ctx;

    // wrapping in a function so that global variables did not affect result (had a global variable
    // called $i with a definite value, and when I used $i to define _any_ key, I got no completion)
    final public static String EXPR_PREFIX = "<?php\n(function(){return ";
    final public static String EXPR_POSTFIX = ";})();";

    public DocParamRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private static class PropDoc
    {
        final String type;
        final String name;
        String desc;

        PropDoc(String type, String name, String desc) {
            this.type = type.startsWith("\\") ? type : "\\" + type;
            this.name = name;
            this.desc = desc;
        }
    }

    public static Opt<String> getDocCommentText(PhpDocComment docComment)
    {
        Opt<String> result = Tls.regex("\\s*\\/\\*{2}\\s*(.*?)\\s*\\*\\/", docComment.getText())
            .fop(matches -> matches.gat(0))
            .fap(starred -> It(starred.split("\n")))
            .fop(line -> Tls.regex("\\s*\\*?(.*)", line))
            .fop(matches -> matches.gat(0))
            .wap(cleanLines -> opt(Tls.implode("\n", cleanLines)));
        return result;
    }

    private static boolean nameMatches(PropDoc propDoc, PhpDocTag docTag)
    {
        return Tls.cast(PhpDocParamTag.class, docTag)
            .map(pt -> pt.getVarName())
            .flt(nme -> nme.equals(propDoc.name))
            .has();
    }

    /**
     * @var stdClass $row {
     *      @property int id some description
     *      @property string name some description
     *      @property string childPurchase {
     *          @property int id some description
     *          @property float price
     *      }
     * }
     */
    private static L<PropDoc> parseMiraObjPropertyDoc(String body)
    {
        Mutable<Integer> depth = new Mutable<>(0);
        L<PropDoc> props = L();
        for (String line: body.split("\n")) {
            if (depth.get() == 0) {
                Tls.regex("\\s*@(property|var|param)\\s+([A-Za-z\\d\\\\_]+)\\s+\\$?(\\w+)(.*)", line)
                    .map(matches -> new PropDoc(matches.get(1), matches.get(2), matches.get(3)))
                    .thn(prop -> {
                        props.add(prop);
                        if (Tls.regex(".*\\{\\s*", prop.desc).has()) {
                            depth.set(depth.get() + 1);
                        }
                    })
                    .els(() -> props.lst().thn(prop -> prop.desc += "\n" + line));
            } else {
                props.lst().thn(prop -> prop.desc += "\n" + line);
                if (Tls.regex("\\s*\\}\\s*", line).has()) {
                    depth.set(depth.get() - 1);
                }
            }
        }
        return props;
    }

    private static Opt<DeepType> propDescToType(String propDesc, PsiElement decl)
    {
        L<PropDoc> props = parseMiraObjPropertyDoc(propDesc);
        if (props.size() > 0) {
            DeepType type = new DeepType(decl, PhpType.OBJECT);
            props.fch(prop -> {
                PhpType fqnType = new PhpType().add(prop.type);
                type.addProp(prop.name, decl)
                    .addType(
                        () -> propDescToType(prop.desc, decl).itr()
                            .cct(list(new DeepType(decl, fqnType, false)))
                            .wap(Mt::new),
                        fqnType
                    );
            });

            return opt(type);
        } else {
            return opt(null);
        }
    }

    public static It<DeepType> parseExpression(String expr, Project project, IExprCtx docCtx)
    {
        // adding "$arg = " so anonymous functions were parsed as expressions
        expr = EXPR_PREFIX + expr + EXPR_POSTFIX;
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr);
        return opt(psiFile.getFirstChild())
            .fop(toCast(GroupStatement.class))
            .map(gr -> gr.getFirstPsiChild())
            .fop(toCast(Statement.class))
            .map(st -> st.getFirstChild())
            .fop(toCast(PhpExpression.class))
            .fap(ex -> docCtx.findExprType(ex));
    }

    public It<DeepType> parseEqExpression(String eqExpr, PsiElement sourcePsi)
    {
        IExprCtx docCtx = ctx.subCtxDoc(sourcePsi);

        /** @param $data['a']['b'] = 123 */
        List<String> assKeys = new ArrayList<>();
        Opt<L<String>> keyMatchOpt;
        while ((keyMatchOpt = Tls.regex("\\s*\\[['\"]([^'\"]*?)['\"]\\](.*)", eqExpr)).has()) {
            assKeys.add(keyMatchOpt.unw().get(0));
            eqExpr = keyMatchOpt.unw().get(1);
        }

        It<DeepType> valTit = Tls.regex("^\\s*=\\s*(.+)$", eqExpr)
            .fop(matches -> matches.gat(0))
            .fap(expr -> parseExpression(expr, sourcePsi.getProject(), docCtx));
        Mt valMt = new Mt(valTit);
        for (int i = assKeys.size() - 1; i >= 0; --i) {
            Mt valMtF = valMt;
            Key keyEntry = new Key(assKeys.get(i), sourcePsi)
                .addType(() -> valMtF);
            valMt = new Build(sourcePsi, PhpType.ARRAY)
                .keys(som(keyEntry)).get().mt();
        }
        return valMt.types.itr();
    }

    private It<DeepType> parseDoc(PhpDocTag doc)
    {
        String tagValue = doc.getTagValue();
        return It.cnc(
            parseEqExpression(tagValue, doc),
            DeepAssocApi.inst().parseDoc(tagValue, doc),
            opt(doc.getParent())
                .fop(toCast(PhpDocComment.class))
                .fop(full -> getDocCommentText(full))
                .fap(clean -> parseMiraObjPropertyDoc(clean))
                .flt(prop -> nameMatches(prop, doc))
                .fop(prop -> propDescToType(prop.desc, doc))
        );
    }

    public It<DeepType> resolve(PhpDocTag doc)
    {
        return parseDoc(doc);
    }
}
