package org.overengineer.inlineproblems;

import com.intellij.notification.NotificationType;
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

    private Listener enabledListenerBefore;

    @Getter
    private final UnityProjectScanner unityProjectScanner = new UnityProjectScanner();

    private final SettingsState settings = SettingsState.getInstance();

    private final Logger logger = Logger.getInstance(UnityProjectManager.class);

    private static UnityProjectManager instance;

    private UnityProjectManager() {
        enabledListenerBefore = settings.getActiveListener();
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
            handleUnityProjectOpened(project.project());
        }
    }

    public void projectClosed(Project project) {
        boolean isUnityProjectOpenedBefore = isAUnityProjectOpened();

        List<InlineProblemProject> projectsToRemove = projects.stream()
                .filter(p -> p.project().equals(project))
                .collect(Collectors.toList());

        if (projectsToRemove.size() != 1)
            logger.warn("ProjectManager projects to remove size: " + projectsToRemove.size());

        projectsToRemove.forEach(projects::remove);

        if (isUnityProjectOpenedBefore && !isAUnityProjectOpened()) {
            handleNoMoreUnityProjectsOpened(project);
        }
    }

    public void scanAllOpenProjectsForUnity() {
        ProjectManager projectManager = ProjectManager.getInstance();
        if (projectManager != null) {
            for (var project : projectManager.getOpenProjects()) {
                ProjectType type = unityProjectScanner.isUnityProject(project)
                        ? ProjectType.UNITY_GAME_ENGINE
                        : ProjectType.DEFAULT;

                projectOpened(new InlineProblemProject(project, type));
            }
        }
    }

    private boolean isAUnityProjectOpened() {
        return projects.stream()
                .anyMatch(p -> p.type() == ProjectType.UNITY_GAME_ENGINE);
    }

    private void handleUnityProjectOpened(Project project) {
        enabledListenerBefore = settings.getActiveListener();
        if (enabledListenerBefore == Listener.MANUAL_SCANNING)
            return;

        settings.setActiveListener(Listener.MANUAL_SCANNING);

        ListenerManager listenerManager = ListenerManager.getInstance();
        listenerManager.resetAndRescan();
        listenerManager.changeListener();
        Notifier.notify("Unity project opened. Listener changed to Manual Scanning.", NotificationType.INFORMATION, project);
    }

    private void handleNoMoreUnityProjectsOpened(Project project) {
        if (enabledListenerBefore == Listener.MANUAL_SCANNING)
            return;

        if (settings.getActiveListener() == Listener.MANUAL_SCANNING) {
            settings.setActiveListener(enabledListenerBefore);

            ListenerManager listenerManager = ListenerManager.getInstance();
            listenerManager.resetAndRescan();
            listenerManager.changeListener();
            Notifier.notify("No more Unity projects opened. Listener changed back to the previous one", NotificationType.INFORMATION, project);

            // PersistentStateComponent is somehow not working (Settings change is not persisted)
            ApplicationManager.getApplication().saveSettings();
        }
    }
}
