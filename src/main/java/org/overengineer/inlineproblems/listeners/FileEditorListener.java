package org.overengineer.inlineproblems.listeners;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.FileNameUtil;

import java.util.Arrays;


public class FileEditorListener implements FileEditorManagerListener {

    SettingsState settingsState = SettingsState.getInstance();

    @Override
    public void fileOpenedSync(
            @NotNull FileEditorManager source,
            @NotNull VirtualFile file,
            @NotNull Pair<FileEditor[], FileEditorProvider[]> editors
    ) {
        if (settingsState.getEnabledListener() != Listener.MARKUP_MODEL_LISTENER)
            return;

        if (FileNameUtil.ignoreFile(file.getName())) {
            return;
        }

        Arrays.stream(editors.first)
                .filter(e -> e instanceof TextEditor)
                .map(e -> (TextEditor)e)
                .forEach(MarkupModelProblemListener::setup);
    }
}
