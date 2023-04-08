package org.overengineer.inlineproblems.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Transient;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.entities.enums.Listener;
import org.overengineer.inlineproblems.utils.ColorConverter;

import java.awt.*;


@Getter
@Setter
@State(
        name = "org.overengineer.inlineproblems.settings.SettingsState",
        storages = @Storage("OverEngineer_InlineProblems.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    private boolean showErrors = true;
    private boolean highlightErrors = false;
    private boolean showWarnings = true;
    private boolean highlightWarnings = false;
    private boolean showWeakWarnings = false;
    private boolean highlightWeakWarnings = false;
    private boolean showInfos = false;
    private boolean highlightInfos = false;

    /**
     * Colors renamed from '<NAME>Color' to '<NAME>Col' to solve
     * the compatibility issue with old version persisted xml setting file.
     *
     * @see <a href="https://github.com/0verEngineer/InlineProblems/pull/10">Github discussion</a>
     */

    @OptionTag(converter = ColorConverter.class)
    private Color errorBackgroundCol = new Color(0x654243);

    @OptionTag(converter = ColorConverter.class)
    private Color errorTextCol = new Color(0xEC5151);

    @OptionTag(converter = ColorConverter.class)
    private Color errorHighlightCol = errorBackgroundCol;

    @OptionTag(converter = ColorConverter.class)
    private Color warningTextCol = new Color(0xEC821F);

    @OptionTag(converter = ColorConverter.class)
    private Color warningBackgroundCol = new Color(0x815125);

    @OptionTag(converter = ColorConverter.class)
    private Color warningHighlightCol = warningBackgroundCol;

    @OptionTag(converter = ColorConverter.class)
    private Color weakWarningTextCol= new Color(0xC07937);

    @OptionTag(converter = ColorConverter.class)
    private Color weakWarningBackgroundCol = new Color(0xA47956);

    @OptionTag(converter = ColorConverter.class)
    private Color weakWarningHighlightCol = weakWarningBackgroundCol;

    @OptionTag(converter = ColorConverter.class)
    private Color infoTextCol = new Color(0x3BB1CE);

    @OptionTag(converter = ColorConverter.class)
    private Color infoBackgroundCol = new Color(0x1E5664);

    @OptionTag(converter = ColorConverter.class)
    private Color infoHighlightCol = infoBackgroundCol;

    private boolean drawBoxesAroundErrorLabels = true;
    private boolean roundedCornerBoxes = true;
    private boolean forceProblemsInSameLine = true;
    private boolean useEditorFont = false;
    private int inlayFontSizeDelta = 0;
    private boolean fillProblemLabels = false;
    private boolean boldProblemLabels = false;
    private boolean italicProblemLabels = false;
    private int problemLineLengthOffsetPixels = 50;
    private int enabledListener = Listener.HIGHLIGHT_PROBLEMS_LISTENER;
    private String problemFilterList = "todo;fixme;open in browser";

    private boolean showOnlyHighestSeverityPerLine = false;

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

    //<editor-fold desc="Handwritten Colors getter/setter to compatible with external callers.">
    @Transient
    public Color getErrorTextColor() {
        return errorTextCol;
    }

    @Transient
    public void setErrorTextColor(Color color) {
        this.errorTextCol = color;
    }

    @Transient
    public Color getErrorBackgroundColor() {
        return errorBackgroundCol;
    }

    @Transient
    public void setErrorBackgroundColor(Color color) {
        this.errorBackgroundCol = color;
    }

    @Transient
    public Color getErrorHighlightColor() {
        return errorHighlightCol;
    }

    @Transient
    public void setErrorHighlightColor(Color color) {
        this.errorHighlightCol = color;
    }

    @Transient
    public Color getWarningTextColor() {
        return warningTextCol;
    }

    @Transient
    public void setWarningTextColor(Color color) {
        this.warningTextCol = color;
    }

    @Transient
    public Color getWarningBackgroundColor() {
        return warningBackgroundCol;
    }

    @Transient
    public void setWarningBackgroundColor(Color color) {
        this.warningBackgroundCol = color;
    }

    @Transient
    public Color getWarningHighlightColor() {
        return warningHighlightCol;
    }

    @Transient
    public void setWarningHighlightColor(Color color) {
        this.warningHighlightCol = color;
    }

    @Transient
    public Color getWeakWarningTextColor() {
        return weakWarningTextCol;
    }

    @Transient
    public void setWeakWarningTextColor(Color color) {
        this.weakWarningTextCol = color;
    }

    @Transient
    public Color getWeakWarningBackgroundColor() {
        return weakWarningBackgroundCol;
    }

    @Transient
    public void setWeakWarningBackgroundColor(Color color) {
        this.weakWarningBackgroundCol = color;
    }

    @Transient
    public Color getWeakWarningHighlightColor() {
        return weakWarningHighlightCol;
    }

    @Transient
    public void setWeakWarningHighlightColor(Color color) {
        this.weakWarningHighlightCol = color;
    }

    @Transient
    public Color getInfoTextColor() {
        return infoTextCol;
    }

    @Transient
    public void setInfoTextColor(Color color) {
        this.infoTextCol = color;
    }

    @Transient
    public Color getInfoBackgroundColor() {
        return infoBackgroundCol;
    }

    @Transient
    public void setInfoBackgroundColor(Color color) {
        this.infoBackgroundCol = color;
    }

    @Transient
    public Color getInfoHighlightColor() {
        return infoHighlightCol;
    }

    @Transient
    public void setInfoHighlightColor(Color color) {
        this.infoHighlightCol = color;
    }
    //</editor-fold>
}
