package org.overengineer.inlineproblems.entities;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import lombok.Getter;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.FontUtil;

import java.awt.Font;
import java.awt.FontMetrics;


/**
 * Everything the drawing needs that depends on the editor but not on the single problem.
 * <p>
 * Obtaining these per problem is what dominated the drawing of a file with many problems: a
 * service lookup for the settings, the visible area, the editor font and its metrics, and above
 * all the label font, whose construction builds a fallback chain
 * ({@link FontUtil#getLabelFontMetrics}). Building the context once per editor and reusing it for
 * the whole drawing pass removes all of that from the per problem path.
 * <p>
 * The context is short lived on purpose - it is built for one drawing pass and thrown away, so it
 * cannot go stale when the color scheme, the settings or the editor size change.
 */
@Getter
public class EditorDrawContext {
    private final SettingsState settings;
    private final int editorWidth;
    private final Font editorFont;
    private final FontMetrics editorFontMetrics;
    private final FontMetrics labelFontMetrics;

    private EditorDrawContext(Editor editor) {
        settings = SettingsState.getInstance();
        editorWidth = editor.getScrollingModel().getVisibleArea().width;
        editorFont = editor.getColorsScheme().getFont(EditorFontType.PLAIN);

        /* The metrics are taken from the editor content component instead of a throwaway
         * java.awt.Canvas: instantiating a heavyweight AWT component per drawn problem showed up
         * as a hotspot while scrolling (GitHub issue #96). */
        editorFontMetrics = editor.getContentComponent().getFontMetrics(editorFont);
        labelFontMetrics = FontUtil.getLabelFontMetrics(editor);
    }

    public static EditorDrawContext forEditor(Editor editor) {
        return new EditorDrawContext(editor);
    }
}
