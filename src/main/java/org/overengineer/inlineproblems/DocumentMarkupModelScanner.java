package org.overengineer.inlineproblems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.ProjectManager;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DocumentMarkupModelScanner {
    private final ProblemManager problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);

    private static DocumentMarkupModelScanner instance;

    public static DocumentMarkupModelScanner getInstance() {
        if (instance == null)
            instance = new DocumentMarkupModelScanner();

        return instance;
    }

    private DocumentMarkupModelScanner() {}

    public void scanForProblemsManually() {
        ProjectManager projectManager = ProjectManager.getInstanceIfCreated();

        if (projectManager != null) {
            List<InlineProblem> problems = new ArrayList<>();

            for (var project : projectManager.getOpenProjects()) {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                for (var editor : fileEditorManager.getAllEditors()) {
                    if (editor instanceof TextEditor) {
                        problems.addAll(getProblemsInEditor((TextEditor) editor));
                    }
                }
            }

            problemManager.updateFromNewActiveProblems(problems);
        }
    }

    public void scanForProblemsManuallyInEditor(TextEditor textEditor) {
        List<InlineProblem> problems = getProblemsInEditor(textEditor);
        problemManager.updateFromNewActiveProblemsForProjectAndFile(
                problems,
                textEditor.getEditor().getProject(),
                textEditor.getFile().getPath()
        );
    }

    private List<InlineProblem> getProblemsInEditor(TextEditor textEditor) {
        Editor editor = textEditor.getEditor();
        Document document = editor.getDocument();
        List<InlineProblem> problems = new ArrayList<>();

        int lineCount = document.getLineCount();
        if (lineCount <= 0) {
            return problems;
        }

        int fileEndOffset = document.getLineEndOffset(lineCount - 1);

        RangeHighlighter[] highlighters = DocumentMarkupModel
                .forDocument(document, editor.getProject(), false)
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
                    if (fileEndOffset > h.getEndOffset()) {
                        int line = document.getLineNumber(h.getEndOffset());

                        InlineProblem newProblem = new InlineProblem(
                                line,
                                h.getSeverity().myVal,
                                h.getDescription(),
                                textEditor.getEditor(),
                                textEditor.getFile().getPath(),
                                editor.getProject()
                        );

                        problems.add(newProblem);
                    }
                });

        return problems;
    }
}
