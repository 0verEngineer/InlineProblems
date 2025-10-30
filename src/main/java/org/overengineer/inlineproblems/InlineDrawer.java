package org.overengineer.inlineproblems;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.util.TextRange;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.SeverityUtil;

import java.awt.Font;
import java.awt.Canvas;
import java.util.List;
import java.util.Arrays;


public class InlineDrawer {

    public void drawProblemLabel(InlineProblem problem) {
        var drawDetails = problem.getDrawDetails();
        if (!drawDetails.isDrawProblem()) {
            return;
        }

        SettingsState settings = SettingsState.getInstance();
        Editor editor = problem.getTextEditor().getEditor();
        var inlayModel = editor.getInlayModel();

        TextRange textRange = new TextRange(
                editor.getDocument().getLineStartOffset(problem.getLine()),
                editor.getDocument().getLineEndOffset(problem.getLine())
        );
        String lineText = editor.getDocument().getText(textRange);

        InlineProblemLabel inlineProblemLabel = new InlineProblemLabel(
                problem,
                drawDetails.getTextColor(),
                drawDetails.getBackgroundColor(),
                drawDetails.getTextColor().brighter(),
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
            inlineProblemLabel.setBlockElement(true);
            problem.setBlockElement(true);

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

    /** Draws the highlighter and the gutter icon for the currently shown problem in the line
     */
    public void drawLineHighlighterAndGutterIcon(List<InlineProblem> problemsInLine) {
        var problem = problemsInLine.get(0);
        var drawDetails = problem.getDrawDetails();

        /* If a lower severity has drawHighlighter and gutterIcon enabled and in the same line a problem with a higher
         * severity which has drawHighlighter and gutterIcon disabled is added, no highlighter and gutter icon is shown.
         */
        if (!drawDetails.isDrawHighlighter() && drawDetails.getIcon() == null) {
            return;
        }

        Editor editor = problem.getTextEditor().getEditor();
        Document document = editor.getDocument();

        if (document.getLineCount() <= problem.getLine()) {
            return;
        }

        TextAttributes textAttributes = new TextAttributes(
                editor.getColorsScheme().getDefaultForeground(),
                drawDetails.getHighlightColor(),
                null,
                null,
                Font.PLAIN
        );

        if (!drawDetails.isDrawHighlighter())
            textAttributes.setBackgroundColor(editor.getColorsScheme().getDefaultBackground());

        var highlighter = editor.getMarkupModel().addRangeHighlighter(
                document.getLineStartOffset(problem.getLine()),
                document.getLineEndOffset(problem.getLine()),
                problem.getSeverity(), // Use the severity as layer, hopefully it will not overdraw some important stuff
                textAttributes,
                HighlighterTargetArea.EXACT_RANGE
        );

        if (drawDetails.getIcon() != null) {
            removeGutterIconsForLine(editor, problem.getLine());
            highlighter.setGutterIconRenderer(new GutterRenderer(getGutterText(problemsInLine), drawDetails.getIcon()));
        }

        problem.setProblemLineHighlighterHashCode(highlighter.hashCode());
    }

    /**
     * @param problem the problem
     * @param problemsInLine the problems in the same line as problem, null if no gutter icons are enabled, keep in mind that
     *                       it still contains the problem itself
     */
    public void undrawErrorLineHighlight(InlineProblem problem, List<InlineProblem> problemsInLine) {
        MarkupModel markupModel = problem.getTextEditor().getEditor().getMarkupModel();

        Arrays.stream(markupModel.getAllHighlighters())
                .filter(h -> h.isValid() && h.hashCode() == problem.getProblemLineHighlighterHashCode())
                .forEach(markupModel::removeHighlighter);

        // Gutter icon re-adding
        if (problemsInLine != null && problemsInLine.size() > 1) {
            problemsInLine.remove(problem);
            drawLineHighlighterAndGutterIcon(problemsInLine);
        }
    }

    public void undrawInlineProblemLabel(InlineProblem problem) {
        Editor editor = problem.getTextEditor().getEditor();
        Document document = editor.getDocument();

        // Here is not checked if single or multi line, both are disposed because we do not have the info here
        // We search for all elements because they can move
        int documentLineStartOffset = document.getLineStartOffset(0);
        int endLine = document.getLineCount() - 1;
        if (endLine < 0) endLine = 0;
        int documentLineEndOffset = document.getLineEndOffset(endLine);

        if (problem.isBlockElement()) {
            editor.getInlayModel()
                    .getBlockElementsInRange(
                            documentLineStartOffset,
                            documentLineEndOffset
                    )
                    .stream()
                    .filter(e -> problem.getInlineProblemLabelHashCode() == e.getRenderer().hashCode())
                    .filter(e -> e.getRenderer() instanceof InlineProblemLabel)
                    .forEach(Disposable::dispose);
        }
        else {
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
    }

    private String getGutterText(List<InlineProblem> problemsInLine) {
        StringBuilder text = new StringBuilder();
        int previousSeverity = -1;
        String severityString;
        boolean sizeBiggerThanOne = problemsInLine.size() > 1;

        for (var p : problemsInLine) {
            if (sizeBiggerThanOne) {
                if (p.getSeverity() != previousSeverity) {
                    severityString = SeverityUtil.getSeverityAsString(p.getSeverity()) + "S: \n";
                    text.append(severityString);
                }
                text.append("- ");
            }

            text.append(p.getText());

            if (sizeBiggerThanOne)
                text.append("\n");

            previousSeverity = p.getSeverity();
        }

        return text.toString();
    }

    private void removeGutterIconsForLine(Editor editor, int line) {
        Document document = editor.getDocument();
        int lineStartOffset = document.getLineStartOffset(line);
        int lineEndOffset = document.getLineEndOffset(line);

        MarkupModel markupModel = editor.getMarkupModel();
        for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
            if (highlighter.getStartOffset() <= lineEndOffset && highlighter.getEndOffset() >= lineStartOffset) {
                GutterIconRenderer gutterIconRenderer = highlighter.getGutterIconRenderer();
                if (gutterIconRenderer instanceof GutterRenderer) {
                    highlighter.setGutterIconRenderer(null);
                }
            }
        }
    }
}
