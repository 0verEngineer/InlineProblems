package org.overengineer.inlineproblems.entities;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;


@Getter
@Setter
public class InlineProblem {
    private final int line;
    private final int severity;
    private final String text;
    private final String file;
    private final Editor editor;
    private final Project project;
    @Nullable
    private RangeHighlighter problemLineHighlighter;
    private int inlineProblemLabelHashCode = 0;

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

    public boolean equals(Object other) {
        if (!(other instanceof InlineProblem))
                return false;

        InlineProblem otherProblem = (InlineProblem) other;

        // The InlineProblemLabel is not used here because we do not have it if new InlineProblems come from the
        // HighlightProblemsListener
        return otherProblem.getLine() == line &&
                otherProblem.getSeverity() == severity &&
                otherProblem.getFile().equals(file) &&
                otherProblem.getProject().equals(project) &&
                otherProblem.getText().equals(text);
    }
}
