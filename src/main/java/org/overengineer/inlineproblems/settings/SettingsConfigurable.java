package org.overengineer.inlineproblems.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.DocumentMarkupModelScanner;
import org.overengineer.inlineproblems.ListenerManager;
import org.overengineer.inlineproblems.ProblemManager;
import org.overengineer.inlineproblems.entities.enums.Listeners;
import org.overengineer.inlineproblems.listeners.HighlightProblemListener;

import javax.swing.*;


public class SettingsConfigurable implements Configurable {

    private SettingsComponent settingsComponent;

    private final ProblemManager problemManager;

    private final DocumentMarkupModelScanner documentMarkupModelScanner = DocumentMarkupModelScanner.getInstance();
    private final ListenerManager listenerManager = ListenerManager.getInstance();

    SettingsConfigurable() {
        problemManager = ApplicationManager.getApplication().getService(ProblemManager.class);
    }

    @Override
    @NlsContexts.ConfigurableName
    public String getDisplayName() {
        return "InlineProblems";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }

    @Override
    @Nullable
    public JComponent createComponent() {
        settingsComponent = new SettingsComponent();
        return settingsComponent.getSettingsPanel();
    }

    @Override
    public boolean isModified() {
        SettingsState state = SettingsState.getInstance();

        boolean oldStateEqualsNewState = state.isForceProblemsInSameLine() == settingsComponent.isForceErrorsInSameLine() &&
                state.isDrawBoxesAroundErrorLabels() == settingsComponent.getDrawBoxesAroundProblemLabels() &&
                state.isRoundedCornerBoxes() == settingsComponent.isRoundedCornerBoxes() &&
                state.isUseEditorFont() == settingsComponent.isUseEditorFont() &&
                state.isFillProblemLabels() == settingsComponent.isFillProblemLabels() &&

                state.getErrorTextColor().equals(settingsComponent.getErrorTextColor()) &&
                state.getErrorBackgroundColor().equals(settingsComponent.getErrorLabelBackgroundColor()) &&
                state.getErrorHighlightColor().equals(settingsComponent.getErrorHighlightColor()) &&
                state.isShowErrors() == settingsComponent.isShowErrors() &&
                state.isHighlightErrors() == settingsComponent.isHighlightErrors() &&

                state.getWarningTextColor().equals(settingsComponent.getWarningTextColor()) &&
                state.getWarningBackgroundColor().equals(settingsComponent.getWarningLabelBackgroundColor()) &&
                state.getWarningHighlightColor().equals(settingsComponent.getWarningHighlightColor()) &&
                state.isShowWarnings() == settingsComponent.isShowWarnings() &&
                state.isHighlightWarnings() == settingsComponent.isHighlightWarnings() &&

                state.getWeakWarningTextColor().equals(settingsComponent.getWeakWarningTextColor()) &&
                state.getWeakWarningBackgroundColor().equals(settingsComponent.getWeakWarningLabelBackgroundColor()) &&
                state.getWeakWarningHighlightColor().equals(settingsComponent.getWeakWarningHighlightColor()) &&
                state.isShowWeakWarnings() == settingsComponent.isShowWeakWarnings() &&
                state.isHighlightWeakWarnings() == settingsComponent.isHighlightWeakWarnings() &&

                state.getInfoTextColor().equals(settingsComponent.getInfoTextColor()) &&
                state.getInfoBackgroundColor().equals(settingsComponent.getInfoLabelBackgroundColor()) &&
                state.getInfoHighlightColor().equals(settingsComponent.getInfoHighlightColor()) &&
                state.isShowInfos() == settingsComponent.isShowInfos() &&
                state.isHighlightInfos() == settingsComponent.isHighlightInfo() &&
                state.getEnabledListener() == settingsComponent.getEnabledListener() &&

                state.getProblemFilterList().equals(settingsComponent.getProblemFilterList());

        return !oldStateEqualsNewState;
    }

    @Override
    public void apply() {
        SettingsState state = SettingsState.getInstance();

        boolean listenerChanged = state.getEnabledListener() != settingsComponent.getEnabledListener();

        state.setShowErrors(settingsComponent.isShowErrors());
        state.setHighlightErrors(settingsComponent.isHighlightErrors());
        state.setErrorTextColor(settingsComponent.getErrorTextColor());
        state.setErrorBackgroundColor(settingsComponent.getErrorLabelBackgroundColor());
        state.setErrorHighlightColor(settingsComponent.getErrorHighlightColor());

        state.setShowWarnings(settingsComponent.isShowWarnings());
        state.setHighlightWarnings(settingsComponent.isHighlightWarnings());
        state.setWarningTextColor(settingsComponent.getWarningTextColor());
        state.setWarningBackgroundColor(settingsComponent.getWarningLabelBackgroundColor());
        state.setWarningHighlightColor(settingsComponent.getWarningHighlightColor());

        state.setShowWeakWarnings(settingsComponent.isShowWeakWarnings());
        state.setHighlightWeakWarnings(settingsComponent.isHighlightWeakWarnings());
        state.setWeakWarningTextColor(settingsComponent.getWeakWarningTextColor());
        state.setWeakWarningBackgroundColor(settingsComponent.getWeakWarningLabelBackgroundColor());
        state.setWeakWarningHighlightColor(settingsComponent.getWeakWarningHighlightColor());

        state.setShowInfos(settingsComponent.isShowInfos());
        state.setHighlightInfos(settingsComponent.isHighlightInfo());
        state.setInfoTextColor(settingsComponent.getInfoTextColor());
        state.setInfoBackgroundColor(settingsComponent.getInfoLabelBackgroundColor());
        state.setInfoHighlightColor(settingsComponent.getInfoHighlightColor());

        state.setForceProblemsInSameLine(settingsComponent.isForceErrorsInSameLine());
        state.setDrawBoxesAroundErrorLabels(settingsComponent.getDrawBoxesAroundProblemLabels());
        state.setRoundedCornerBoxes(settingsComponent.isRoundedCornerBoxes());
        state.setUseEditorFont(settingsComponent.isUseEditorFont());
        state.setFillProblemLabels(settingsComponent.isFillProblemLabels());

        state.setEnabledListener(settingsComponent.getEnabledListener());
        state.setProblemFilterList(settingsComponent.getProblemFilterList());

        if (listenerChanged && state.getEnabledListener() == Listeners.MARKUP_MODEL_LISTENER) {
            documentMarkupModelScanner.setIsManualScanEnabled(false);
            listenerManager.installMarkupModelListenerOnAllProjects();
        }
        else if (listenerChanged && state.getEnabledListener() == Listeners.HIGHLIGHT_PROBLEMS_LISTENER) {
            documentMarkupModelScanner.setIsManualScanEnabled(true);
            documentMarkupModelScanner.setFrequencyMilliseconds(HighlightProblemListener.MANUAL_SCAN_FREQUENCY_MILLIS);
        }

        problemManager.reset();
        documentMarkupModelScanner.scanForProblemsManually();
    }

    @Override
    public void reset() {
        SettingsState state = SettingsState.getInstance();

        settingsComponent.setShowErrors(state.isShowErrors());
        settingsComponent.setHighlightErrors(state.isHighlightErrors());
        settingsComponent.setErrorTextColor(state.getErrorTextColor());
        settingsComponent.setErrorLabelBackgroundColor(state.getErrorBackgroundColor());
        settingsComponent.setErrorHighlightColor(state.getErrorHighlightColor());

        settingsComponent.setShowWarnings(state.isShowWarnings());
        settingsComponent.setHighlightWarnings(state.isHighlightWarnings());
        settingsComponent.setWarningTextColor(state.getWarningTextColor());
        settingsComponent.setWarningLabelBackgroundColor(state.getWarningBackgroundColor());
        settingsComponent.setWarningHighlightColor(state.getWarningHighlightColor());

        settingsComponent.setShowWeakWarnings(state.isShowWeakWarnings());
        settingsComponent.setHighlightWeakWarnings(state.isHighlightWeakWarnings());
        settingsComponent.setWeakWarningTextColor(state.getWeakWarningTextColor());
        settingsComponent.setWeakWarningLabelBackgroundColor(state.getWeakWarningBackgroundColor());
        settingsComponent.setWeakWarningHighlightColor(state.getWeakWarningHighlightColor());

        settingsComponent.setShowInfos(state.isShowInfos());
        settingsComponent.setHighlightInfo(state.isHighlightInfos());
        settingsComponent.setInfoTextColor(state.getInfoTextColor());
        settingsComponent.setInfoLabelBackgroundColor(state.getInfoBackgroundColor());
        settingsComponent.setInfoHighlightColor(state.getInfoHighlightColor());

        settingsComponent.setForceErrorsInSameLine(state.isForceProblemsInSameLine());
        settingsComponent.setDrawBoxesAroundProblemLabels(state.isDrawBoxesAroundErrorLabels());
        settingsComponent.setRoundedCornerBoxes(state.isRoundedCornerBoxes());
        settingsComponent.setUseEditorFont(state.isUseEditorFont());
        settingsComponent.setFillProblemLabels(state.isFillProblemLabels());

        settingsComponent.setEnabledListener(state.getEnabledListener());
        settingsComponent.setProblemFilterList(state.getProblemFilterList());
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
