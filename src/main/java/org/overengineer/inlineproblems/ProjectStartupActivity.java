package org.overengineer.inlineproblems;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.entities.IDE;
import org.overengineer.inlineproblems.entities.InlineProblemProject;
import org.overengineer.inlineproblems.entities.enums.ProjectType;


public class ProjectStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        if (ApplicationInfo.getInstance().getFullApplicationName().startsWith(IDE.RIDER)) {
            UnityProjectManager projectManager = UnityProjectManager.getInstance();

            if (projectManager.getUnityProjectScanner().isUnityProject(project)) {
                projectManager.projectOpened(new InlineProblemProject(project, ProjectType.UNITY_GAME_ENGINE));
            }
            else {
                projectManager.projectOpened(new InlineProblemProject(project, ProjectType.DEFAULT));
            }
        }
    }
}
