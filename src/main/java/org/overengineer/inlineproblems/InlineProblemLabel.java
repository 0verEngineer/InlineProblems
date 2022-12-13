package org.overengineer.inlineproblems;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.components.JBLabel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;


@Getter
public class InlineProblemLabel implements EditorCustomElementRenderer {

    private final JBLabel label;
    private final Color textColor;
    private final Color backgroundColor;
    private final boolean isDrawBox;
    private final boolean isRoundedCorners;


    @Setter
    private boolean isMultiLine;
    private final FontMetrics fontMetrics;

    public InlineProblemLabel(
            final String problemMessage,
            final Color textColor,
            final Color backgroundColor,
            final boolean isMultiLine,
            final boolean isDrawBox,
            final boolean isRoundedCorners,
            final Editor editor
    ) {
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.isMultiLine = isMultiLine;
        this.isDrawBox = isDrawBox;
        this.isRoundedCorners = isRoundedCorners;

        this.label = new JBLabel(problemMessage);

        fontMetrics = new Canvas().getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        return calcWidthInPixels();
    }

    public int calcWidthInPixels() {
        return fontMetrics.stringWidth(label.getText()) + 2;
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return calcHeightInPixels();
    }

    public int calcHeightInPixels() {
        // Not guaranteed to be correct, because we are not able to draw the label in the paint method, we use
        // drawString so the height does not matter to much
        return label.getPreferredSize().height;
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics graphics, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        EditorColorsScheme colorsScheme = editor.getColorsScheme();

        graphics.setFont(colorsScheme.getFont(EditorFontType.PLAIN));

        if (isDrawBox) {
            if (isRoundedCorners) {
                graphics.setColor(backgroundColor);

                // The box is somehow a bit off, adjust it here, the text is also moved by 2 pixels
                graphics.drawRoundRect(
                        targetRegion.x + 2,
                        targetRegion.y + 1, // Not centered without the + 1
                        calcWidthInPixels(),
                        calcHeightInPixels(),
                        5,
                        5
                );
            }
            else {
                graphics.setColor(backgroundColor);

                graphics.drawRect(
                        targetRegion.x + 2,
                        targetRegion.y + 1,
                        calcWidthInPixels(),
                        calcHeightInPixels()
                );
            }
        }

        graphics.setColor(textColor);
        graphics.drawString(label.getText(), targetRegion.x + 3, targetRegion.y - 1 + editor.getAscent());
    }

    @Override
    public @Nullable GutterIconRenderer calcGutterIconRenderer(@NotNull Inlay inlay) {
        return EditorCustomElementRenderer.super.calcGutterIconRenderer(inlay);
    }
}
