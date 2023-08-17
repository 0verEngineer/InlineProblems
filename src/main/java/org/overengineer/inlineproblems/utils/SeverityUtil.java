package org.overengineer.inlineproblems.utils;

import com.intellij.lang.annotation.HighlightSeverity;

public class SeverityUtil {
    public static String getSeverityAsString(int severity) {
        if (severity >= HighlightSeverity.ERROR.myVal) {
            return "ERROR";
        } else if (severity >= HighlightSeverity.WARNING.myVal) {
            return "WARNING";
        } else if (severity >= HighlightSeverity.WEAK_WARNING.myVal) {
            return "WEAK WARNING";
        } else if (severity >= HighlightSeverity.INFORMATION.myVal) {
            return "INFO";
        }
        return "";
    }
}
