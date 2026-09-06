package org.overengineer.inlineproblems.listeners;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.FileUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class HighlightProblemListener implements HighlightInfoFilter {
    private final DocumentMarkupModelScanner markupModelScanner = DocumentMarkupModelScanner.getInstance();
    private final SettingsState settingsState = SettingsState.getInstance();

    /* Static because the platform may instantiate the filter more than once. Entries are removed
     * as soon as the queued task runs, so the set stays at the number of files being analyzed. */
    private static final Set<VirtualFile> pendingFiles = ConcurrentHashMap.newKeySet();

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (!settingsState.isEnableInlineProblem())
            return true;
        if (settingsState.getActiveListener() != Listener.HIGHLIGHT_PROBLEMS_LISTENER)
            return true;
        if (file == null || !file.isValid())
            return true;

        // Only check file name here, the line count is checked in the scanForProblemsManuallyInTextEditor call
        if (FileUtil.ignoreFile(file.getName(), -1)) {
            return true;
        }

        if (file.getProject().isDisposed()) {
            return true;
        }

        /* accept() is called once per HighlightInfo, so a single analysis run of a file with a
         * thousand problems used to post a thousand EDT tasks that all end up doing the same
         * rescan. One pending task per file is enough. */
        VirtualFile virtualFile = file.getVirtualFile();

        if (virtualFile != null && pendingFiles.add(virtualFile)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                pendingFiles.remove(virtualFile);
                handleAccept(file);
            });
        }

        return true;
    }

    public void handleAccept(PsiFile file) {
        if (settingsState.getActiveListener() != Listener.HIGHLIGHT_PROBLEMS_LISTENER)
            return;

        if (file.getProject().isDisposed() || file.getVirtualFile() == null)
            return;

        FileEditor editor = FileEditorManager.getInstance(file.getProject()).getSelectedEditor(file.getVirtualFile());

        // instanceof also covers editor being null
        if (!(editor instanceof TextEditor textEditor)) {
            return;
        }

        markupModelScanner.scanForProblemsManuallyInTextEditor(textEditor);
    }
}
