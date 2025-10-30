package org.overengineer.inlineproblems.utils;

import org.overengineer.inlineproblems.settings.SettingsState;

public class FileUtil {

    /**
     * Returns true if the file should be ignored, current checks involve the file name and the line count
     * @param fileName can be null to ignore
     * @param lineCount can be -1 to ignore
     * @return true if the file should be ignored, false otherwise
     */
    public static boolean ignoreFile(String fileName, int lineCount) {
        boolean ignore = false;
        var settings = SettingsState.getInstance();

        if (fileName != null) {
            for (var e : settings.getFileExtensionBlacklist().split(";")) {
                if (e.isBlank() || e.isEmpty() || e.equals(";")) continue;
                if (fileName.endsWith(e)) {
                    ignore = true;
                    break;
                }
            }
        }

        if (lineCount >= 0) {
            var maxFileLines = settings.getMaxFileLines();

            // maxFileLines == 0 => line count is ignored
            if (maxFileLines > 0 && lineCount >= maxFileLines) {
                ignore = true;
            }
        }

        return ignore;
    }
}
