package org.overengineer.inlineproblems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.listeners.HighlightProblemListener;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class DocumentMarkupModelScanner implements Disposable {
    private final ProblemManager problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);

    private final Logger logger = Logger.getInstance(DocumentMarkupModelScanner.class);

    private int delayMilliseconds = HighlightProblemListener.ADDITIONAL_MANUAL_SCAN_DELAY_MILLIS;

    private static DocumentMarkupModelScanner instance;

    private final MergingUpdateQueue mergingUpdateQueue;

    private ScheduledFuture<?> scheduledFuture;

    public static final String NAME = "ManualScanner";

    private DocumentMarkupModelScanner() {
        Disposer.register(problemManager, this);

        mergingUpdateQueue = new MergingUpdateQueue(
                "DocumentMarkupModelScannerQueue",
                10,
                true,
                null,
                this,
                null,
                true
        );

        SettingsState settingsState = SettingsState.getInstance();
        if (settingsState.getEnabledListener() == Listener.MANUAL_SCANNING) {
            delayMilliseconds = settingsState.getManualScannerDelay();
        }

        createAndStartScheduledFuture();
    }

    public static DocumentMarkupModelScanner getInstance() {
        if (instance == null)
            instance = new DocumentMarkupModelScanner();

        return instance;
    }

    @Override
    public void dispose() {
        mergingUpdateQueue.cancelAllUpdates();
    }

    public void scanForProblemsManually() {
        ProjectManager projectManager = ProjectManager.getInstanceIfCreated();

        if (projectManager != null) {
            List<InlineProblem> problems = new ArrayList<>();

            for (var project : projectManager.getOpenProjects()) {
                if (!project.isInitialized() || project.isDisposed())
                    continue;

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

    /**
     * This function is queued in the mergingUpdateQueue because it is called frequently, this can be multiple times per
     * millisecond if the HighlightProblemListener is used.
     */
    public void scanForProblemsManuallyInTextEditor(TextEditor textEditor) {
        if (textEditor.getFile() == null) {
            return;
        }

        mergingUpdateQueue.queue(new Update("scan") {
            @Override
            public void run() {
                List<InlineProblem> problems = getProblemsInEditor(textEditor);

                problemManager.updateFromNewActiveProblemsForProjectAndFile(
                        problems,
                        textEditor.getEditor().getProject(),
                        textEditor.getFile().getPath()
                );
            }
        });
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

                    problemManager.applyCustomSeverity(newProblem);
                    if (problemManager.shouldProblemBeIgnored(newProblem.getSeverity())) {
                        return;
                    }

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
                () -> ApplicationManager.getApplication().invokeAndWait(this::scanForProblemsManually),
                2000,
                delayMilliseconds,
                TimeUnit.MILLISECONDS
        );
    }
}
