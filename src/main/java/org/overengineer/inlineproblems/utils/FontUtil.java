package org.overengineer.inlineproblems.utils;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.util.ui.UIUtil;

import org.overengineer.inlineproblems.settings.SettingsState;

import java.awt.*;

public class FontUtil {
    public static Font getActiveFont(Editor editor) {
        SettingsState settingsState = SettingsState.getInstance();

        int appliedDelta = 0;
        int editorFontSize = editor.getColorsScheme().getEditorFontSize();

        if (editorFontSize > settingsState.getInlayFontSizeDelta()) {
            appliedDelta = settingsState.getInlayFontSizeDelta();
        }

        var fontType = EditorFontType.PLAIN;

        if (settingsState.isBoldProblemLabels() && settingsState.isItalicProblemLabels()) {
            fontType = EditorFontType.BOLD_ITALIC;
        }
        else if (settingsState.isItalicProblemLabels()) {
            fontType = EditorFontType.ITALIC;
        }
        else if (settingsState.isBoldProblemLabels()) {
            fontType = EditorFontType.BOLD;
        }

        if (settingsState.isUseEditorFont()) {
            return UIUtil.getFontWithFallback(
                    editor.getColorsScheme().getFont(fontType).getFontName(),
                    fontType.ordinal(),
                    editorFontSize - appliedDelta
            );
        }
        else {
            Font toolTipFont = UIUtil.getToolTipFont();
            return UIUtil.getFontWithFallback(
                    toolTipFont.getFontName(),
                    fontType.ordinal(),
                    editorFontSize - appliedDelta
            );
        }
    }
}
