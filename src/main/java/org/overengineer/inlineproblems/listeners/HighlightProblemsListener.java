package org.overengineer.inlineproblems.listeners;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
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
    private final InlineDrawer inlineDrawer = new InlineDrawer();

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (file == null || !file.isValid())
            return true;

        ApplicationManager.getApplication().invokeLater(() -> handleAccept(file));
        return true;
    }

    public void handleAccept(PsiFile file) {
        Editor editor = FileEditorManager.getInstance(file.getProject()).getSelectedTextEditor();

        if (editor == null || !editor.getEditorKind().name().equalsIgnoreCase("main_editor"))
            return;

        var highlighters = DocumentMarkupModel
                .forDocument(editor.getDocument(), file.getProject(), false)
                .getAllHighlighters();

        List<InlineProblem> problems = new ArrayList<>();

        List<String> problemTextBeginningFilterList = new ArrayList<>();
        for (var filterItem : SettingsState.getInstance().getProblemFilterList().split(";")) {
            problemTextBeginningFilterList.add(filterItem);
        }

        int lineCount = editor.getDocument().getLineCount();
        int fileEndOffset = editor.getDocument().getLineEndOffset(lineCount - 1);

        Arrays.stream(highlighters)
                .map(RangeHighlighter::getErrorStripeTooltip)
                .filter(h -> h instanceof HighlightInfo)
                .map(h -> (HighlightInfo)h)
                .filter(h -> h.getDescription() != null)
                .filter(h -> problemTextBeginningFilterList
                        .stream()
                        .noneMatch(p -> h.getText().toLowerCase().startsWith(p.toLowerCase())))
                .forEach(h -> {
                    int usedEndOffset = h.getEndOffset();
                    int usedStartOffset = h.getStartOffset();

                    if (fileEndOffset < h.getEndOffset()) {
                        usedEndOffset = fileEndOffset;
                    }

                    if (fileEndOffset < h.getStartOffset()) {
                        usedStartOffset = fileEndOffset;
                    }

                    int line = editor.getDocument().getLineNumber(usedEndOffset);

                    problems.add(new InlineProblem(
                            line,
                            usedStartOffset,
                            usedEndOffset,
                            h.getSeverity().myVal,
                            h.getDescription(),
                            editor,
                            file.getVirtualFile().getPath(),
                            file.getProject()
                    ));}
                );

        inlineDrawer.updateFromListOfNewActiveProblems(problems, file.getProject());
    }
}
