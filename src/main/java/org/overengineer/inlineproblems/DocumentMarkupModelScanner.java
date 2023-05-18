package org.overengineer.inlineproblems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.listeners.HighlightProblemListener;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class DocumentMarkupModelScanner {
    private final ProblemManager problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);

    private final Logger logger = Logger.getInstance(DocumentMarkupModelScanner.class);

    private int delayMilliseconds = HighlightProblemListener.ADDITIONAL_MANUAL_SCAN_DELAY_MILLIS;

    private static DocumentMarkupModelScanner instance;

    private ScheduledFuture<?> scheduledFuture;

    public static final String NAME = "ManualScanner";

    public static DocumentMarkupModelScanner getInstance() {
        if (instance == null)
            instance = new DocumentMarkupModelScanner();

        return instance;
    }

    private DocumentMarkupModelScanner() {
        SettingsState settingsState = SettingsState.getInstance();
        if (settingsState.getEnabledListener() == Listener.MANUAL_SCANNING) {
            delayMilliseconds = settingsState.getManualScannerDelay();
        }

        createAndStartScheduledFuture();
    }

    public void scanForProblemsManually() {
        ProjectManager projectManager = ProjectManager.getInstanceIfCreated();

        if (projectManager != null) {
            List<InlineProblem> problems = new ArrayList<>();

            for (var project : projectManager.getOpenProjects()) {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                for (var editor : fileEditorManager.getAllEditors()) {
                    if (editor instanceof TextEditor) {
                        var textEditor = (TextEditor) editor;
                        if (textEditor.getFile() == null) {
                            continue;
                        }
                        problems.addAll(getProblemsInEditor(textEditor));
                    }
                }
            }

            problemManager.updateFromNewActiveProblems(problems);
        }
    }

    public void scanForProblemsManuallyInTextEditor(TextEditor textEditor) {
        if (textEditor.getFile() == null) {
            return;
        }

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
                .filter(h -> {
                    if (h.isValid() && h.getErrorStripeTooltip() instanceof HighlightInfo) {
                        HighlightInfo highlightInfo = (HighlightInfo) h.getErrorStripeTooltip();
                        return highlightInfo.getDescription() != null &&
                                !highlightInfo.getDescription().isEmpty() &&
                                problemTextBeginningFilterList.stream()
                                        .noneMatch(f -> highlightInfo.getDescription().stripLeading().toLowerCase().startsWith(f.toLowerCase())) &&
                                fileEndOffset >= highlightInfo.getStartOffset()
                        ;
                    }

                    return false;
                })
                .forEach(h -> {
                    HighlightInfo highlightInfo = (HighlightInfo) h.getErrorStripeTooltip();

                    InlineProblem newProblem = new InlineProblem(
                            document.getLineNumber(highlightInfo.getStartOffset()),
                            textEditor.getFile().getPath(),
                            highlightInfo,
                            textEditor,
                            h
                    );

                    problems.add(newProblem);
                });

        return problems;
    }

    public void restartManualScan() {
        cancelScheduledFuture();
        createAndStartScheduledFuture();
    }

    public void setDelayMilliseconds(int newDelayMilliseconds) {
        delayMilliseconds = newDelayMilliseconds;
        restartManualScan();
    }

    private void cancelScheduledFuture() {
        if (!scheduledFuture.cancel(false)) {
            if (!scheduledFuture.cancel(true)) {
                logger.warn("Unable to cancel scheduledFuture");
            }
        }
    }

    private void createAndStartScheduledFuture() {
        scheduledFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                () -> ApplicationManager.getApplication().invokeLater(this::scanForProblemsManually),
                2000,
                delayMilliseconds,
                TimeUnit.MILLISECONDS
        );
    }
}
