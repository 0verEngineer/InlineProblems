package org.overengineer.inlineproblems;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.entities.IDE;
import org.overengineer.inlineproblems.entities.InlineProblemProject;
import org.overengineer.inlineproblems.entities.enums.ProjectType;


/**
 * {@link ProjectActivity} is a Kotlin interface with a suspending function. Implemented from Java
 * that means taking the {@link Continuation} and returning {@link Unit#INSTANCE}, which marks the
 * activity as completed without suspending. The predecessor {@code StartupActivity} is deprecated
 * and the platform logs a PluginException asking for this migration.
 */
public class ProjectStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // Rider specific handling for Unity projects
        if (ApplicationInfo.getInstance().getFullApplicationName().startsWith(IDE.RIDER)) {
            UnityProjectManager projectManager = UnityProjectManager.getInstance();

            ProjectType type = projectManager.getUnityProjectScanner().isUnityProject(project)
                    ? ProjectType.UNITY_GAME_ENGINE
                    : ProjectType.DEFAULT;

            projectManager.projectOpened(new InlineProblemProject(project, type));
        }

        return Unit.INSTANCE;
    }
}
