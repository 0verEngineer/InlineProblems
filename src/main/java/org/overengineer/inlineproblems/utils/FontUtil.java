package org.overengineer.inlineproblems.utils;

import com.intellij.ide.ui.AntialiasingType;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.util.ui.UIUtil;

import org.overengineer.inlineproblems.settings.SettingsState;

import java.awt.*;
import java.awt.font.FontRenderContext;

public class FontUtil {

    /**
     * The metrics the problem labels are measured with. Building them means constructing a font
     * with a fallback chain, which is far too expensive to repeat per problem - callers that draw
     * many labels at once should obtain them once and pass them around.
     */
    public static FontMetrics getLabelFontMetrics(Editor editor) {
        var editorContext = FontInfo.getFontRenderContext(editor.getComponent());
        var context = new FontRenderContext(
                editorContext.getTransform(),
                AntialiasingType.getKeyForCurrentScope(false),
                UISettings.getEditorFractionalMetricsHint()
        );

        return FontInfo.getFontMetrics(getActiveFont(editor), context);
    }
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
