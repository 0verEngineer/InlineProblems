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
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.FileUtil;


public class HighlightProblemListener implements HighlightInfoFilter {
    private final DocumentMarkupModelScanner markupModelScanner = DocumentMarkupModelScanner.getInstance();
    private final SettingsState settingsState = SettingsState.getInstance();
    public static final String NAME = "HighlightProblemListener";
    public static final int ADDITIONAL_MANUAL_SCAN_DELAY_MILLIS = 2000;

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (settingsState.isEnableInlineProblem())
            return true;
        if (settingsState.getEnabledListener() != Listener.HIGHLIGHT_PROBLEMS_LISTENER)
            return true;
        if (file == null || !file.isValid())
            return true;

        // Only check file name here, the line count is checked in the scanForProblemsManuallyInTextEditor call
        if (FileUtil.ignoreFile(file.getName(), -1)) {
            return true;
        }

        if (!file.getProject().isDisposed()) {
            ApplicationManager.getApplication().invokeLater(() -> handleAccept(file));
        }

        return true;
    }

    public void handleAccept(PsiFile file) {
        if (settingsState.getEnabledListener() != Listener.HIGHLIGHT_PROBLEMS_LISTENER)
            return;

        if (file.getProject().isDisposed() || file.getVirtualFile() == null)
            return;

        FileEditor editor = FileEditorManager.getInstance(file.getProject()).getSelectedEditor(file.getVirtualFile());
        if (editor == null || !(editor instanceof TextEditor)) {
            return;
        }

        TextEditor textEditor = (TextEditor) editor;

        markupModelScanner.scanForProblemsManuallyInTextEditor(textEditor);
    }
}
