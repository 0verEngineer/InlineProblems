package org.overengineer.inlineproblems;

import com.intellij.configurationStore.StoreUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import lombok.Getter;
import org.overengineer.inlineproblems.entities.InlineProblemProject;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.entities.enums.ProjectType;
import org.overengineer.inlineproblems.scanners.UnityProjectScanner;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


// Only used in rider because of the unity projects
public class UnityProjectManager {

    private final List<InlineProblemProject> projects = new ArrayList<>();

    private int enabledListenerBefore;

    @Getter
    private final UnityProjectScanner unityProjectScanner = new UnityProjectScanner();

    private final SettingsState settings = SettingsState.getInstance();

    private final Logger logger = Logger.getInstance(UnityProjectManager.class);

    private static UnityProjectManager instance;

    private UnityProjectManager() {
        enabledListenerBefore = settings.getEnabledListener();
    }

    public static UnityProjectManager getInstance() {
        if (instance == null)
            instance = new UnityProjectManager();

        return instance;
    }

    public void projectOpened(InlineProblemProject project) {
        boolean isUnityProjectOpenedBefore = isAUnityProjectOpened();

        projects.add(project);

        if (!isUnityProjectOpenedBefore && isAUnityProjectOpened()) {
            handleUnityProjectOpened();
        }
    }

    public void projectClosed(Project project) {
        boolean isUnityProjectOpenedBefore = isAUnityProjectOpened();

        List<InlineProblemProject> projectsToRemove = projects.stream()
                .filter(p -> p.getProject().equals(project))
                .collect(Collectors.toList());

        if (projectsToRemove.size() != 1)
            logger.warn("ProjectManager projects to remove size: " + projectsToRemove.size());

        projectsToRemove.forEach(projects::remove);

        if (isUnityProjectOpenedBefore && !isAUnityProjectOpened()) {
            handleNoMoreUnityProjectsOpened();
        }
    }

    public void scanAllOpenProjectsForUnity() {
        ProjectManager projectManager = ProjectManager.getInstance();
        if (projectManager != null) {
            for (var project : projectManager.getOpenProjects()) {
                if (unityProjectScanner.isUnityProject(project))
                    projectOpened(new InlineProblemProject(project, ProjectType.UNITY_GAME_ENGINE));
                else
                    projectOpened(new InlineProblemProject(project, ProjectType.DEFAULT));
            }
        }
    }

    private boolean isAUnityProjectOpened() {
        return projects.stream()
                .anyMatch(p -> p.getType().equals(ProjectType.UNITY_GAME_ENGINE));
    }

    private void handleUnityProjectOpened() {
        enabledListenerBefore = settings.getEnabledListener();
        settings.setEnabledListener(Listener.MANUAL_SCANNING);
        ListenerManager.getInstance().changeListener();
    }

    private void handleNoMoreUnityProjectsOpened() {
        if (settings.getEnabledListener() == Listener.MANUAL_SCANNING) {
            settings.setEnabledListener(enabledListenerBefore);
            ListenerManager.getInstance().changeListener();
            StoreUtil.saveSettings(ApplicationManager.getApplication());
        }
    }
}
