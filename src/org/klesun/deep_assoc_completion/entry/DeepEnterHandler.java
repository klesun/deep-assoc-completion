package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.opt;

public class DeepEnterHandler implements EnterHandlerDelegate {
    @Override
    public Result preprocessEnter(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull Ref<Integer> ref, @NotNull Ref<Integer> ref1, @NotNull DataContext dataContext, @Nullable EditorActionHandler editorActionHandler) {
        CaretModel caret = editor.getCaretModel();
        DocWrapper docWr = new DocWrapper(editor.getDocument());
        int pos = caret.getOffset();

        String ch = docWr.sub(pos - 1, pos);
        String nextCh = docWr.sub(pos, pos + 1);

        return opt(PsiTreeUtil.findElementOfClassAtOffset(psiFile, pos - 1, PhpDocTag.class, false))
            .flt(tag -> tag.getTagValue().matches("\\s*=.*"))
            .flt(tag -> ch.equals("["))
            .fop(tag -> Tls.regex(
                ".*\\n(\\s*\\*\\s*).*",
                docWr.sub(pos - 100, pos)
            ).fop(match -> match.gat(0))
                .map(baseIndent -> {
                    String insertion = "\n" + baseIndent + "    ";
                    docWr.doc.insertString(pos, insertion);
                    int newPos = pos + insertion.length();
                    caret.moveToOffset(newPos);
                    if (nextCh.equals("]")) {
                        docWr.doc.insertString(newPos, "\n" + baseIndent);
                    } else if (tag.getTagValue().matches(".*\\[\\s*")) {
                        docWr.doc.insertString(newPos, "\n" + baseIndent + "]");
                    }
                    return Result.Stop;
                }))
            .def(Result.Continue);
    }

    @Override
    public Result postProcessEnter(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull DataContext dataContext) {
        return Result.Continue;
    }

    /**
     * a convenient wrapper for the Document to deal with
     * text range bounds and more simple signatures
     */
    static class DocWrapper
    {
        final public Document doc;

        public DocWrapper(Document doc) {
            this.doc = doc;
        }
        /** @param end - exclusive */
        public String sub(int start, int end) {
            start = Math.min(doc.getTextLength(), Math.max(0, start));
            end = Math.min(doc.getTextLength(), Math.max(0, end));
            return doc.getText(new TextRange(start, end));
        }
    }
}
