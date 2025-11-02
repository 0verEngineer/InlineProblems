package org.overengineer.inlineproblems.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.ProblemManager;
import org.overengineer.inlineproblems.settings.SettingsState;

public abstract class IPAction extends AnAction {

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
