package org.overengineer.inlineproblems.scanners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.entities.enums.Listeners;
import org.overengineer.inlineproblems.listeners.MarkupModelProblemListener;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.io.*;


public class UnityProjectScanner {

    Logger logger = Logger.getInstance(UnityProjectScanner.class);

    DocumentMarkupModelScanner documentMarkupModelScanner = DocumentMarkupModelScanner.getInstance();

    SettingsState settingsState = SettingsState.getInstance();

    private final String[] unityReferences = {
            "UnityEngine",
            "UnityEngine.CoreModule",
            "UnityEngine.SharedInternalsModule",
            "UnityEditor",
            "UnityEditor.CoreModule"
    };

    public void scanAndHandleUnityProject(Project project) {
        if (project.getBasePath() == null)
            return;

        File dir = new File(project.getBasePath());

        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isFile() && file.exists() && file.getName().endsWith(".csproj")) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line = reader.readLine();

                    while (line != null) {
                        for (String reference : unityReferences) {
                            if (line.contains(reference)) {
                                handleUnityProject();
                                return;
                            }
                        }

                        line = reader.readLine();
                    }

                } catch (FileNotFoundException ignored) {} catch (IOException e) {
                    logger.warn("IOException in unity detection");
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleUnityProject() {
        settingsState.setEnabledListener(Listeners.MANUAL_SCANNING);
        documentMarkupModelScanner.setFrequencyMilliseconds(DocumentMarkupModelScanner.MANUAL_SCAN_FREQUENCY_MILLIS);
    }
}
