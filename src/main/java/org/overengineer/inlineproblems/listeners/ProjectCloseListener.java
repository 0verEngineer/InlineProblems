package org.overengineer.inlineproblems.listeners;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.UnityProjectManager;
import org.overengineer.inlineproblems.entities.IDE;


public class ProjectCloseListener implements ProjectManagerListener {

    @Override
    public void projectClosing(@NotNull Project project) {
        // Only used in Rider because of the Unity projects
        if (ApplicationInfo.getInstance().getFullApplicationName().startsWith(IDE.RIDER)) {
            UnityProjectManager.getInstance().projectClosed(project);
        }
    }
}
