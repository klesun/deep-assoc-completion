package org.klesun.deep_assoc_completion.entry;

import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.jetbrains.php.lang.psi.PhpElementType;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ForeachImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpUseListImpl;
import com.jetbrains.php.lang.psi.elements.impl.StatementImpl;
import org.apache.commons.lang.StringEscapeUtils;
import org.klesun.deep_assoc_completion.resolvers.VarRes;
import org.klesun.lang.It;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.Iterator;

import static org.klesun.lang.Lang.*;

public class TranspileToNodeJs extends AnAction
{
    private static It<PsiElement> removeClsMods(It<PsiElement> clsParts)
    {
        //for (PsiElement psi: clsParts) {
            //System.out.println("zhopa type " + psi.getNode().getElementType().getClass() + " " + psi.getNode().getElementType());
        //}
        return clsParts
            .flt(part -> opt(part.getNode().getElementType())
                .cst(PhpElementType.class)
                .all(elt -> {
                    String debugName = elt.toString();
                    return !list("abstract", "final").contains(debugName);
                }))
                ;
    }

    private static It<PsiElement> getChildrenWithLeaf(PsiElement psi)
    {
        PsiElement next = psi.getFirstChild();
        return It(() -> new Iterator<PsiElement>() {
            private PsiElement nextProp = next;
            public boolean hasNext() {
                return nextProp != null;
            }
            public PsiElement next() {
                PsiElement current = nextProp;
                nextProp = nextProp.getNextSibling();
                return current;
            }
        });
    }

    private static Opt<String> transpileImport(PhpUseListImpl lst)
    {
        return It(lst.getDeclarations())
            .fal(usePsi -> opt(usePsi.getTargetReference())
            .map(pathPsi -> {
                String path = pathPsi.getText();
                String alias = opt(usePsi.getAliasName()).def(pathPsi.getName());
                return "const " + alias + " = require('" + path.replace("\\", "/") + "');";
            }))
            .map(vals -> vals.itr().str("\n"));
    }

    private static String getIndent(PsiElement psi)
    {
        return opt(psi.getPrevSibling())
            .cst(PsiWhiteSpaceImpl.class)
            .fop(ws -> Tls.regex("^(.*\\n|)(\\s*?)$", ws.getText()))
            .fop(ma -> ma.gat(1))
            .def("");
    }

    private static String getOutdent(PsiElement psi)
    {
        return opt(psi.getNextSibling())
            .cst(PsiWhiteSpaceImpl.class)
            .fop(ws -> Tls.regex("^(\\s*\\n).*$", ws.getText()))
            .fop(ma -> ma.gat(0))
            .def("");
    }

    private static String transpileMethod(Method typed)
    {
        It<PsiElement> stats = Tls.findChildren(typed, GroupStatement.class)
            .fst().fap(gr -> It(gr.getStatements()));
        It<PsiElement> args = It(typed.getParameters());
        String name = typed.getName();
        name = "__construct".equals(name) ? "constructor" : name;
        return (typed.isStatic() ? "static " : "") + name + "("
            + args.map(st -> transpilePsi(st)).str(", ")
            + ") {\n"
            + stats.map(st -> getIndent(st) + transpilePsi(st)).str("\n")
            + "\n" + getIndent(typed) + "}";
    }

    private static Opt<String> transpileArray(ArrayCreationExpression typed)
    {
        return It(typed.getHashElements())
            .fal(hash -> opt(hash.getKey())
                .fop(key -> opt(hash.getValue())
                    .map(val -> getIndent(key)
                        + (key instanceof StringLiteralExpression
                            ? key.getText() : "[" + transpilePsi(key) + "]")
                        + ": " + transpilePsi(val)
                        + (opt(hash.getNextPsiSibling()).any(next -> next.getText().equals("]")) ? "" : ",")
                        + getOutdent(val))))
            .flt(parts -> parts.size() > 0)
            .map(parts -> "{" + parts.itr().str("") + "}");
    }

    private static String transpileCatch(Catch typed)
    {
        Opt<Catch> prevCatch = Tls.findPrevSibling(typed, Catch.class);
        if (prevCatch.has()) {
            return ""; // you can't have 2 catch-es in javascript
        }
        It<PsiElement> stats = Tls.findChildren(typed, GroupStatement.class)
            .fst().fap(gr -> It(gr.getStatements()));
        return "catch (" + typed.getException().getText() + ") {\n"
            + stats.map(st -> getIndent(st) + transpilePsi(st)).str("\n")
            + "}";
    }

    private static Opt<String> transpileForeach(ForeachImpl typed)
    {
        It<PsiElement> stats = Tls.findChildren(typed, GroupStatement.class)
            .fst().fap(gr -> It(gr.getStatements()));
        return VarRes.parseForeach(typed)
            .fop(tup -> tup.nme((arr, keyOpt, valOpt, tuple) -> {
                Opt<String> valuePartOpt = Opt.fst(
                    () -> valOpt.map(vari -> vari.getText()),
                    () -> som("[" + tuple.map(vari -> vari.getText()).str(", ") + "]")
                );
                return valuePartOpt.map(valuePart -> {
                    String arrPart = transpilePsi(arr);
                    String content = keyOpt
                        .map(key -> "let [" + key.getText() + ", " + valuePart + "] of Object.entries(" + arrPart + ")")
                        .def("let " + valuePart + " of " + arrPart);
                    return "for (" + content + ") {\n"
                        + stats.map(st -> getIndent(st) + transpilePsi(st)).str("\n")
                        + "}";
                });
            }));
    }

    private static String transpilePsi(PsiElement psi)
    {
        // TODO: add whitespace generally here
        // TODO: $arr[] = $val; -> $arr.push($val)
        // TODO: unset -> delete
        // TODO: process whole directories, not just one file
        // TODO: do not add coma after last object element if there weren't any in original
        // TODO: if ($zhopa = getZhopa()) {...} -> let $zhopa; if ($zhopa = getZhopa()) {...}
        // TODO: class constants
        // TODO: put properties in constructor - node does not allow properties directly in class body
        // TODO: do not add let in +=
        Iterable<String> result = It.frs(() -> It.non()
            , () -> Tls.cast(LeafPsiElement.class, psi)
                .map(leaf ->
                    leaf.getText().equals("<?php") ? "" :
                    leaf.getText().equals("(int)") ? "+" :
                    leaf.getText().equals(".=") ? "+=" :
                    leaf.getText().equals("?:") ? "||" :
                    leaf.getText().equals("??") ? "||" :
                    leaf.getText().equals("self") ? "this" :
                    leaf.getText().equals("static") ? "this" :
                    leaf.getText().equals("namespace") ? "// namespace" :
                    leaf.getText().equals("elseif") ? "else if" :
                    leaf.getText())
            , () -> Tls.cast(PhpClass.class, psi)
                .map(cls -> getChildrenWithLeaf(cls))
                .fap(parts -> removeClsMods(parts)
                    .map(part -> transpilePsi(part)))
            , () -> Tls.cast(PhpUseListImpl.class, psi)
                .fap(typed -> transpileImport(typed))
            , () -> Tls.cast(PhpModifierList.class, psi)
                .map(typed -> "")
            , () -> Tls.cast(Method.class, psi)
                .map(typed -> transpileMethod(typed))
            , () -> Tls.cast(Parameter.class, psi)
                .map(typed -> (typed.getText().startsWith("...") ? "..." : "") + "$" + typed.getName())
            , () -> Tls.cast(Variable.class, psi)
                .map(typed -> {
                    Boolean isDeclSt = opt(typed.getParent())
                        .cst(AssignmentExpression.class)
                        .flt(ass -> typed.equals(ass.getVariable()))
                        .fop(ass -> opt(ass.getParent()))
                        .any(st -> st.getClass().equals(StatementImpl.class));
                    String varName = typed.getText().equals("$this") ? "this" : typed.getText();
                    return (isDeclSt ? "let " : "") + varName;
                })
            , () -> Tls.cast(FieldReference.class, psi)
                .map(typed -> transpilePsi(typed.getClassReference()) + ".$" + typed.getName())
            , () -> Tls.cast(MethodReference.class, psi)
                .map(typed -> transpilePsi(typed.getClassReference()) + "." + typed.getName() + "(" + It(typed.getParameters()).map(arg -> transpilePsi(arg)).str(", ") + ")")
            , () -> Tls.cast(ArrayCreationExpression.class, psi).itr()
                .fap(typed -> transpileArray(typed))
            , () -> Tls.cast(Catch.class, psi).itr()
                .map(typed -> transpileCatch(typed))
            , () -> Tls.cast(ForeachImpl.class, psi).itr()
                .fap(typed -> transpileForeach(typed))
            , () -> Tls.cast(ConcatenationExpression.class, psi)
                .map(typed -> transpilePsi(typed.getLeftOperand()) + " + "
                            + transpilePsi(typed.getRightOperand()))
            , () -> Tls.cast(ClassConstantReference.class, psi)
                .map(typed -> transpilePsi(typed.getClassReference()) + '.' + typed.getName())
            , () -> Tls.cast(StringLiteralExpression.class, psi)
                .map(typed -> {
                    if (!typed.isSingleQuote()) {
                        return typed.getText();
                    } else {
                        String content = typed.getContents();
                        return Tls.regex("\\/(.+)\\/([a-z]{0,3})", content)
                            .map(m -> "/" + m.get(0) + "/" + m.get(1)) // '/\s*/i' -> /\s*/i
                            .def("'" + StringEscapeUtils.escapeJavaScript(typed.getContents()) + "'");
                    }
                })
            , () -> Tls.cast(FunctionReference.class, psi)
                .fap(call -> {
                    PsiElement[] args = call.getParameters();
                    if ("preg_match".equals(call.getName()) && args.length > 2) {
                        String matchesVar = args[2].getText();
                        return som(matchesVar + " = " + call.getName() + "(" + It(call.getParameters()).map(arg -> transpilePsi(arg)).str(", ") + ")");
                    } else {
                        return non();
                    }
                })
        );
        return It(result).def(getChildrenWithLeaf(psi)
            .map(c -> transpilePsi(c))).str("");
    }

    private void openAsScratchFile(String text, AnActionEvent e)
    {
        opt(e.getData(LangDataKeys.PROJECT)).thn(project -> {
            Language lang = L(Language.findInstancesByMimeType("text/javascript")).fst()
                .def(PlainTextLanguage.INSTANCE);
            VirtualFile file = ScratchRootType.getInstance().createScratchFile(
                project, "transpiled.js", lang,
                text, ScratchFileService.Option.create_new_always
            );
            if (file != null) {
                PsiNavigationSupport.getInstance().createNavigatable(project, file, 0).navigate(true);
                PsiFile nullPsiFile = PsiManager.getInstance(project).findFile(file);
                opt(e.getData(LangDataKeys.IDE_VIEW))
                    .thn(ideView -> opt(nullPsiFile)
                        .thn(psiFile -> ideView.selectElement(psiFile)));
            }
        });
    }

    @Override
    public void actionPerformed(AnActionEvent e)
    {
        String output = opt(e.getData(LangDataKeys.PSI_FILE))
            .map(psiFile -> It(psiFile.getChildren()).map(psi -> transpilePsi(psi)))
            .map(psiTexts -> Tls.implode("", psiTexts))
            .def("Error: could not retrieve current file");
        System.out.println(output);
        openAsScratchFile(output, e);
    }
}
