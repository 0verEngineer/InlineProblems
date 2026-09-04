package org.overengineer.inlineproblems;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;
import org.overengineer.inlineproblems.utils.SeverityUtil;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.List;


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

        /* The font metrics are taken from the editor content component instead of a throwaway
         * java.awt.Canvas: instantiating a heavyweight AWT component for every drawn problem
         * showed up as a hotspot while scrolling (GitHub issue #96). */
        FontMetrics editorFontMetrics = editor.getContentComponent().getFontMetrics(editorFont);

        int problemWidth = inlineProblemLabel.calcWidthInPixels(editor) +
                editorFontMetrics.stringWidth(lineText) +
                existingInlineElementsWidth;

        /* The offset is added because the width calculation is not exact. It is configurable
         * because the deviation depends on the font and the editor. */
        if (problemWidth + settings.getProblemLineLengthOffsetPixels() > editorWidth && !settings.isForceProblemsInSameLine())
        {
            inlineProblemLabel.setBlockElement(true);
            problem.setBlockElement(true);

            problem.setInlay(inlayModel.addBlockElement(
                    editor.getDocument().getLineStartOffset(problem.getLine()),
                    false,
                    true,
                    1,
                    inlineProblemLabel
            ));
        }
        else {
            InlayProperties properties = new InlayProperties()
                    .relatesToPrecedingText(true)
                    .disableSoftWrapping(true)
                    .priority(1);

            problem.setInlay(inlayModel.addAfterLineEndElement(
                    problem.getActualEndOffset(),
                    properties,
                    inlineProblemLabel
            ));
        }
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

        problem.setLineHighlighter(highlighter);
    }

    /**
     * @param problem the problem
     * @param problemsInLine the problems in the same line as problem, null if no gutter icons are enabled, keep in mind that
     *                       it still contains the problem itself
     */
    public void undrawErrorLineHighlight(InlineProblem problem, List<InlineProblem> problemsInLine) {
        RangeHighlighter lineHighlighter = problem.getLineHighlighter();

        if (lineHighlighter != null) {
            problem.setLineHighlighter(null);

            if (lineHighlighter.isValid()) {
                problem.getTextEditor().getEditor().getMarkupModel().removeHighlighter(lineHighlighter);
            }
        }

        // Gutter icon re-adding
        if (problemsInLine != null && problemsInLine.size() > 1) {
            problemsInLine.remove(problem);
            drawLineHighlighterAndGutterIcon(problemsInLine);
        }
    }

    public void undrawInlineProblemLabel(InlineProblem problem) {
        Inlay<?> inlay = problem.getInlay();

        if (inlay == null) {
            return;
        }

        problem.setInlay(null);

        /* The inlay moves with the document, so the reference stays correct. Searching all
         * elements of the document for a matching renderer hash code, as it was done before,
         * is both slow (GitHub issue #96) and ambiguous on a hash collision, which could leave
         * the label behind or dispose a foreign one (GitHub issues #38, #44). */
        if (inlay.isValid()) {
            Disposer.dispose(inlay);
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

        /* Only the highlighters overlapping the line are relevant. Materializing every
         * highlighter of the editor was one of the hotspots in GitHub issue #96. */
        if (markupModel instanceof MarkupModelEx markupModelEx) {
            markupModelEx.processRangeHighlightersOverlappingWith(
                    lineStartOffset,
                    lineEndOffset,
                    highlighter -> {
                        removeOwnGutterIcon(highlighter);
                        return true;
                    }
            );

            return;
        }

        for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
            if (highlighter.getStartOffset() <= lineEndOffset && highlighter.getEndOffset() >= lineStartOffset) {
                removeOwnGutterIcon(highlighter);
            }
        }
    }

    private void removeOwnGutterIcon(RangeHighlighter highlighter) {
        if (highlighter.getGutterIconRenderer() instanceof GutterRenderer) {
            highlighter.setGutterIconRenderer(null);
        }
    }
}
