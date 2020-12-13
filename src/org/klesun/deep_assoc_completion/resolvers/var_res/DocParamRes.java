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
    final public static String EXPR_POSTFIX = "\n;})();";

    public DocParamRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private static class PropDoc
    {
        final String docTag;
        final String type;
        final String name;
        String desc;

        PropDoc(String docTag, String type, String name, String desc) {
            this.docTag = docTag;
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
     *
     *  following is WordPress format, should also be supported
     *
     * @param string|array $query {
     *     Optional. Array or string of Query parameters.
     *
     *     @type int          $attachment_id           Attachment post ID. Used for 'attachment' post_type.
     *     @type int|string   $author                  Author ID, or comma-separated list of IDs.
     *     @type string       $author_name             User 'user_nicename'.
     *     @type array        $author__in              An array of author IDs to query from.
     *     @type array        $author__not_in          An array of author IDs not to query from.
     * }
     */
    private static L<PropDoc> parseWordpressDoc(String body)
    {
        Mutable<Integer> depth = new Mutable<>(0);
        L<PropDoc> props = L();
        for (String line: body.split("\n")) {
            if (depth.get() == 0) {
                Tls.regex("\\s*@(property|var|param|type)\\s+([A-Za-z\\d\\\\_\\|]+)\\s+\\$?(\\w+)(.*)", line)
                    .map(matches -> new PropDoc(matches.get(0), matches.get(1), matches.get(2), matches.get(3)))
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

    private static Opt<DeepType> wpDescToType(PropDoc parentProp, PsiElement decl)
    {
        L<PropDoc> props = parseWordpressDoc(parentProp.desc);
        List<Key> assocKeys = new ArrayList<>();
        List<Key> propKeys = new ArrayList<>();
        if (props.size() > 0) {
            props.fch(prop -> {
                PhpType fqnType = new PhpType().add(prop.type);
                L<DeepType> valts = wpDescToType(prop, decl).arr()
                    .cct(list(new DeepType(decl, fqnType, false)));
                Granted<Mt> getType = Granted(new Mt(valts));
                Key keyEntry = new Key(prop.name, decl)
                    .addType(getType, fqnType);
                if (parentProp.type.contains("array")) {
                    assocKeys.add(keyEntry);
                } else {
                    propKeys.add(keyEntry);
                }
                String desc = prop.desc.trim();
                if (desc.length() > 0 && !desc.startsWith("{")) {
                    keyEntry.addComments(som(desc.replaceAll("\\s+", " ")));
                }
            });

            PhpType pst = PhpType.builder()
                .add(parentProp.type).build();
            return opt(new Build(decl, pst)
                .keys(L(assocKeys))
                .props(L(propKeys))
                .get());
        } else {
            return opt(null);
        }
    }

    public static IIt<DeepType> parseExpression(String expr, Project project, IExprCtx docCtx)
    {
        // adding "$arg = " so anonymous functions were parsed as expressions
        expr = EXPR_PREFIX + expr + EXPR_POSTFIX;
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr);
        return opt(psiFile.getFirstChild())
            .cst(GroupStatement.class)
            .map(gr -> gr.getFirstPsiChild())
            .cst(Statement.class)
            .map(st -> st.getFirstChild())
            .cst(PhpExpression.class).arr()
            .rap(ex -> docCtx.findExprType(ex));
    }

    public IIt<DeepType> parseEqExpression(String eqExpr, PsiElement sourcePsi)
    {
        IExprCtx docCtx = ctx.subCtxDoc(sourcePsi);

        /** @param $data['a']['b'] = 123 */
        List<String> assKeys = new ArrayList<>();
        Opt<L<String>> keyMatchOpt;
        while ((keyMatchOpt = Tls.regex("\\s*\\[['\"]([^'\"]*?)['\"]\\](.*)", eqExpr)).has()) {
            assKeys.add(keyMatchOpt.unw().get(0));
            eqExpr = keyMatchOpt.unw().get(1);
        }

        IIt<DeepType> valTit = Tls.regex("^\\s*=\\s*(.+)$", eqExpr)
            .fop(matches -> matches.gat(0))
            .rap(expr -> parseExpression(expr, sourcePsi.getProject(), docCtx));
        Mt valMt = Mt.reuse(valTit);
        for (int i = assKeys.size() - 1; i >= 0; --i) {
            Mt valMtF = valMt;
            Key keyEntry = new Key(assKeys.get(i), sourcePsi)
                .addType(() -> valMtF);
            valMt = new Build(sourcePsi, PhpType.ARRAY)
                .keys(som(keyEntry)).get().mt();
        }
        return valMt.types;
    }

    private IIt<DeepType> parseDoc(PhpDocTag doc)
    {
        String tagValue = doc.getTagValue();
        return IResolvedIt.rnc(
            parseEqExpression(tagValue, doc),
            DeepAssocApi.inst().parseDoc(tagValue, doc),
            opt(doc.getParent())
                .fop(toCast(PhpDocComment.class))
                .fop(full -> getDocCommentText(full))
                .rap(clean -> parseWordpressDoc(clean))
                .flt(prop -> nameMatches(prop, doc))
                .fop(prop -> wpDescToType(prop, doc))
                .arr()
        );
    }

    public IIt<DeepType> resolve(PhpDocTag doc)
    {
        return parseDoc(doc);
    }
}
