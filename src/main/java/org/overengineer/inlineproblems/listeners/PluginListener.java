package org.overengineer.inlineproblems.listeners;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.overengineer.inlineproblems.ListenerManager;
import org.overengineer.inlineproblems.entities.enums.Listeners;
import org.overengineer.inlineproblems.settings.SettingsState;


public class PluginListener implements DynamicPluginListener {
    private SettingsState settingsState = SettingsState.getInstance();
    private final static String PLUGIN_ID = "org.overengineer.inlineproblems";
    private final ListenerManager listenerManager = ListenerManager.getInstance();

    @Override
    public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
        if (settingsState.getEnabledListener() != Listeners.MARKUP_MODEL_LISTENER)
            return;

        if (descriptor.getPluginId().getIdString().equalsIgnoreCase(PLUGIN_ID)) {
            listenerManager.installMarkupModelListenerOnAllProjects();
        }
    }
}
