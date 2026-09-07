package org.overengineer.inlineproblems.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.ProblemManager;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.FileUtil;

import java.util.Arrays;


public class FileEditorListener implements FileEditorManagerListener {

    SettingsState settingsState = SettingsState.getInstance();

    /**
     * Both fileOpenedSync overloads are deprecated, so the listener is installed from fileOpened.
     * That runs once the file is open instead of synchronously during opening, which is what this
     * listener wants anyway - the editor and its markup model are ready at that point.
     */
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (settingsState.getActiveListener() != Listener.MARKUP_MODEL_LISTENER)
            return;

        // Precheck only the file name, the line count is checked per editor below
        if (FileUtil.ignoreFile(file.getName(), -1)) {
            return;
        }

        Arrays.stream(source.getAllEditors(file))
                .filter(TextEditor.class::isInstance)
                .map(TextEditor.class::cast)
                .filter(tE -> !FileUtil.ignoreFile(null, tE.getEditor().getDocument().getLineCount()))
                .forEach(MarkupModelProblemListener::setup);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        MarkupModelProblemListener.disposeInvalid();

        ApplicationManager.getApplication()
                .getService(ProblemManager.class)
                .removeObsoleteProblems();
    }
}
