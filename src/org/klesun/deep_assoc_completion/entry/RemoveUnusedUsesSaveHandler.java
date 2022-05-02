package org.klesun.deep_assoc_completion.entry;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.inspections.PhpUnusedAliasInspection;
import com.jetbrains.php.lang.psi.elements.PhpUse;
import com.jetbrains.php.lang.psi.elements.impl.PhpUseListImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.lang.It;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;

public class RemoveUnusedUsesSaveHandler implements FileDocumentManagerListener
{
    @Override
    public void beforeAllDocumentsSaving() {
        // do nothing ^_^
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (false) {
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
                if (psiFile == null) continue;

                // TODO: check if file has syntax errors! It may report false unused imports otherwise
                // @see https://intellij-support.jetbrains.com/hc/en-us/community/posts/206778625/comments/206721629
                // DaemonCodeAnalyzerImpl codeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(myProject);
                // List infos = codeAnalyzer.runMainPasses(psiFile, document,
                // progress);

                PhpUnusedAliasInspection inspection = new PhpUnusedAliasInspection();
                InspectionManager manager = InspectionManager.getInstance(psiFile.getProject());
                It<ProblemDescriptor> checked = It(inspection.processFile(psiFile, manager))
                    .flt(problem ->
                        problem.getDescriptionTemplate().contains("never used") &&
                        !problem.getDescriptionTemplate().contains("not necessary")); // same namespace, keep

                It<T2<Integer, Integer>> unuseRanges = checked
                    .map(problem -> problem.getPsiElement())
                    .fop(toCast(PhpUse.class))
                    .fop(psi -> Tls.findParent(psi, PhpUseListImpl.class, a -> true))
                    .flt(lst -> It(lst.getChildren())
                        .fop(toCast(PhpUse.class))
                        .arr().size() == 1) // just one name on this use line
                    .map(unuse -> unuse.getTextRange())
                    .map(r -> T2(r.getStartOffset(), r.getEndOffset() + 1))
                    .flt(p -> document.getText(new TextRange(p.a, p.b)).endsWith("\n"))
                    ;

                ApplicationManager.getApplication().runWriteAction(() ->
                    CommandProcessor.getInstance().runUndoTransparentAction(() ->
                        unuseRanges.arr().srt(r -> -r.a) // start from the end to not mess up range indexes
                            .forEach(range -> document.deleteString(range.a, range.b))));
            }
        }
    }

    @Override
    public void beforeFileContentReload(VirtualFile virtualFile, @NotNull Document document) {
        // do nothing ^_^
    }

    @Override
    public void fileWithNoDocumentChanged(@NotNull VirtualFile virtualFile) {
        // do nothing ^_^
    }

    @Override
    public void fileContentReloaded(@NotNull VirtualFile virtualFile, @NotNull Document document) {
        // do nothing ^_^
    }

    @Override
    public void fileContentLoaded(@NotNull VirtualFile virtualFile, @NotNull Document document) {
        // do nothing ^_^
    }

    @Override
    public void unsavedDocumentsDropped() {
        // do nothing ^_^
    }
}
