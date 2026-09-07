package org.overengineer.inlineproblems.scanners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;


public class UnityProjectScanner {

    private final Logger logger = Logger.getInstance(UnityProjectScanner.class);

    private final String[] unityReferences = {
            "UnityEngine",
            "UnityEngine.CoreModule",
            "UnityEngine.SharedInternalsModule",
            "UnityEditor",
            "UnityEditor.CoreModule"
    };

    public boolean isUnityProject(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null)
            return false;

        File[] files = new File(basePath).listFiles();
        if (files == null)
            return false;

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".csproj")) {
                continue;
            }

            if (containsUnityReference(file.toPath())) {
                return true;
            }
        }

        return false;
    }

    private boolean containsUnityReference(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = reader.readLine();

            while (line != null) {
                for (String reference : unityReferences) {
                    if (line.contains(reference)) {
                        return true;
                    }
                }

                line = reader.readLine();
            }
        }
        catch (NoSuchFileException ignored) {
            // The file disappeared between listing and reading
        }
        catch (IOException e) {
            logger.warn("Unable to read '" + path.getFileName() + "' during Unity project detection", e);
        }

        return false;
    }
}
