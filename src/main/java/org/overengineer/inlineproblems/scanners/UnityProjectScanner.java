package org.overengineer.inlineproblems.scanners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.*;


public class UnityProjectScanner {

    Logger logger = Logger.getInstance(UnityProjectScanner.class);

    private final String[] unityReferences = {
            "UnityEngine",
            "UnityEngine.CoreModule",
            "UnityEngine.SharedInternalsModule",
            "UnityEditor",
            "UnityEditor.CoreModule"
    };

    public boolean isUnityProject(Project project) {
        if (project.getBasePath() == null)
            return false;

        File dir = new File(project.getBasePath());

        File[] files = dir.listFiles();
        if (files == null)
            return false;

        for (File file : files) {
            if (file.isFile() && file.exists() && file.getName().endsWith(".csproj")) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line = reader.readLine();

                    while (line != null) {
                        for (String reference : unityReferences) {
                            if (line.contains(reference)) {
                                return true;
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

        return false;
    }
}
