package org.overengineer.inlineproblems;

import com.intellij.ide.ui.AntialiasingType;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.FontUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;


@Getter
public class InlineProblemLabel implements EditorCustomElementRenderer {

    private final String text;
    private final Color textColor;
    private final Color backgroundColor;
    private final boolean isDrawBox;
    private final boolean isRoundedCorners;
    private final boolean isFillBackground;

    @Setter
    private boolean isBlockElement;

    private int inlayFontSizeDelta;
    private boolean isUseEditorFont = false;

    private static final int WIDTH_OFFSET = 7;
    private static final int DRAW_BOX_HEIGHT_OFFSET = -2; // Makes the box lines visible even if line below / above is highlighted
    private static final int DRAW_BOX_WIDTH_OFFSET = -2; // To have space between 2 boxes
    private static final int DRAW_BOX_PLACEMENT_OFFSET_Y = 1;
    private static final int DRAW_STRING_LINE_PLACEMENT_OFFSET_Y = -1;
    private static final int DRAW_STRING_LINE_PLACEMENT_OFFSET_X = 3;

    public InlineProblemLabel(
            final InlineProblem problem,
            final Color textColor,
            final Color backgroundColor,
            final SettingsState settings
    ) {
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.isDrawBox = settings.isDrawBoxesAroundErrorLabels();
        this.isRoundedCorners = settings.isRoundedCornerBoxes();
        this.text = problem.getText();
        this.isBlockElement = false;
        this.isFillBackground = settings.isFillProblemLabels();

        this.isUseEditorFont = settings.isUseEditorFont();
        this.inlayFontSizeDelta = settings.getInlayFontSizeDelta();
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        return calcWidthInPixels(inlay.getEditor());
    }

    public int calcWidthInPixels(@NotNull Editor editor) {
        var editorContext = FontInfo.getFontRenderContext(editor.getComponent());
        var context = new FontRenderContext(editorContext.getTransform(),
                AntialiasingType.getKeyForCurrentScope(false),
                UISettings.getEditorFractionalMetricsHint());

        var fontMetrics = FontInfo.getFontMetrics(FontUtil.getActiveFont(editor), context);

        return fontMetrics.stringWidth(text) + WIDTH_OFFSET;
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return inlay.getEditor().getLineHeight();
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics graphics, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();

        // These offsets are applied here and not in the calc functions itself because we use it to shrink the drawn stuff a little bit
        int width = calcWidthInPixels(inlay) + DRAW_BOX_WIDTH_OFFSET;
        int height = calcHeightInPixels(inlay) + DRAW_BOX_HEIGHT_OFFSET;

        int targetRegionY = targetRegion.y + DRAW_BOX_PLACEMENT_OFFSET_Y;

        int editorFontSize = editor.getColorsScheme().getEditorFontSize();

        // Apply delta on the boxes
        if (inlayFontSizeDelta != 0 && editorFontSize > inlayFontSizeDelta) {
            height = height - inlayFontSizeDelta;
            targetRegionY += (int)(inlayFontSizeDelta / 1.5);
        }

        if (isDrawBox) {
            graphics.setColor(backgroundColor);

            if (isRoundedCorners) {
                graphics.drawRoundRect(
                        targetRegion.x,
                        targetRegionY,
                        width,
                        height,
                        5,
                        5
                );

                if (isFillBackground) {
                    graphics.fillRoundRect(
                            targetRegion.x,
                            targetRegionY,
                            width,
                            height,
                            5,
                            5
                    );
                }
            }
            else {
            graphics.drawRect(
                    targetRegion.x,
                    targetRegionY,
                    width,
                    height
            );

                if (isFillBackground) {
                    graphics.fillRect(
                            targetRegion.x,
                            targetRegionY,
                            width,
                            height
                    );
                }
            }
        }

        graphics.setColor(textColor);

        graphics.setFont(FontUtil.getActiveFont(editor));

        graphics.drawString(
                text,
                targetRegion.x + DRAW_STRING_LINE_PLACEMENT_OFFSET_X,
                targetRegion.y + DRAW_STRING_LINE_PLACEMENT_OFFSET_Y + editor.getAscent()
        );
    }

    @Override
    public @Nullable GutterIconRenderer calcGutterIconRenderer(@NotNull Inlay inlay) {
        return EditorCustomElementRenderer.super.calcGutterIconRenderer(inlay);
    }
}
