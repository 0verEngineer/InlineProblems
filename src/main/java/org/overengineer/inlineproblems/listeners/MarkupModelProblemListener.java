package org.overengineer.inlineproblems.listeners;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class MarkupModelProblemListener implements MarkupModelListener {
    private final SettingsState settingsState;
    private final ProblemManager problemManager;
    private final TextEditor textEditor;

    private static final List<Disposable> disposables = new ArrayList<>();

    public static final String NAME = "MarkupModelListener (default)";

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

        Disposable disposable = new MarkupModelProblemListenerDisposable();
        Disposer.register(ApplicationManager.getApplication().getService(ProblemManager.class), disposable);

        ((MarkupModelEx) documentMarkupModel).addMarkupModelListener(
                disposable,
                new MarkupModelProblemListener(textEditor)
        );

        disposables.add(disposable);
    }

    public static void disposeAll() {
        List.copyOf(disposables)
                .forEach(d -> {
                    Disposer.dispose(d);
                    disposables.remove(d);
                });
    }

    private void handleEvent(EventType type, @NotNull RangeHighlighterEx highlighter) {
        if (!settingsState.isEnableInlineProblem())
            return;

        if (settingsState.getEnabledListener() != Listener.MARKUP_MODEL_LISTENER)
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
            problemToRemove = findActiveProblemByRangeHighlighterHashCode(highlighter.hashCode());

            if (problemToRemove == null) {
                return;
            }
        }

        List<String> problemTextBeginningFilterList = new ArrayList<>(
                Arrays.asList(SettingsState.getInstance().getProblemFilterList().split(";"))
        );

        if (
                newProblem.getText().isEmpty() ||
                        problemTextBeginningFilterList.stream()
                                .anyMatch(f -> newProblem.getText().toLowerCase().startsWith(f.toLowerCase()))
        ) {
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

    private InlineProblem findActiveProblemByRangeHighlighterHashCode(int hashCode) {
        return problemManager.getActiveProblems().stream()
                .filter(p -> p.getRangeHighlighterHashCode() == hashCode)
                .findFirst()
                .orElse(null);
    }
}
