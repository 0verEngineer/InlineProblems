package org.overengineer.inlineproblems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.ProjectManager;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.listeners.HighlightProblemListener;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class DocumentMarkupModelScanner {
    private final ProblemManager problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);

    private final AtomicBoolean isManualScanEnabled = new AtomicBoolean(true);

    private final AtomicInteger frequencyMilliseconds = new AtomicInteger(HighlightProblemListener.MANUAL_SCAN_FREQUENCY_MILLIS);

    private static DocumentMarkupModelScanner instance;

    public static DocumentMarkupModelScanner getInstance() {
        if (instance == null)
            instance = new DocumentMarkupModelScanner();

        return instance;
    }

    private DocumentMarkupModelScanner() {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                if (isManualScanEnabled.get())
                    ApplicationManager.getApplication().invokeLater(() -> {scanForProblemsManually();});

                try {
                    Thread.sleep(frequencyMilliseconds.get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                run();
            }
        });
    }

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

    public void scanForProblemsManuallyInTextEditor(TextEditor textEditor) {
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
                .filter(RangeMarker::isValid)
                .filter(h -> {
                    if (h.getErrorStripeTooltip() instanceof HighlightInfo highlightInfo) {
                        return highlightInfo.getDescription() != null &&
                                !highlightInfo.getDescription().isEmpty() &&
                                problemTextBeginningFilterList.stream()
                                        .noneMatch(f -> highlightInfo.getDescription().stripLeading().toLowerCase().startsWith(f.stripLeading().toLowerCase()));
                    }

                    return false;
                })
                .forEach(h -> {
                    HighlightInfo highlightInfo = (HighlightInfo) h.getErrorStripeTooltip();
                    if (fileEndOffset >= highlightInfo.getStartOffset()) {
                        int line = document.getLineNumber(highlightInfo.getStartOffset());

                        InlineProblem newProblem = new InlineProblem(
                                line,
                                highlightInfo,
                                textEditor,
                                h
                                );

                        problems.add(newProblem);
                    }
                });

        return problems;
    }

    public void setIsManualScanEnabled(boolean isEnabled) {
        isManualScanEnabled.set(isEnabled);
    }

    public void setFrequencyMilliseconds(int newFrequencyMilliseconds) {
        frequencyMilliseconds.set(newFrequencyMilliseconds);
    }
}
