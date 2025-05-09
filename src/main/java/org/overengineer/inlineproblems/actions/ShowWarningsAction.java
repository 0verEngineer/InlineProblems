package org.overengineer.inlineproblems.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.settings.SettingsState;

public class ShowWarningsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        SettingsState settingsState = SettingsState.getInstance();
        settingsState.setShowWarnings(!settingsState.isShowWarnings());
    }
}
