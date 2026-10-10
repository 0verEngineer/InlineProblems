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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.ProblemManager;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MarkupModelProblemListener implements MarkupModelListener {
    private final SettingsState settingsState;
    private final TextEditor textEditor;

    /* One listener per TextEditor. The map is also used to avoid installing a second listener
     * on an editor that already has one, which would double every problem event. */
    private static final Map<TextEditor, Disposable> disposables = new HashMap<>();

    private MarkupModelProblemListener(
            final TextEditor textEditor
    ) {
        this.textEditor = textEditor;

        settingsState = SettingsState.getInstance();
    }

    @Override
    public void afterAdded(@NotNull RangeHighlighterEx highlighter) {
        queueRescan(highlighter);
    }

    @Override
    public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
        queueRescan(highlighter);
    }

    @Override
    public void attributesChanged(@NotNull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
        queueRescan(highlighter);
    }

    /**
     * A daemon run fires one event per highlighter, so in a file with a thousand problems this is
     * called thousands of times per edit. Handling every event on its own meant walking the whole
     * active problem list twice per event and adding or removing a single inlay - and every inlay
     * change makes the editor recalculate its preferred size.
     * <p>
     * The events are therefore coalesced into a single rescan of this editor through the scanners
     * merging queue, and the diff in
     * {@link ProblemManager#updateFromNewActiveProblemsForTextEditor} then only touches the
     * problems that really changed.
     */
    private void queueRescan(RangeHighlighterEx highlighter) {
        if (!settingsState.isEnableInlineProblem())
            return;

        if (settingsState.getActiveListener() != Listener.MARKUP_MODEL_LISTENER)
            return;

        // The markup model holds far more than problems, e.g. the caret row or search results
        if (!(highlighter.getErrorStripeTooltip() instanceof HighlightInfo))
            return;

        Editor editor = textEditor.getEditor();
        Project project = editor.getProject();

        if (
                editor.isDisposed() ||
                !textEditor.isValid() ||
                textEditor.getFile() == null ||
                project == null ||
                project.isDisposed() ||
                !project.isInitialized()
        ) {
            return;
        }

        DocumentMarkupModelScanner.getInstance().scanForProblemsManuallyInTextEditor(textEditor);
    }

    public static void setup(TextEditor textEditor) {
        Editor editor = textEditor.getEditor();
        var documentMarkupModel = DocumentMarkupModel.forDocument(editor.getDocument(), editor.getProject(), false);

        if (!(documentMarkupModel instanceof MarkupModelEx markupModelEx) || textEditor.getFile() == null) {
            return;
        }

        if (disposables.containsKey(textEditor)) {
            return;
        }

        Disposable disposable = new MarkupModelProblemListenerDisposable();
        Disposer.register(ApplicationManager.getApplication().getService(ProblemManager.class), disposable);

        markupModelEx.addMarkupModelListener(
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
}
