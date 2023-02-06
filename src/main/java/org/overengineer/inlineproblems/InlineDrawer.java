package org.overengineer.inlineproblems;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import org.overengineer.inlineproblems.entities.DrawDetails;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.awt.*;
import java.util.Arrays;


public class InlineDrawer {

    public void drawProblemLabel(InlineProblem problem, DrawDetails drawDetails) {
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

    public void drawProblemLineHighlight(InlineProblem problem, DrawDetails drawDetails) {
        if (!drawDetails.isDrawHighlighter()) {
            return;
        }

        Editor editor = problem.getTextEditor().getEditor();

        TextAttributes textAttributes = new TextAttributes(
                editor.getColorsScheme().getDefaultForeground(),
                drawDetails.getHighlightColor(),
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
}
