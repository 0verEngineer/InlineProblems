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
import org.overengineer.inlineproblems.utils.FileUtil;
import org.overengineer.inlineproblems.utils.ProblemTextFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class DocumentMarkupModelScanner implements Disposable {
    private final ProblemManager problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);
    private final SettingsState settingsState;

    private final Logger logger = Logger.getInstance(DocumentMarkupModelScanner.class);

    /* The periodic full scan is only a safety net when one of the event driven listeners is
     * active, so it may run rarely. It used to run every 2 seconds, which meant rebuilding every
     * problem of every open editor on the EDT that often. */
    public static final int SAFETY_NET_SCAN_DELAY_MILLIS = 10_000;

    /* Coalescing window for the per editor rescans. A daemon run fires thousands of markup events
     * within a few hundred milliseconds, and all of them should end up in one rescan. */
    private static final int RESCAN_MERGE_MILLIS = 100;

    private int delayMilliseconds = SAFETY_NET_SCAN_DELAY_MILLIS;

    private static DocumentMarkupModelScanner instance;

    private final MergingUpdateQueue mergingUpdateQueue;

    private ScheduledFuture<?> scheduledFuture;

    private DocumentMarkupModelScanner() {
        Disposer.register(problemManager, this);

        settingsState = SettingsState.getInstance();

        mergingUpdateQueue = new MergingUpdateQueue(
                "DocumentMarkupModelScannerQueue",
                RESCAN_MERGE_MILLIS,
                true,
                null,
                this,
                null,
                true
        );

        SettingsState settingsState = SettingsState.getInstance();
        if (settingsState.getActiveListener() == Listener.MANUAL_SCANNING) {
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
        /* Without cancelling the future the scan keeps running after a plugin unload, and the
         * stale static instance would make a reload end up with two scheduled scans. */
        cancelScheduledFuture();
        mergingUpdateQueue.cancelAllUpdates();

        if (instance == this) {
            instance = null;
        }
    }

    /**
     * Only the visible editors are scanned. A background tab is not analyzed by the daemon either,
     * and once it becomes visible the daemon run that follows fires the markup events that trigger
     * a rescan of it.
     */
    public void scanForProblemsManually() {
        if (!settingsState.isEnableInlineProblem()) {
            return;
        }

        ProjectManager projectManager = ProjectManager.getInstanceIfCreated();
        if (projectManager == null) {
            return;
        }

        for (var project : projectManager.getOpenProjects()) {
            if (!project.isInitialized() || project.isDisposed())
                continue;

            for (var editor : FileEditorManager.getInstance(project).getSelectedEditors()) {
                if (!(editor instanceof TextEditor textEditor)) {
                    continue;
                }

                if (
                        editor.getFile() == null ||
                        FileUtil.ignoreFile(editor.getFile().getName(), textEditor.getEditor().getDocument().getLineCount())
                ) {
                    continue;
                }

                problemManager.updateFromNewActiveProblemsForTextEditor(getProblemsInEditor(textEditor), textEditor);
            }
        }
    }

    /**
     * This function is queued in the mergingUpdateQueue because it is called frequently, this can be multiple times per
     * millisecond if the HighlightProblemListener is used.
     */
    public void scanForProblemsManuallyInTextEditor(TextEditor textEditor) {
        if (
                textEditor.getFile() == null ||
                FileUtil.ignoreFile(textEditor.getFile().getName(), textEditor.getEditor().getDocument().getLineCount())
        ) {
            return;
        }

        /* The editor is the identity of the update: queued rescans of the same editor collapse
         * into one, while rescans of different editors stay independent. With a shared identity
         * only one of several open editors got rescanned. */
        mergingUpdateQueue.queue(new Update(textEditor) {
            @Override
            public void run() {
                List<InlineProblem> problems = settingsState.isEnableInlineProblem() ? getProblemsInEditor(textEditor) : List.of();

                problemManager.updateFromNewActiveProblemsForTextEditor(problems, textEditor);
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

        Arrays.stream(highlighters)
                .filter(h -> {
                    if (h.isValid() && h.getErrorStripeTooltip() instanceof HighlightInfo highlightInfo) {
                        String description = highlightInfo.getDescription();

                        return description != null &&
                                !description.isEmpty() &&
                                !ProblemTextFilter.isFiltered(description) &&
                                fileEndOffset >= highlightInfo.getStartOffset();
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
                            h,
                            settingsState
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
        ScheduledFuture<?> future = scheduledFuture;
        if (future == null) {
            return;
        }

        scheduledFuture = null;

        if (!future.cancel(false) && !future.isDone()) {
            logger.warn("Unable to cancel the scheduled manual scan");
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
