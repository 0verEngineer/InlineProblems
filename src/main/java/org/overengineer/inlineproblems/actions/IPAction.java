package org.overengineer.inlineproblems.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.ProblemManager;
import org.overengineer.inlineproblems.settings.SettingsState;

public abstract class IPAction extends AnAction {

    /**
     * None of the actions implements update(), so there is nothing that needs the EDT. Without
     * this override the platform logs a warning for every action since 2022.3.
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    protected void resetOrRefreshProblems(SettingsState settingsState) {
        var app = ApplicationManager.getApplication();
        if (settingsState.isEnableInlineProblem()) {
            app.invokeLater(DocumentMarkupModelScanner.getInstance()::scanForProblemsManually);
        }
        else {
            var problemManager = app.getService(ProblemManager.class);
            if (problemManager != null) {
                problemManager.reset();
            }
        }
    }
}
