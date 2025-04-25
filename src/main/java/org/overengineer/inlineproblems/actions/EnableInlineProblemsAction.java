package org.overengineer.inlineproblems.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.Notifier;
import org.overengineer.inlineproblems.bundles.SettingsBundle;
import org.overengineer.inlineproblems.settings.SettingsState;

public class EnableInlineProblemsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        SettingsState settingsState = SettingsState.getInstance();
        if (settingsState.isEnableInlineProblem()) {
            Notifier.notify(SettingsBundle.message("settings.enableInlineProblem.disabled"), NotificationType.IDE_UPDATE, anActionEvent.getProject());
            settingsState.setEnableInlineProblem(false);
        } else {
            Notifier.notify(SettingsBundle.message("settings.enableInlineProblem.enabled"), NotificationType.IDE_UPDATE, anActionEvent.getProject());
            settingsState.setEnableInlineProblem(true);
        }
        ApplicationManager.getApplication().invokeAndWait(DocumentMarkupModelScanner.getInstance()::scanForProblemsManually);
    }
}
