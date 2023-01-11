package org.overengineer.inlineproblems.listeners;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.InlineDrawer;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HighlightProblemsListener implements HighlightInfoFilter {
    private final InlineDrawer inlineDrawer;

    HighlightProblemsListener() {
        inlineDrawer = ApplicationManager.getApplication().getService(InlineDrawer.class);
    }

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (file == null || !file.isValid())
            return true;

        ApplicationManager.getApplication().invokeLater(() -> handleAccept(file));
        return true;
    }

    public void handleAccept(PsiFile file) {
        if (file.getVirtualFile() == null)
            return;

        FileEditor editor = FileEditorManager.getInstance(file.getProject()).getSelectedEditor(file.getVirtualFile());
        if (editor == null) {
            return;
        }

        TextEditor textEditor = (TextEditor) editor;
        Document document = textEditor.getEditor().getDocument();
        List<InlineProblem> problems = new ArrayList<>();

        int lineCount = document.getLineCount();
        if (lineCount <= 0) {
            // Can be triggered when a file is deleted -> update with empty list
            inlineDrawer.updateFromListOfNewActiveProblems(problems, file.getProject(), textEditor.getFile().getPath());
        }

        int fileEndOffset = document.getLineEndOffset(lineCount - 1);

        RangeHighlighter[] highlighters = DocumentMarkupModel
                .forDocument(document, file.getProject(), false)
                .getAllHighlighters();

        List<String> problemTextBeginningFilterList = new ArrayList<>(
                Arrays.asList(SettingsState.getInstance().getProblemFilterList().split(";"))
        );

        Arrays.stream(highlighters)
                .map(RangeHighlighter::getErrorStripeTooltip)
                .filter(h -> h instanceof HighlightInfo)
                .map(h -> (HighlightInfo)h)
                .filter(h -> h.getDescription() != null && !h.getDescription().isEmpty())
                .filter(h -> problemTextBeginningFilterList
                        .stream()
                        .noneMatch(p -> h.getDescription().stripLeading().toLowerCase().startsWith(p.stripLeading().toLowerCase())))
                .forEach(h -> {
                    int usedEndOffset = h.getEndOffset();

                    if (fileEndOffset < h.getEndOffset()) {
                        usedEndOffset = fileEndOffset;
                    }

                    int line = document.getLineNumber(usedEndOffset);

                    problems.add(new InlineProblem(
                            line,
                            h.getSeverity().myVal,
                            h.getDescription(),
                            textEditor.getEditor(),
                            textEditor.getFile().getPath(),
                            file.getProject()
                    ));}
                );

        inlineDrawer.updateFromListOfNewActiveProblems(problems, file.getProject(), textEditor.getFile().getPath());
    }
}
