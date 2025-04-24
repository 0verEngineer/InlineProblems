package org.overengineer.inlineproblems;

import com.intellij.codeInsight.hints.presentation.InputHandler;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.ide.ui.AntialiasingType;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.paint.EffectPainter;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingx.action.ActionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.FontUtil;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;


@Getter
public class InlineProblemLabel implements EditorCustomElementRenderer, InputHandler {
    private final String text;
    private final Color textColor;
    private final Color backgroundColor;
    private final Color hoverColor;
    private final boolean isDrawBox;
    private final boolean isRoundedCorners;
    private final boolean isFillBackground;
    private boolean hovered;
    private final boolean hoveredEnabled;
    private Inlay<?> inlay;
    private final int actualStartOffset;

    @Setter
    private boolean isBlockElement;

    private final int inlayFontSizeDelta;
    private final boolean isUseEditorFont;

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
            final Color hoverColor,
            final SettingsState settings
    ) {
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.hoverColor = hoverColor;
        this.isDrawBox = settings.isDrawBoxesAroundErrorLabels();
        this.isRoundedCorners = settings.isRoundedCornerBoxes();
        this.text = problem.getText();
        this.isBlockElement = false;
        this.isFillBackground = settings.isFillProblemLabels();

        this.isUseEditorFont = settings.isUseEditorFont();
        this.inlayFontSizeDelta = settings.getInlayFontSizeDelta();
        this.hovered = false;
        this.actualStartOffset = problem.getActualStartffset();
        hoveredEnabled = settings.isHovering();
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
    public void paint(@NotNull Inlay inlay, @NotNull Graphics graphics, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        this.inlay = inlay;
        // These offsets are applied here and not in the calc functions itself because we use it to shrink the drawn stuff a little bit
        int width = calcWidthInPixels(inlay) + DRAW_BOX_WIDTH_OFFSET;
        int height = calcHeightInPixels(inlay) + DRAW_BOX_HEIGHT_OFFSET;

        int targetRegionY = targetRegion.y + DRAW_BOX_PLACEMENT_OFFSET_Y;

        int editorFontSize = editor.getColorsScheme().getEditorFontSize();

        // Apply delta on the boxes
        if (inlayFontSizeDelta != 0 && editorFontSize > inlayFontSizeDelta) {
            height -= inlayFontSizeDelta;
            targetRegionY += (int) (inlayFontSizeDelta / 1.5);
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
            } else {
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

        graphics.setColor(hovered ? hoverColor : textColor);

        graphics.setFont(FontUtil.getActiveFont(editor));

        graphics.drawString(
                text,
                targetRegion.x + DRAW_STRING_LINE_PLACEMENT_OFFSET_X,
                targetRegion.y + DRAW_STRING_LINE_PLACEMENT_OFFSET_Y + editor.getAscent()
        );
        if (hovered)
            EffectPainter.LINE_UNDERSCORE.paint(
                    (Graphics2D) graphics,
                    targetRegion.x - DRAW_BOX_WIDTH_OFFSET,
                    targetRegion.y + editor.getAscent() ,
                    width + (DRAW_BOX_WIDTH_OFFSET * 2),
                    editor.getAscent(),
                    FontUtil.getActiveFont(editor));
    }

    private void setHovered(boolean hovered) {
        if (!this.hoveredEnabled || this.hovered == hovered) {
            return;
        }
        this.hovered = hovered;
        if (inlay != null)
            inlay.repaint();
    }

    @Override
    public @Nullable GutterIconRenderer calcGutterIconRenderer(@NotNull Inlay inlay) {
        return EditorCustomElementRenderer.super.calcGutterIconRenderer(inlay);
    }

    @Override
    public void mouseClicked(@NotNull MouseEvent mouseEvent, @NotNull Point point) {
        if (!hoveredEnabled) {
            return;
        }
        if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
            mouseEvent.consume();

            var editor = inlay.getEditor();
            editor.getCaretModel().moveToOffset(actualStartOffset);
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

            var action = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
            if (action == null) {
                Project project = editor.getProject();
                if (project == null) return;
                PsiFile psiFileInEditor = PsiUtilBase.getPsiFileInEditor(editor, project);
                if (psiFileInEditor == null) return;
                new ShowIntentionActionsHandler().invoke(project, editor, psiFileInEditor, false);
            } else {
                ActionUtil.invokeAction((AnAction) action, editor.getComponent(), "EditorInlay", null, null);

            }
        }
    }

    @Override
    public void mouseMoved(@NotNull MouseEvent mouseEvent, @NotNull Point point) {
        setHovered(true);
    }

    @Override
    public void mouseExited() {
        setHovered(false);
    }

}
