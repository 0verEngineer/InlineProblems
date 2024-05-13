package org.overengineer.inlineproblems.utils;

import org.overengineer.inlineproblems.settings.SettingsState;

public class FileNameUtil {
    public static boolean ignoreFile(String fileName) {
        boolean ignore = false;

        for (var e : SettingsState.getInstance().getFileExtensionBlacklist().split(";")) {
            if (e.isBlank() || e.isEmpty() || e.equals(";")) continue;
            if (fileName.endsWith(e)) {
                ignore = true;
                break;
            }
        }

        return ignore;
    }
}
