package org.overengineer.inlineproblems.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;


@Getter
@Setter
@State(
        name = "org.overengineer.inlineproblems.settings.SettingsState",
        storages = @Storage("OverEngineer_InlineProblems.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    private boolean showErrors = true;
    private boolean highlightErrors = true;
    private boolean showWarnings = false;
    private boolean highlightWarnings = false;
    private boolean showWeakWarnings = false;
    private boolean highlightWeakWarnings = false;
    private boolean showInfos = false;
    private boolean highlightInfos = false;
    private Color errorTextColor = new Color(0xEC5151);
    private Color errorBackgroundColor = new Color(0x654243);
    private Color errorHighlightColor = errorBackgroundColor;
    private Color warningTextColor = new Color(0xEC821F);
    private Color warningBackgroundColor = new Color(0x815125);
    private Color warningHighlightColor = warningBackgroundColor;
    private Color weakWarningTextColor = new Color(0xC07937);
    private Color weakWarningBackgroundColor = new Color(0xA47956);
    private Color weakWarningHighlightColor = weakWarningBackgroundColor;
    private Color infoTextColor = new Color(0x3BB1CE);
    private Color infoBackgroundColor = new Color(0x1E5664);
    private Color infoHighlightColor = infoBackgroundColor;
    private boolean drawBoxesAroundErrorLabels = true;
    private boolean roundedCornerBoxes = true;
    private boolean forceErrorsInSameLine = false;
    private int problemLineLengthOffsetPixels = 50;
    private String problemFilterList = "todo;fixme";

    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
