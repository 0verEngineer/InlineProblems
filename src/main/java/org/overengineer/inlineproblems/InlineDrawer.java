package org.overengineer.inlineproblems;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.awt.*;
import java.util.Arrays;


public class InlineDrawer {

    public void drawProblemLabel(InlineProblem problem) {
        if (!shouldDrawProblemLabel(problem))
            return;

        Editor editor = problem.getTextEditor().getEditor();
        SettingsState settings = SettingsState.getInstance();
        var inlayModel = editor.getInlayModel();

        TextRange textRange = new TextRange(
                editor.getDocument().getLineStartOffset(problem.getLine()),
                editor.getDocument().getLineEndOffset(problem.getLine())
        );
        String lineText = editor.getDocument().getText(textRange);

        InlineProblemLabel inlineProblemLabel = new InlineProblemLabel(
                problem,
                getTextColor(problem, editor),
                getBackgroundColor(problem, editor),
                settings
        );

        // To get the potential width of the whole line with the problem text including existent inline elements
        int existingInlineElementsWidth = 0;
        var existingElements = editor.getInlayModel().getAfterLineEndElementsForLogicalLine(problem.getLine());
        for (final var existingElement : existingElements) {
            existingInlineElementsWidth += existingElement.getWidthInPixels();
        }

        int editorWidth = editor.getScrollingModel().getVisibleArea().width;

        Font editorFont = editor.getColorsScheme().getFont(EditorFontType.PLAIN);

        int problemWidth = inlineProblemLabel.calcWidthInPixels(editor) +
                new Canvas().getFontMetrics(editorFont).stringWidth(lineText) +
                existingInlineElementsWidth;

        // We add 50 as offset here because the calculation is somehow not exact
        if (problemWidth + 50 > editorWidth && !settings.isForceProblemsInSameLine())
        {
            inlineProblemLabel.setMultiLine(true);

            inlayModel.addBlockElement(
                    editor.getDocument().getLineStartOffset(problem.getLine()),
                    false,
                    true,
                    1,
                    inlineProblemLabel
            );
        }
        else {
            InlayProperties properties = new InlayProperties()
                    .relatesToPrecedingText(true)
                    .disableSoftWrapping(true)
                    .priority(1);

            inlayModel.addAfterLineEndElement(
                    problem.getActualEndOffset(),
                    properties,
                    inlineProblemLabel
            );
        }

        problem.setInlineProblemLabelHashCode(inlineProblemLabel.hashCode());
    }

    public void drawProblemLineHighlight(InlineProblem problem) {
        if (!shouldDrawProblemHighlighter(problem))
            return;

        Editor editor = problem.getTextEditor().getEditor();
        TextAttributes textAttributes = new TextAttributes(
                editor.getColorsScheme().getDefaultForeground(),
                getHighlightColor(problem, editor),
                null,
                null,
                Font.PLAIN
        );

        Document document = editor.getDocument();

        problem.setProblemLineHighlighterHashCode(editor.getMarkupModel().addRangeHighlighter(
                document.getLineStartOffset(problem.getLine()),
                document.getLineEndOffset(problem.getLine()),
                problem.getSeverity(), // Use the severity as layer, hopefully it will not overdraw some important stuff
                textAttributes,
                HighlighterTargetArea.EXACT_RANGE
        ).hashCode());
    }

    public void undrawErrorLineHighlight(InlineProblem problem) {
        MarkupModel markupModel = problem.getTextEditor().getEditor().getMarkupModel();

        Arrays.stream(markupModel.getAllHighlighters())
                .filter(h -> h.isValid() && h.hashCode() == problem.getProblemLineHighlighterHashCode())
                .forEach(markupModel::removeHighlighter);
    }

    public void undrawInlineProblemLabel(InlineProblem problem) {
        Editor editor = problem.getTextEditor().getEditor();
        Document document = editor.getDocument();

        // Here is not checked if single or multi line, both are disposed because we do not have the info here
        // We search for all elements because they can move
        int documentLineStartOffset = document.getLineStartOffset(0);
        int documentLineEndOffset = document.getLineEndOffset(document.getLineCount() - 1);
        editor.getInlayModel()
                .getBlockElementsInRange(
                        documentLineStartOffset,
                        documentLineEndOffset
                )
                .stream()
                .filter(e -> problem.getInlineProblemLabelHashCode() == e.getRenderer().hashCode())
                .filter(e -> e.getRenderer() instanceof InlineProblemLabel)
                .forEach(Disposable::dispose);

        editor.getInlayModel()
                .getAfterLineEndElementsInRange(
                        documentLineStartOffset,
                        documentLineEndOffset
                )
                .stream()
                .filter(e -> problem.getInlineProblemLabelHashCode() == e.getRenderer().hashCode())
                .filter(e -> e.getRenderer() instanceof InlineProblemLabel)
                .forEach(Disposable::dispose);
    }

    private boolean shouldDrawProblemLabel(InlineProblem problem) {
        SettingsState settings = SettingsState.getInstance();
        int severity = problem.getSeverity();

        if (severity >= HighlightSeverity.ERROR.myVal &&
                settings.isShowErrors())
            return true;
        else if (severity >= HighlightSeverity.WARNING.myVal &&
                severity < HighlightSeverity.ERROR.myVal && settings.isShowWarnings())
            return true;
        else if (severity >= HighlightSeverity.WEAK_WARNING.myVal &&
                severity < HighlightSeverity.WARNING.myVal && settings.isShowWeakWarnings())
            return true;
        else if (severity >= HighlightSeverity.INFORMATION.myVal &&
                severity < HighlightSeverity.WEAK_WARNING.myVal && settings.isShowInfos())
            return true;

        return false;
    }

    private boolean shouldDrawProblemHighlighter(InlineProblem problem) {
        SettingsState settings = SettingsState.getInstance();
        int severity = problem.getSeverity();

        if (severity >= HighlightSeverity.ERROR.myVal &&
                settings.isHighlightErrors())
            return true;
        else if (severity >= HighlightSeverity.WARNING.myVal &&
                severity < HighlightSeverity.ERROR.myVal && settings.isHighlightWarnings())
            return true;
        else if (severity >= HighlightSeverity.WEAK_WARNING.myVal &&
                severity < HighlightSeverity.WARNING.myVal && settings.isHighlightWeakWarnings())
            return true;
        else if (severity >= HighlightSeverity.INFORMATION.myVal &&
                severity < HighlightSeverity.WEAK_WARNING.myVal && settings.isHighlightInfos())
            return true;

        return false;
    }

    private Color getTextColor(InlineProblem problem, Editor editor) {
        SettingsState settings = SettingsState.getInstance();
        Color color = editor.getColorsScheme().getDefaultForeground();

        if (problem.getSeverity() >= HighlightSeverity.ERROR.myVal)
            color = settings.getErrorTextColor();
        else if (problem.getSeverity() >= HighlightSeverity.WARNING.myVal)
            color = settings.getWarningTextColor();
        else if (problem.getSeverity() >= HighlightSeverity.WEAK_WARNING.myVal)
            color = settings.getWeakWarningTextColor();
        else if (problem.getSeverity() >= HighlightSeverity.INFORMATION.myVal)
            color = settings.getInfoTextColor();

        return color;
    }

    private Color getBackgroundColor(InlineProblem problem, Editor editor) {
        SettingsState settings = SettingsState.getInstance();
        Color backgroundColor = editor.getColorsScheme().getDefaultBackground();

        if (problem.getSeverity() >= HighlightSeverity.ERROR.myVal)
            backgroundColor = settings.getErrorBackgroundColor();
        else if (problem.getSeverity() >= HighlightSeverity.WARNING.myVal)
            backgroundColor = settings.getWarningBackgroundColor();
        else if (problem.getSeverity() >= HighlightSeverity.WEAK_WARNING.myVal)
            backgroundColor = settings.getWeakWarningBackgroundColor();
        else if (problem.getSeverity() >= HighlightSeverity.INFORMATION.myVal)
            backgroundColor = settings.getInfoBackgroundColor();

        return backgroundColor;
    }

    private Color getHighlightColor(InlineProblem problem, Editor editor) {
        SettingsState settings = SettingsState.getInstance();
        Color color = editor.getColorsScheme().getDefaultBackground();

        if (shouldDrawProblemHighlighter(problem)) {
            if (problem.getSeverity() >= HighlightSeverity.ERROR.myVal)
                color = settings.getErrorHighlightColor();
            else if (problem.getSeverity() >= HighlightSeverity.WARNING.myVal)
                color = settings.getWarningHighlightColor();
            else if (problem.getSeverity() >= HighlightSeverity.WEAK_WARNING.myVal)
                color = settings.getWeakWarningHighlightColor();
            else if (problem.getSeverity() >= HighlightSeverity.INFORMATION.myVal)
                color = settings.getInfoHighlightColor();
        }

        return color;
    }
}
