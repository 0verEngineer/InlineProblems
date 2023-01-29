package org.overengineer.inlineproblems.listeners;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.entities.enums.Listeners;
import org.overengineer.inlineproblems.settings.SettingsState;


public class HighlightProblemListener implements HighlightInfoFilter {
    private final DocumentMarkupModelScanner markupModelScanner = DocumentMarkupModelScanner.getInstance();
    private final SettingsState settingsState = SettingsState.getInstance();

    public static final String NAME = "HighlightProblemListener";
    public static final int MANUAL_SCAN_FREQUENCY_MILLIS = 2000;

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (settingsState.getEnabledListener() != Listeners.HIGHLIGHT_PROBLEMS_LISTENER)
            return true;

        if (file == null || !file.isValid())
            return true;

        ApplicationManager.getApplication().invokeLater(() -> handleAccept(file));
        return true;
    }

    public void handleAccept(PsiFile file) {
        if (file.getVirtualFile() == null)
            return;

        FileEditor editor = FileEditorManager.getInstance(file.getProject()).getSelectedEditor(file.getVirtualFile());
        if (editor == null) {
            return;
        }

        TextEditor textEditor = (TextEditor) editor;

        markupModelScanner.scanForProblemsManuallyInTextEditor(textEditor);
    }
}
