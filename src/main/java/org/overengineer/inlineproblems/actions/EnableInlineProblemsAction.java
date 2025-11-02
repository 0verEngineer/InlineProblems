package org.overengineer.inlineproblems.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.Notifier;
import org.overengineer.inlineproblems.bundles.SettingsBundle;
import org.overengineer.inlineproblems.settings.SettingsState;

public class EnableInlineProblemsAction extends IPAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        SettingsState settingsState = SettingsState.getInstance();
        if (settingsState.isEnableInlineProblemsNotifications()) {
            Notifier.notify(SettingsBundle.message(
                    settingsState.isEnableInlineProblem()
                    ? "settings.enableInlineProblem.disabled"
                    : "settings.enableInlineProblem.enabled"),
                    NotificationType.INFORMATION,
                    anActionEvent.getProject()
            );
        }
        settingsState.setEnableInlineProblem(!settingsState.isEnableInlineProblem());
        resetOrRefreshProblems(settingsState);
    }
}
