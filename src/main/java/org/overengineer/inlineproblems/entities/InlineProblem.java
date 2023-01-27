package org.overengineer.inlineproblems.entities;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode(exclude = {"problemLineHighlighterHashCode", "inlineProblemLabelHashCode", "problemInLineCount"})
public class InlineProblem {

    private final int line;
    private final int severity;

    // If two problems with the same text occur in the same line only one will be shown
    private final String text;
    private final String file;
    private final Editor editor;
    private final Project project;

    // Set after drawing the line highlight, used to remove it again
    private int problemLineHighlighterHashCode;

    // Set after drawing the inlay, used to remove the inlay again
    private int inlineProblemLabelHashCode = 0;

    // Increased if the same problem (same text) would be on the same line
    private int problemInLineCount = 0;

    public InlineProblem(
            int line,
            int severity,
            String text,
            Editor editor,
            String filePath,
            Project project
    ) {
        this.line = line;
        this.text = text;
        this.severity = severity;
        this.editor = editor;
        this.file = filePath;
        this.project = project;
    }
}
