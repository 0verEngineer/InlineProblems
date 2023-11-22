package org.overengineer.inlineproblems.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.ListenerManager;
import org.overengineer.inlineproblems.settings.SettingsState;

public class ToggleShowErrorsAction extends ToggleAction implements DumbAware {

    private SettingsState settings =
            ApplicationManager.getApplication().getService(SettingsState.class);
    private final ListenerManager listenerManager = ListenerManager.getInstance();

    private boolean showErrors;
    private boolean showWarnings;
    private boolean showWeakWarnings;
    private boolean showInfos;


    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        showErrors = settings.isShowErrors();
        showWarnings = settings.isShowWarnings();
        showWeakWarnings = settings.isShowWeakWarnings();
        showInfos = settings.isShowInfos();

        return showErrors || showWarnings || showWeakWarnings || showInfos;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        settings.setShowErrors(state);
        settings.setShowWarnings(state);
        settings.setShowWeakWarnings(state);
        settings.setShowInfos(state);

        listenerManager.resetAndRescan();
        listenerManager.changeListener();
    }
}
