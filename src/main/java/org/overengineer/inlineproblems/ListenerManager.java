package org.overengineer.inlineproblems;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.ProjectManager;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.listeners.HighlightProblemListener;
import org.overengineer.inlineproblems.listeners.MarkupModelProblemListener;
import org.overengineer.inlineproblems.settings.SettingsState;


public class ListenerManager {
    private final Logger logger = Logger.getInstance(ListenerManager.class);

    private final SettingsState settings = SettingsState.getInstance();

    private final DocumentMarkupModelScanner documentMarkupModelScanner = DocumentMarkupModelScanner.getInstance();

    private final ProblemManager problemManager;

    private static ListenerManager instance;

    public static ListenerManager getInstance() {
        if (instance == null)
            instance = new ListenerManager();

        return instance;
    }

    private ListenerManager() {
        problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);
    }

    public void installMarkupModelListenerOnAllProjects() {
        ProjectManager manager = ProjectManager.getInstanceIfCreated();
        if (manager != null) {
            for (var project : manager.getOpenProjects()) {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                for (var editor : fileEditorManager.getAllEditors()) {
                    if (editor instanceof TextEditor) {
                        MarkupModelProblemListener.setup((TextEditor) editor);
                        logger.debug("Installing MarkupModelListener");
                    }
                }
            }
        }
    }

    public void resetAndRescan() {
        problemManager.reset();
        documentMarkupModelScanner.scanForProblemsManually();
    }

    public void changeListener() {
        if (settings.getEnabledListener() != Listener.MARKUP_MODEL_LISTENER) {
            MarkupModelProblemListener.disposeAll();
        }

        if (settings.getEnabledListener() == Listener.MARKUP_MODEL_LISTENER) {
            documentMarkupModelScanner.setIsManualScanEnabled(false);
            installMarkupModelListenerOnAllProjects();
        }
        else if (settings.getEnabledListener() == Listener.HIGHLIGHT_PROBLEMS_LISTENER) {
            documentMarkupModelScanner.setIsManualScanEnabled(true);
            documentMarkupModelScanner.setDelayMilliseconds(HighlightProblemListener.ADDITIONAL_MANUAL_SCAN_DELAY_MILLIS);
        }
        else if (settings.getEnabledListener() == Listener.MANUAL_SCANNING) {
            documentMarkupModelScanner.setIsManualScanEnabled(true);
            documentMarkupModelScanner.setDelayMilliseconds(DocumentMarkupModelScanner.MANUAL_SCAN_DELAY_MILLIS);
        }
    }
}
