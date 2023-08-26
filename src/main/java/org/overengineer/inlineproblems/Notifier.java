package org.overengineer.inlineproblems;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;


public class Notifier {
    public static void notify(String content, NotificationType type, Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("InlineProblems")
                .createNotification("InlineProblems", content, type)
                .notify(project);
    }
}