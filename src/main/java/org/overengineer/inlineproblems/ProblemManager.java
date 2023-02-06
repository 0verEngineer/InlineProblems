package org.overengineer.inlineproblems;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.overengineer.inlineproblems.entities.DrawDetails;
import org.overengineer.inlineproblems.entities.InlineProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ProblemManager implements Disposable {
    private final List<InlineProblem> activeProblems = new ArrayList<>();

    private final InlineDrawer inlineDrawer = new InlineDrawer();

    private final Logger logger = Logger.getInstance(ProblemManager.class);

    public void dispose() {
        reset();
    }

    public void removeProblem(InlineProblem problem) {
        InlineProblem problemToRemove = findActiveProblemByRangeHighlighterHashCode(problem.getRangeHighlighterHashCode());

        if (problemToRemove == null) {
            logger.warn("Removal of problem failed, not found by RangeHighlighterHashCode");
            resetForEditor(problem.getTextEditor().getEditor());
            return;
        }

        inlineDrawer.undrawErrorLineHighlight(problemToRemove);
        inlineDrawer.undrawInlineProblemLabel(problemToRemove);

        if (!activeProblems.remove(problemToRemove)) {
            logger.warn("Removal of problem failed, resetting");
            resetForEditor(problemToRemove.getTextEditor().getEditor());
            return;
        }
    }

    public void addProblem(InlineProblem problem) {
        DrawDetails drawDetails = new DrawDetails(problem, problem.getTextEditor().getEditor());

        inlineDrawer.drawProblemLabel(problem, drawDetails);
        inlineDrawer.drawProblemLineHighlight(problem, drawDetails);

        activeProblems.add(problem);
    }

    public void reset() {
        final List<InlineProblem> activeProblemSnapShot = List.copyOf(activeProblems);
        activeProblemSnapShot.forEach(this::removeProblem);
    }

    public void resetForEditor(Editor editor) {
        final List<InlineProblem> activeProblemsSnapShot = List.copyOf(activeProblems);

        activeProblemsSnapShot.stream()
                .filter(aP -> aP.getTextEditor().getEditor().equals(editor))
                .forEach(this::removeProblem);
    }

    public void updateFromNewActiveProblems(List<InlineProblem> problems) {
        final List<InlineProblem> activeProblemsSnapShot = List.copyOf(activeProblems);

        updateFromNewActiveProblems(problems, activeProblemsSnapShot);
    }

    public void updateFromNewActiveProblemsForProjectAndFile(List<InlineProblem> problems, Project project, String filePath) {
        final List<InlineProblem> activeProblemsSnapShot = activeProblems.stream()
                .filter(p -> p.getProject().equals(project) && p.getFile().equals(filePath))
                .collect(Collectors.toList());

        updateFromNewActiveProblems(problems, activeProblemsSnapShot);
    }

    private void updateFromNewActiveProblems(List<InlineProblem> problems, List<InlineProblem> activeProblemsSnapShot) {
        final List<InlineProblem> processedProblems = new ArrayList<>();

        activeProblemsSnapShot.stream()
                .filter(p -> !problems.contains(p))
                .forEach(p -> {processedProblems.add(p); removeProblem(p);});

        problems.stream()
                .filter(p -> !activeProblemsSnapShot.contains(p) && !processedProblems.contains(p))
                .forEach(this::addProblem);
    }

    private InlineProblem findActiveProblemByRangeHighlighterHashCode(int hashCode) {
        return activeProblems.stream()
                .filter(p -> p.getRangeHighlighterHashCode() == hashCode)
                .findFirst()
                .orElse(null);
    }
}
