package org.overengineer.inlineproblems.listeners;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.ProblemManager;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.ProblemTextFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MarkupModelProblemListener implements MarkupModelListener {
    private final SettingsState settingsState;
    private final ProblemManager problemManager;
    private final TextEditor textEditor;

    /* One listener per TextEditor. The map is also used to avoid installing a second listener
     * on an editor that already has one, which would double every problem event. */
    private static final Map<TextEditor, Disposable> disposables = new HashMap<>();

    private enum EventType {
        ADD, REMOVE, CHANGE
    }

    private MarkupModelProblemListener(
            final TextEditor textEditor
    ) {
        this.textEditor = textEditor;

        problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);
        settingsState = SettingsState.getInstance();
    }

    @Override
    public void afterAdded(@NotNull RangeHighlighterEx highlighter) {
        ApplicationManager.getApplication().invokeLater(() -> handleEvent(EventType.ADD, highlighter));
    }

    @Override
    public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
        ApplicationManager.getApplication().invokeLater(() -> handleEvent(EventType.REMOVE, highlighter));
    }

    @Override
    public void attributesChanged(@NotNull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
        ApplicationManager.getApplication().invokeLater(() -> handleEvent(EventType.CHANGE, highlighter));
    }

    public static void setup(TextEditor textEditor) {
        Editor editor = textEditor.getEditor();
        var documentMarkupModel = DocumentMarkupModel.forDocument(editor.getDocument(), editor.getProject(), false);

        if (!(documentMarkupModel instanceof MarkupModelEx) || textEditor.getFile() == null) {
            return;
        }

        if (disposables.containsKey(textEditor)) {
            return;
        }

        Disposable disposable = new MarkupModelProblemListenerDisposable();
        Disposer.register(ApplicationManager.getApplication().getService(ProblemManager.class), disposable);

        ((MarkupModelEx) documentMarkupModel).addMarkupModelListener(
                disposable,
                new MarkupModelProblemListener(textEditor)
        );

        disposables.put(textEditor, disposable);
    }

    /**
     * Removes the listeners of all editors that are gone, to be called when an editor is closed.
     * Without this the disposable stays registered on the ProblemManager for the whole IDE
     * session, one per file that was ever opened.
     */
    public static void disposeInvalid() {
        List.copyOf(disposables.keySet()).stream()
                .filter(tE -> !tE.isValid() || tE.getEditor().isDisposed())
                .forEach(tE -> {
                    Disposable disposable = disposables.remove(tE);

                    if (disposable != null) {
                        Disposer.dispose(disposable);
                    }
                });
    }

    public static void disposeAll() {
        List.copyOf(disposables.values()).forEach(Disposer::dispose);
        disposables.clear();
    }

    private void handleEvent(EventType type, @NotNull RangeHighlighterEx highlighter) {
        if (!settingsState.isEnableInlineProblem())
            return;

        if (settingsState.getActiveListener() != Listener.MARKUP_MODEL_LISTENER)
            return;

        Editor editor = textEditor.getEditor();

        if (editor.isDisposed() || editor.getProject() == null || editor.getProject().isDisposed() || !editor.getProject().isInitialized() || textEditor.getFile() == null)
            return;

        int lineCount = editor.getDocument().getLineCount();
        if (lineCount <= 0)
            return;

        int fileEndOffset = editor.getDocument().getLineEndOffset(lineCount - 1);

        if (fileEndOffset < highlighter.getStartOffset()) {
            return;
        }

        if (!(highlighter.getErrorStripeTooltip() instanceof HighlightInfo))
            return;

        /*
         * We use manual scanning if this option is enabled because we need all problems in the current textEditor to be
         * updated.
         */
        if (settingsState.isShowOnlyHighestSeverityPerLine() && highlighter.getErrorStripeTooltip() != null) {
            var highlightInfo = (HighlightInfo) highlighter.getErrorStripeTooltip();

            if (highlightInfo != null &&
                    highlightInfo.getDescription() != null &&
                    !Objects.equals(highlightInfo.getDescription(), "")
            ) {
                DocumentMarkupModelScanner.getInstance().scanForProblemsManuallyInTextEditor(textEditor);
                return;
            }

            return;
        }

        InlineProblem newProblem;
        InlineProblem problemToRemove = null;

        var highlightInfo = (HighlightInfo) highlighter.getErrorStripeTooltip();
        if (highlightInfo == null)
            return;

        int startOffset = highlighter.getStartOffset();
        if (startOffset < 0)
            return;

        newProblem = new InlineProblem(
                editor.getDocument().getLineNumber(startOffset),
                textEditor.getFile().getPath(),
                highlightInfo,
                textEditor,
                highlighter,
                settingsState
        );

        if (type == EventType.CHANGE || type == EventType.REMOVE) {
            problemToRemove = findActiveProblemByRangeHighlighter(highlighter);

            if (problemToRemove == null) {
                return;
            }
        }

        if (newProblem.getText().isEmpty() || ProblemTextFilter.isFiltered(newProblem.getText())) {
            return;
        }

        problemManager.applyCustomSeverity(newProblem);
        if (problemManager.shouldProblemBeIgnored(newProblem.getSeverity())) {
            return;
        }

        switch (type) {
            case ADD:
                problemManager.addProblem(newProblem);
                break;
            case REMOVE:
                problemManager.removeProblem(problemToRemove);
                break;
            case CHANGE:
                problemManager.removeProblem(problemToRemove);
                problemManager.addProblem(newProblem);
                break;
        }
    }

    /**
     * The markup model belongs to the document, so a highlighter is shared between all editors
     * of that document (split view). The problem of this listeners own editor is the one to
     * look for.
     */
    private InlineProblem findActiveProblemByRangeHighlighter(RangeHighlighter rangeHighlighter) {
        return problemManager.getActiveProblems().stream()
                .filter(p -> p.getRangeHighlighter() == rangeHighlighter)
                .filter(p -> Objects.equals(p.getTextEditor(), textEditor))
                .findFirst()
                .orElse(null);
    }
}
