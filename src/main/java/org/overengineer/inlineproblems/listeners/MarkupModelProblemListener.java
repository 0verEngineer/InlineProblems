package org.overengineer.inlineproblems.listeners;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.fileEditor.TextEditor;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.InlineDrawer;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.entities.enums.Listeners;
import org.overengineer.inlineproblems.settings.SettingsState;


public class MarkupModelProblemListener implements MarkupModelListener {
    private final SettingsState settingsState;
    private final InlineDrawer inlineDrawer;
    private final DocumentMarkupModelScanner markupModelScanner = new DocumentMarkupModelScanner();
    private final String filePath;
    private final TextEditor textEditor;
    private boolean isUnityProject = false;

    public static final String NAME = "MarkupModelListener";

    private enum EventType {
        ADD, REMOVE, CHANGE
    }

    private MarkupModelProblemListener(
            final TextEditor textEditor
    ) {
        this.textEditor = textEditor;
        this.filePath = textEditor.getFile().getPath();

        inlineDrawer = ApplicationManager.getApplication().getService(InlineDrawer.class);
        settingsState = SettingsState.getInstance();
    }

    @Override
    public void afterAdded(@NotNull RangeHighlighterEx highlighter) {
        handleEvent(EventType.ADD, highlighter);
    }

    @Override
    public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
        handleEvent(EventType.REMOVE, highlighter);
    }

    @Override
    public void attributesChanged(@NotNull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
        handleEvent(EventType.CHANGE, highlighter);
    }

    public static void setup(TextEditor textEditor) {
        Editor editor = textEditor.getEditor();
        var documentMarkupModel = DocumentMarkupModel.forDocument(editor.getDocument(), editor.getProject(), false);

        if (!(documentMarkupModel instanceof MarkupModelEx) || textEditor.getFile() == null) {
            return;
        }

        ((MarkupModelEx) documentMarkupModel).addMarkupModelListener(
                ApplicationManager.getApplication().getService(InlineDrawer.class),
                new MarkupModelProblemListener(textEditor)
        );
    }

    private void handleEvent(EventType type, @NotNull RangeHighlighterEx highlighter) {
        if (settingsState.getEnabledListener() != Listeners.MARKUP_MODEL_LISTENER)
            return;

        Editor editor = textEditor.getEditor();

        if (editor.isDisposed())

        if (isUnityProject) {
            // todo does not work reliably at all
            //  -> use a typedHandler and a timer

            markupModelScanner.scanForProblemsManually(textEditor);
            return;
        }

        int lineCount = editor.getDocument().getLineCount();
        if (lineCount <= 0)
            return;

        int fileEndOffset = editor.getDocument().getLineEndOffset(lineCount - 1);

        if (fileEndOffset < highlighter.getEndOffset()) {
            return;
        }

        if (!(highlighter.getErrorStripeTooltip() instanceof HighlightInfo))
            return;

        InlineProblem problem = constructProblem(
                editor.getDocument().getLineNumber(highlighter.getEndOffset()),
                (HighlightInfo) highlighter.getErrorStripeTooltip()
        );

        if (problem.getText().equals(""))
            return;

        switch (type) {
            case ADD:
                inlineDrawer.addProblem(problem);
                break;
            case REMOVE:
                if (!inlineDrawer.removeProblemWithRefreshFromActiveProblems(problem)) {
                    markupModelScanner.scanForProblemsManually(textEditor);
                }
                break;
            case CHANGE:
                if(!inlineDrawer.removeProblemWithRefreshFromActiveProblems(problem)) {
                    markupModelScanner.scanForProblemsManually(textEditor);
                }
                else {
                    inlineDrawer.addProblem(problem);
                }
                break;
        }
    }

    private InlineProblem constructProblem(int line, HighlightInfo info) {
        String usedText = info.getDescription();
        if (usedText == null)
            usedText = "";

        var problem =  new InlineProblem(
                line,
                info.getSeverity().myVal,
                usedText,
                textEditor.getEditor(),
                filePath,
                textEditor.getEditor().getProject()
        );

        return problem;
    }
}