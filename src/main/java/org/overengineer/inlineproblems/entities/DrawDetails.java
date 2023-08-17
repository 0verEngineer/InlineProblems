package org.overengineer.inlineproblems.entities;

import com.intellij.icons.AllIcons;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import lombok.Getter;
import org.overengineer.inlineproblems.settings.SettingsState;

import javax.swing.*;
import java.awt.*;


@Getter
public class DrawDetails {
    private Color textColor;
    private Color backgroundColor;
    private Color highlightColor;

    private Icon icon = null;
    
    private boolean isDrawProblem = false;
    private boolean isDrawHighlighter = false;
    
    public DrawDetails(InlineProblem problem, Editor editor) {
        SettingsState settings = SettingsState.getInstance();
        int severity = problem.getSeverity();

        textColor = editor.getColorsScheme().getDefaultForeground();
        backgroundColor = editor.getColorsScheme().getDefaultBackground();
        highlightColor = editor.getColorsScheme().getDefaultBackground();

        if (severity >= HighlightSeverity.ERROR.myVal) {
            textColor = settings.getErrorTextColor();
            backgroundColor = settings.getErrorBackgroundColor();
            highlightColor = settings.getErrorHighlightColor();
            isDrawHighlighter = settings.isHighlightErrors();
            isDrawProblem = settings.isShowErrors();
            if (settings.isShowErrorsInGutter())
                icon = AllIcons.General.Error;
        }
        else if (severity >= HighlightSeverity.WARNING.myVal) {
            textColor = settings.getWarningTextColor();
            backgroundColor = settings.getWarningBackgroundColor();
            highlightColor = settings.getWarningHighlightColor();
            isDrawHighlighter = settings.isHighlightWarnings();
            isDrawProblem = settings.isShowWarnings();
            if (settings.isShowWarningsInGutter())
                icon = AllIcons.General.Warning;
        }
        else if (severity >= HighlightSeverity.WEAK_WARNING.myVal) {
            textColor = settings.getWeakWarningTextColor();
            backgroundColor = settings.getWeakWarningBackgroundColor();
            highlightColor = settings.getWeakWarningHighlightColor();
            isDrawHighlighter = settings.isHighlightWeakWarnings();
            isDrawProblem = settings.isShowWeakWarnings();
            if (settings.isShowWeakWarningsInGutter())
                icon = AllIcons.General.Warning;
        }
        else if (severity >= HighlightSeverity.INFORMATION.myVal) {
            textColor = settings.getInfoTextColor();
            backgroundColor = settings.getInfoBackgroundColor();
            highlightColor = settings.getInfoHighlightColor();
            isDrawHighlighter = settings.isHighlightInfos();
            isDrawProblem = settings.isShowInfos();
            if (settings.isShowInfosInGutter())
                icon = AllIcons.General.Information;
        }
    }
}
