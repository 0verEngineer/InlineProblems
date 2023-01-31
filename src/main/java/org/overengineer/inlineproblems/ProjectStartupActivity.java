package org.overengineer.inlineproblems;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.scanners.UnityProjectScanner;


public class ProjectStartupActivity implements StartupActivity {

    private static final String RIDER_NAME = "JetBrains Rider";

    @Override
    public void runActivity(@NotNull Project project) {
        if (ApplicationInfo.getInstance().getFullApplicationName().startsWith(RIDER_NAME)) {
            new UnityProjectScanner().scanAndHandleUnityProject(project);
        }
    }
}
