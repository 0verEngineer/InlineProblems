package org.overengineer.inlineproblems.listeners;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.ListenerManager;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Redraws all problems when the color scheme or the look and feel changes.
 * <p>
 * The drawn elements keep the colors they were created with: the inlay renderer copies them in
 * its constructor and the line highlighter takes the default foreground and background from the
 * scheme that was active while drawing. Without a redraw they stay in the old theme until the
 * problems happen to be re-created, which is what GitHub issue #61 reports for the automatic
 * dark/light switch.
 */
public class ThemeChangeListener implements EditorColorsListener, LafManagerListener {

    /* Static because the platform creates one listener instance per subscribed topic and a
     * theme switch usually fires both of them. */
    private static final AtomicBoolean redrawScheduled = new AtomicBoolean(false);

    @Override
    public void globalSchemeChange(@Nullable EditorColorsScheme scheme) {
        scheduleRedraw();
    }

    @Override
    public void lookAndFeelChanged(@NotNull LafManager source) {
        scheduleRedraw();
    }

    private void scheduleRedraw() {
        if (!SettingsState.getInstance().isEnableInlineProblem()) {
            return;
        }

        if (!redrawScheduled.compareAndSet(false, true)) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            redrawScheduled.set(false);
            ListenerManager.getInstance().resetAndRescan();
        });
    }
}
