package org.overengineer.inlineproblems.listeners;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationInfo;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.ListenerManager;
import org.overengineer.inlineproblems.UnityProjectManager;
import org.overengineer.inlineproblems.entities.IDE;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.settings.SettingsState;


public class PluginListener implements DynamicPluginListener {
    private SettingsState settingsState = SettingsState.getInstance();

    private final ListenerManager listenerManager = ListenerManager.getInstance();

    private final UnityProjectManager projectManager = UnityProjectManager.getInstance();

    private final static String PLUGIN_ID = "org.overengineer.inlineproblems";

    @Override
    public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
        if (ApplicationInfo.getInstance().getFullApplicationName().startsWith(IDE.RIDER)) {
            projectManager.scanAllOpenProjectsForUnity();
        }

        if (settingsState.getEnabledListener() == Listener.MARKUP_MODEL_LISTENER) {
            if (descriptor.getPluginId().getIdString().equalsIgnoreCase(PLUGIN_ID)) {
                listenerManager.installMarkupModelListenerOnAllProjects();
            }
        }
    }
}
