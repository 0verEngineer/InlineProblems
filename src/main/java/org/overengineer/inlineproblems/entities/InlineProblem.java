package org.overengineer.inlineproblems.entities;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.overengineer.inlineproblems.definitions.RegexPattern;
import org.overengineer.inlineproblems.settings.SettingsState;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InlineProblem {

    /* The identity of a problem is deliberately the editor, the file, the line, the severity and
     * the text - and explicitly not the offsets or the RangeHighlighter instance. A daemon run
     * recreates the highlighters and shifts the offsets of everything behind an edit, so including
     * those made every problem of the file look new on every keystroke: all inlays were disposed
     * and recreated, and every single one of those triggers an editor size revalidation. */

    @EqualsAndHashCode.Include
    private final int line;

    @Setter
    @EqualsAndHashCode.Include
    private int severity;

    // If two problems with the same text occur in the same line only one will be shown
    @EqualsAndHashCode.Include
    private final String text;

    @EqualsAndHashCode.Include
    private final String file;

    @EqualsAndHashCode.Include
    private final TextEditor textEditor;

    private final Project project;

    private DrawDetails drawDetails;

    private int actualEndOffset;
    private int actualStartffset;

    private boolean isBlockElement = false;

    // Set after drawing the line highlight, used to remove it again
    private RangeHighlighter lineHighlighter;

    // Set after drawing the inlay, used to remove the inlay again
    private Inlay<?> inlay;

    // The highlighter the problem was created from
    private RangeHighlighter rangeHighlighter;


    public InlineProblem(
            int line,
            String filePath,
            HighlightInfo highlightInfo,
            TextEditor textEditor,
            RangeHighlighter rangeHighlighter,
            SettingsState settingsState
    ) {
        String usedText = highlightInfo.getDescription();
        if (usedText == null)
            usedText = "";
        else
            usedText = getTextWithHtmlStrippingAndXmlUnescaping(usedText.stripLeading(), settingsState);

        this.line = line;
        this.text = usedText;
        this.severity = highlightInfo.getSeverity().myVal;
        this.textEditor = textEditor;
        this.file = filePath;
        this.project = textEditor.getEditor().getProject();
        this.rangeHighlighter = rangeHighlighter;
        this.actualStartffset = highlightInfo.getStartOffset();

        if (highlightInfo.getActualEndOffset() == 0)
            this.actualEndOffset = highlightInfo.getActualEndOffset();
        else
            this.actualEndOffset = highlightInfo.getActualEndOffset() -1;
    }

    /**
     * Takes over the volatile position data of an equal, freshly scanned problem. The drawn
     * elements are anchored to the document and move with it, so they stay untouched; only the
     * offsets, which are used as the click target, have to follow.
     */
    public void refreshPositionFrom(InlineProblem newProblem) {
        this.actualStartffset = newProblem.actualStartffset;
        this.actualEndOffset = newProblem.actualEndOffset;
        this.rangeHighlighter = newProblem.rangeHighlighter;
    }

    private String getTextWithHtmlStrippingAndXmlUnescaping(String text, SettingsState settingsState) {
        if (
                settingsState.isEnableHtmlStripping() &&
                text.contains("<") &&
                RegexPattern.HTML_TAG_PATTERN.matcher(text).find()
        ) {
            text = StringUtil.stripHtml(text, " ");
        }

        if (
                settingsState.isEnableXmlUnescaping() &&
                text.contains("&")
        ) {
            text = StringUtil.unescapeXmlEntities(text);
        }

        return text;
    }
}
