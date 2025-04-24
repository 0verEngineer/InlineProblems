package org.overengineer.inlineproblems.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import lombok.Getter;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.bundles.SettingsBundle;
import org.overengineer.inlineproblems.listeners.HighlightProblemListener;
import org.overengineer.inlineproblems.listeners.MarkupModelProblemListener;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;


public class SettingsComponent {
    private final JBCheckBox showErrors = new JBCheckBox(SettingsBundle.message("settings.showErrors"));
    private final JBCheckBox highlightErrors = new JBCheckBox(SettingsBundle.message("settings.highlightErrors"));
    private final JBCheckBox showErrorsInGutter = new JBCheckBox(SettingsBundle.message("settings.showErrorsInGutter"));

    private final JBCheckBox showWarnings = new JBCheckBox(SettingsBundle.message("settings.showWarnings"));
    private final JBCheckBox highlightWarnings = new JBCheckBox(SettingsBundle.message("settings.highlightWarnings"));
    private final JBCheckBox showWarningsInGutter = new JBCheckBox(SettingsBundle.message("settings.showWarningsInGutter"));

    private final JBCheckBox showWeakWarnings = new JBCheckBox(SettingsBundle.message("settings.showWeakWarnings"));
    private final JBCheckBox highlightWeakWarnings = new JBCheckBox(SettingsBundle.message("settings.highlightWeakWarnings"));
    private final JBCheckBox showWeakWarningsInGutter = new JBCheckBox(SettingsBundle.message("settings.showWeakWarningsInGutter"));

    private final JBCheckBox showInfos = new JBCheckBox(SettingsBundle.message("settings.showInfos"));
    private final JBCheckBox highlightInfo = new JBCheckBox(SettingsBundle.message("settings.highlightInfos"));
    private final JBCheckBox showInfosInGutter = new JBCheckBox(SettingsBundle.message("settings.showInfosInGutter"));

    private final ColorPanel errorTextColor = new ColorPanel();
    private final ColorPanel errorLabelBackgroundColor = new ColorPanel();
    private final ColorPanel errorHighlightColor = new ColorPanel();

    private final ColorPanel warningTextColor = new ColorPanel();
    private final ColorPanel warningLabelBackgroundColor = new ColorPanel();
    private final ColorPanel warningHighlightColor = new ColorPanel();

    private final ColorPanel weakWarningTextColor = new ColorPanel();
    private final ColorPanel weakWarningLabelBackgroundColor = new ColorPanel();
    private final ColorPanel weakWarningHighlightColor = new ColorPanel();

    private final ColorPanel infoTextColor = new ColorPanel();
    private final ColorPanel infoLabelBackgroundColor = new ColorPanel();
    private final ColorPanel infoHighlightColor = new ColorPanel();

    private final JBCheckBox forceErrorsInSameLine = new JBCheckBox(SettingsBundle.message("settings.forceProblemsInOneLine"));
    private final JBCheckBox drawBoxesAroundProblemLabels = new JBCheckBox(SettingsBundle.message("settings.drawBoxesAroundProblemLabels"));
    private final JBCheckBox roundedCornerBoxes = new JBCheckBox(SettingsBundle.message("settings.roundedCornerBoxes"));
    private final JBCheckBox useEditorFont = new JBCheckBox(SettingsBundle.message("settings.useEditorFont"));

    private final JBCheckBox showOnlyHighestSeverityPerLine = new JBCheckBox(SettingsBundle.message("settings.showOnlyHighestPerLine"));
    private final JBCheckBox enableHtmlStripping = new JBCheckBox(SettingsBundle.message("settings.enableHtmlStripping"));
    private final JBCheckBox enableXmlUnescaping = new JBCheckBox(SettingsBundle.message("settings.enableXmlUnescaping"));
    private final JFormattedTextField inlayFontSizeDeltaText;
    private final JFormattedTextField manualScannerDelay;
    private final JBCheckBox fillProblemLabels = new JBCheckBox(SettingsBundle.message("settings.fillProblemLabels"));
    private final JBCheckBox boldProblemLabels = new JBCheckBox(SettingsBundle.message("settings.boldProblemLabels"));
    private final JBCheckBox italicProblemLabels = new JBCheckBox(SettingsBundle.message("settings.italicProblemLabels"));
    private final JBCheckBox enableHovering = new JBCheckBox(SettingsBundle.message("settings.enableHovering"));

    private final JBTextField problemFilterList = new JBTextField();
    private final JBTextField fileExtensionBlacklist = new JBTextField();

    private final String[] availableListeners = {HighlightProblemListener.NAME, MarkupModelProblemListener.NAME, DocumentMarkupModelScanner.NAME};
    private final JComboBox<String> enabledListener = new ComboBox<>(availableListeners);

    private final JBTextField additionalInfoSeverities = new JBTextField();
    private final JBTextField additionalWarningSeverities = new JBTextField();
    private final JBTextField additionalWeakWarningSeverities = new JBTextField();
    private  final JBTextField additionalErrorSeverities = new JBTextField();

    @Getter
    private final JPanel settingsPanel;

    public SettingsComponent() {
        SettingsState settingsState = SettingsState.getInstance();

        showErrors.setSelected(settingsState.isShowErrors());
        highlightErrors.setSelected(settingsState.isHighlightErrors());
        showErrorsInGutter.setSelected(settingsState.isShowErrorsInGutter());
        errorTextColor.setSelectedColor(settingsState.getErrorTextColor());
        errorLabelBackgroundColor.setSelectedColor(settingsState.getErrorBackgroundColor());
        errorHighlightColor.setSelectedColor(settingsState.getErrorHighlightColor());

        showWarnings.setSelected(settingsState.isShowWarnings());
        highlightWarnings.setSelected(settingsState.isHighlightWarnings());
        showWarningsInGutter.setSelected(settingsState.isShowWarningsInGutter());
        warningTextColor.setSelectedColor(settingsState.getWarningTextColor());
        warningLabelBackgroundColor.setSelectedColor(settingsState.getWarningBackgroundColor());
        warningHighlightColor.setSelectedColor(settingsState.getWarningHighlightColor());

        showWeakWarnings.setSelected(settingsState.isShowWeakWarnings());
        highlightWeakWarnings.setSelected(settingsState.isHighlightWeakWarnings());
        showWeakWarningsInGutter.setSelected(settingsState.isShowWeakWarningsInGutter());
        weakWarningTextColor.setSelectedColor(settingsState.getWeakWarningTextColor());
        weakWarningLabelBackgroundColor.setSelectedColor(settingsState.getWeakWarningBackgroundColor());
        weakWarningHighlightColor.setSelectedColor(settingsState.getWeakWarningHighlightColor());

        showInfos.setSelected(settingsState.isShowInfos());
        highlightInfo.setSelected(settingsState.isHighlightInfos());
        showInfosInGutter.setSelected(settingsState.isShowInfosInGutter());
        infoTextColor.setSelectedColor(settingsState.getInfoTextColor());
        infoLabelBackgroundColor.setSelectedColor(settingsState.getInfoBackgroundColor());
        infoHighlightColor.setSelectedColor(settingsState.getInfoHighlightColor());

        forceErrorsInSameLine.setSelected(settingsState.isForceProblemsInSameLine());
        drawBoxesAroundProblemLabels.setSelected(settingsState.isDrawBoxesAroundErrorLabels());
        roundedCornerBoxes.setSelected(settingsState.isRoundedCornerBoxes());

        useEditorFont.setSelected(settingsState.isUseEditorFont());

        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        intFormat.setGroupingUsed(false);
        NumberFormatter numberFormatter = new NumberFormatter(intFormat);
        numberFormatter.setValueClass(Integer.class); // Optional, ensures we always get a int value

        inlayFontSizeDeltaText = new JFormattedTextField(numberFormatter);
        inlayFontSizeDeltaText.setText(Integer.toString(settingsState.getInlayFontSizeDelta()));

        manualScannerDelay = new JFormattedTextField(numberFormatter);
        manualScannerDelay.setText(Integer.toString(settingsState.getManualScannerDelay()));

        fillProblemLabels.setSelected(settingsState.isFillProblemLabels());
        boldProblemLabels.setSelected(settingsState.isBoldProblemLabels());
        italicProblemLabels.setSelected(settingsState.isItalicProblemLabels());
        enableHovering.setSelected(settingsState.isHovering());

        additionalInfoSeverities.setText(settingsState.getAdditionalInfoSeveritiesAsString());
        additionalWeakWarningSeverities.setText(settingsState.getAdditionalWeakWarningSeveritiesAsString());
        additionalWarningSeverities.setText(settingsState.getAdditionalWarningSeveritiesAsString());
        additionalErrorSeverities.setText(settingsState.getAdditionalErrorSeveritiesAsString());

        problemFilterList.setText(settingsState.getProblemFilterList());
        fileExtensionBlacklist.setText(settingsState.getFileExtensionBlacklist());
        enabledListener.setSelectedItem(Optional.of(settingsState.getEnabledListener()));

        Dimension enabledListenerDimension = enabledListener.getPreferredSize();
        enabledListenerDimension.width += 100;
        enabledListener.setPreferredSize(enabledListenerDimension);

        settingsPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel(SettingsBundle.message("settings.submenu.label")))
                .addComponent(drawBoxesAroundProblemLabels, 0)
                .addComponent(roundedCornerBoxes, 0)
                .addComponent(fillProblemLabels, 0)
                .addComponent(boldProblemLabels, 0)
                .addComponent(italicProblemLabels, 0)
                .addComponent(enableHovering, 0)
                .addSeparator()
                .addComponent(new JBLabel(SettingsBundle.message("settings.submenu.general")))
                .addLabeledComponent(new JBLabel(SettingsBundle.message("settings.activeProblemListener")), enabledListener)
                .addTooltip(SettingsBundle.message("settings.markupModelListenerDescription"))
                .addTooltip(SettingsBundle.message("settings.highlightProblemListenerDescription"))
                .addTooltip(SettingsBundle.message("settings.manualScannerDescription"))
                .addTooltip(SettingsBundle.message("settings.manualScannerDescriptionSupplement"))
                .addLabeledComponent(new JBLabel(SettingsBundle.message("settings.manualScannerDelayLabel")), manualScannerDelay)
                .addTooltip(SettingsBundle.message("settings.manualScannerDelayTooltip"))
                .addComponent(forceErrorsInSameLine, 0)
                .addComponent(useEditorFont, 0)
                .addComponent(showOnlyHighestSeverityPerLine, 0)
                .addComponent(enableHtmlStripping, 0)
                .addComponent(enableXmlUnescaping, 0)
                .addLabeledComponent(new JBLabel(SettingsBundle.message("settings.inlaySizeDelta")), inlayFontSizeDeltaText)
                .addTooltip(SettingsBundle.message("settings.inlaySizeDeltaTooltip"))
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.problemFilterListLabel")), problemFilterList)
                .addTooltip(SettingsBundle.message("settings.problemFilterListTooltip"))
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.fileExtensionBlacklistLabel")), fileExtensionBlacklist)
                .addTooltip(SettingsBundle.message("settings.fileExtensionBlaclistTooltip"))
                .addSeparator()
                .addComponent(new JBLabel(SettingsBundle.message("settings.submenu.colors")))
                .addComponent(showErrors)
                .addComponent(highlightErrors)
                .addComponent(showErrorsInGutter)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.errorTextColor")), errorTextColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.errorLabelBorderColor")), errorLabelBackgroundColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.errorLineHighlightColor")), errorHighlightColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.additionalSeverities")), additionalErrorSeverities)
                .addTooltip(SettingsBundle.message("settings.additionalSeveritiesErrorDesc"))
                .addSeparator()
                .addComponent(showWarnings)
                .addComponent(highlightWarnings)
                .addComponent(showWarningsInGutter)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.warningTextColor")), warningTextColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.warningLabelBorderColor")), warningLabelBackgroundColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.warningLineHighlightColor")), warningHighlightColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.additionalSeverities")), additionalWarningSeverities)
                .addTooltip(SettingsBundle.message("settings.additionalSeveritiesWarningDesc"))
                .addSeparator()
                .addComponent(showWeakWarnings)
                .addComponent(highlightWeakWarnings)
                .addComponent(showWeakWarningsInGutter)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.weakWarningTextColor")), weakWarningTextColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.weakWarningLabelBorderColor")), weakWarningLabelBackgroundColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.weakWarningLineHighlightColor")), weakWarningHighlightColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.additionalSeverities")), additionalWeakWarningSeverities)
                .addTooltip(SettingsBundle.message("settings.additionalSeveritiesWeakWarningDesc"))
                .addSeparator()
                .addComponent(showInfos)
                .addComponent(highlightInfo)
                .addComponent(showInfosInGutter)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.infoTextColor")), infoTextColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.infoLabelBorderColor")), infoLabelBackgroundColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.infoLineHighlightColor")), infoHighlightColor)
                .addLabeledComponent(new JLabel(SettingsBundle.message("settings.additionalSeverities")), additionalInfoSeverities)
                .addTooltip(SettingsBundle.message("settings.additionalSeveritiesInfoDesc"))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JComponent getPreferredFocusedComponent() {
        return settingsPanel;
    }

    public boolean isForceErrorsInSameLine() {
        return forceErrorsInSameLine.isSelected();
    }

    public void setForceErrorsInSameLine(final boolean isSelected) {
        forceErrorsInSameLine.setSelected(isSelected);
    }

    public boolean getDrawBoxesAroundProblemLabels() {
        return drawBoxesAroundProblemLabels.isSelected();
    }

    public void setDrawBoxesAroundProblemLabels(final boolean isSelected) {
        drawBoxesAroundProblemLabels.setSelected(isSelected);
    }

    public boolean isRoundedCornerBoxes() {
        return roundedCornerBoxes.isSelected();
    }

    public void setRoundedCornerBoxes(boolean isSelected) {
        roundedCornerBoxes.setSelected(isSelected);
    }

    public boolean isUseEditorFont() {
        return useEditorFont.isSelected();
    }

    public boolean isShowOnlyHighestSeverityPerLine() {
        return showOnlyHighestSeverityPerLine.isSelected();
    }

    public boolean isEnableHtmlStripping() {
        return enableHtmlStripping.isSelected();
    }

    public boolean isEnableXmlUnescaping() {
        return enableXmlUnescaping.isSelected();
    }

    public int getInlayFontSizeDelta() {
        int val = 0;
        // Convert the String
        try {
            val = Integer.parseInt(inlayFontSizeDeltaText.getText());
        }
        catch (NumberFormatException ignored) {}

        if (val < 0)
            val = 0;

        return val;
    }

    public void setInlayFontSizeDelta(int val) {
        inlayFontSizeDeltaText.setText(String.valueOf(Math.max(0, val)));
    }

    public void setUseEditorFont(boolean isSelected) {
        useEditorFont.setSelected(isSelected);
    }

    public void setShowOnlyHighestSeverityPerLine(boolean isSelected) {
        showOnlyHighestSeverityPerLine.setSelected(isSelected);
    }

    public void setEnableHtmlStripping(boolean isSelected) {
        enableHtmlStripping.setSelected(isSelected);
    }

    public void setEnableXmlUnescaping(boolean isSelected) {
        enableXmlUnescaping.setSelected(isSelected);
    }

    public boolean isFillProblemLabels() {
        return fillProblemLabels.isSelected();
    }

    public void setFillProblemLabels(boolean isSelected) {
        fillProblemLabels.setSelected(isSelected);
    }

    public boolean isBoldProblemLabels() {
        return boldProblemLabels.isSelected();
    }

    public void setBoldProblemLabels(boolean isSelected) {
        boldProblemLabels.setSelected(isSelected);
    }

    public boolean isItalicProblemLabels() {
        return italicProblemLabels.isSelected();
    }

    public void setItalicProblemLabels(boolean isSelected) {
        italicProblemLabels.setSelected(isSelected);
    }

    public boolean isHovering() {
        return enableHovering.isSelected();
    }
    public void setHovering(boolean isSelected) {
        enableHovering.setSelected(isSelected);
    }

    public boolean isShowErrors() {
        return showErrors.isSelected();
    }

    public void setShowErrors(final boolean isSelected) {
        showErrors.setSelected(isSelected);
    }

    public boolean isHighlightErrors() {
        return highlightErrors.isSelected();
    }

    public void setHighlightErrors(boolean isSelected) {
        highlightErrors.setSelected(isSelected);
    }

    public boolean isShowErrorsInGutter() {
        return showErrorsInGutter.isSelected();
    }

    public void setShowErrorsInGutter(boolean isSelected) {
        showErrorsInGutter.setSelected(isSelected);
    }

    public boolean isShowWarnings() {
        return showWarnings.isSelected();
    }

    public void setShowWarnings(final boolean isSelected) {
        showWarnings.setSelected(isSelected);
    }

    public boolean isHighlightWarnings() {
        return highlightWarnings.isSelected();
    }

    public void setHighlightWarnings(final boolean isSelected) {
        highlightWarnings.setSelected(isSelected);
    }

    public boolean isShowWarningsInGutter() {
        return showWarningsInGutter.isSelected();
    }

    public void setShowWarningsInGutter(final boolean isSelected) {
        showWarningsInGutter.setSelected(isSelected);
    }

    public boolean isShowWeakWarnings() {
        return showWeakWarnings.isSelected();
    }

    public void setShowWeakWarnings(final boolean isSelected) {
        showWeakWarnings.setSelected(isSelected);
    }

    public boolean isHighlightWeakWarnings() {
        return highlightWeakWarnings.isSelected();
    }

    public void setHighlightWeakWarnings(final boolean isSelected) {
        highlightWeakWarnings.setSelected(isSelected);
    }

    public boolean isShowWeakWarningsInGutter() {
        return showWeakWarningsInGutter.isSelected();
    }

    public void setShowWeakWarningsInGutter(boolean isSelected) {
        showWeakWarningsInGutter.setSelected(isSelected);
    }

    public boolean isShowInfos() {
        return showInfos.isSelected();
    }

    public void setShowInfos(final boolean isSelected) {
        showInfos.setSelected(isSelected);
    }

    public boolean isHighlightInfo() {
        return highlightInfo.isSelected();
    }

    public void setHighlightInfo(final boolean isSelected) {
        highlightInfo.setSelected(isSelected);
    }

    public boolean isShowInfosInGutter() {
        return showInfosInGutter.isSelected();
    }

    public void setShowInfosInGutter(boolean isSelected) {
        showInfosInGutter.setSelected(isSelected);
    }

    public Color getErrorTextColor() {
        return errorTextColor.getSelectedColor();
    }

    public void setErrorTextColor(final Color color) {
        errorTextColor.setSelectedColor(color);
    }

    public Color getErrorLabelBackgroundColor() {
        return errorLabelBackgroundColor.getSelectedColor();
    }

    public void setErrorLabelBackgroundColor(final Color color) {
        errorLabelBackgroundColor.setSelectedColor(color);
    }

    public Color getWarningTextColor() {
        return warningTextColor.getSelectedColor();
    }

    public void setWarningTextColor(final Color color) {
        warningTextColor.setSelectedColor(color);
    }

    public Color getWarningLabelBackgroundColor() {
        return warningLabelBackgroundColor.getSelectedColor();
    }

    public void setWarningLabelBackgroundColor(final Color color) {
        warningLabelBackgroundColor.setSelectedColor(color);
    }

    public Color getWeakWarningTextColor() {
        return weakWarningTextColor.getSelectedColor();
    }

    public void setWeakWarningTextColor(final Color color) {
        weakWarningTextColor.setSelectedColor(color);
    }

    public Color getWeakWarningLabelBackgroundColor() {
        return weakWarningLabelBackgroundColor.getSelectedColor();
    }

    public void setWeakWarningLabelBackgroundColor(final Color color) {
        weakWarningLabelBackgroundColor.setSelectedColor(color);
    }

    public Color getInfoTextColor() {
        return infoTextColor.getSelectedColor();
    }

    public void setInfoTextColor(final Color color) {
        infoTextColor.setSelectedColor(color);
    }

    public Color getInfoLabelBackgroundColor() {
        return infoLabelBackgroundColor.getSelectedColor();
    }

    public void setInfoLabelBackgroundColor(final Color color) {
        infoLabelBackgroundColor.setSelectedColor(color);
    }

    public Color getErrorHighlightColor() {
        return errorHighlightColor.getSelectedColor();
    }

    public void setErrorHighlightColor(final Color color) {
        errorHighlightColor.setSelectedColor(color);
    }

    public Color getWarningHighlightColor() {
        return warningHighlightColor.getSelectedColor();
    }

    public void setWarningHighlightColor(final Color color) {
        warningHighlightColor.setSelectedColor(color);
    }

    public Color getWeakWarningHighlightColor() {
        return weakWarningHighlightColor.getSelectedColor();
    }

    public void setWeakWarningHighlightColor(final Color color) {
        weakWarningHighlightColor.setSelectedColor(color);
    }

    public Color getInfoHighlightColor() {
        return infoHighlightColor.getSelectedColor();
    }

    public void setInfoHighlightColor(final Color color) {
        infoHighlightColor.setSelectedColor(color);
    }

    public String getProblemFilterList() {
        return problemFilterList.getText();
    }

    public void setProblemFilterList(final String newText) {
        problemFilterList.setText(newText);
    }

    public String getFileExtensionBlacklist() {
        return fileExtensionBlacklist.getText();
    }

    public void setFileExtensionBlacklist(final String newText) {
        fileExtensionBlacklist.setText(newText);
    }

    public String getAdditionalInfoSeverities() {
        return additionalInfoSeverities.getText();
    }

    public List<Integer> getAdditionalInfoSeveritiesList() {
        return getSeverityIntegerList(additionalInfoSeverities.getText());
    }

    public void setAdditionalInfoSeverities(final String newText) {
        additionalInfoSeverities.setText(newText);
    }

    public String getAdditionalWarningSeverities() {
        return additionalWarningSeverities.getText();
    }

    public List<Integer> getAdditionalWarningSeveritiesList() {
        return getSeverityIntegerList(additionalWarningSeverities.getText());
    }

    public void setAdditionalWarningSeverities(final String newText) {
        additionalWarningSeverities.setText(newText);
    }

    public String getAdditionalErrorSeverities() {
        return additionalErrorSeverities.getText();
    }

    public List<Integer> getAdditionalErrorSeveritiesList() {
        return getSeverityIntegerList(additionalErrorSeverities.getText());
    }

    public void setAdditionalErrorSeverities(final String newText) {
        additionalErrorSeverities.setText(newText);
    }

    public String getAdditionalWeakWarningSeverities() {
        return additionalWeakWarningSeverities.getText();
    }

    public List<Integer> getAdditionalWeakWarningSeveritiesList() {
        return getSeverityIntegerList(additionalWeakWarningSeverities.getText());
    }

    public void setAdditionalWeakWarningSeverities(final String newText) {
        additionalWeakWarningSeverities.setText(newText);
    }

    public int getEnabledListener() {
        return enabledListener.getSelectedIndex();
    }

    public void setEnabledListener(int index) {
        enabledListener.setSelectedIndex(index);
    }

    private List<Integer> getSeverityIntegerList(String text) {
        return Arrays.stream(text.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> {
                    try {
                        Integer.parseInt(s);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public int getManualScannerDelay() {
        try {
            return Math.max(Integer.parseInt(manualScannerDelay.getText()), 10);
        }
        catch (NumberFormatException e) {
            return 100;
        }
    }

    public void setManualScannerDelay(int delay) {
        manualScannerDelay.setText(Integer.toString(Math.max(10, delay)));
    }
}
