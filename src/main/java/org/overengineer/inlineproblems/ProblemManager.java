package org.overengineer.inlineproblems;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.overengineer.inlineproblems.entities.DrawDetails;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.*;
import java.util.stream.Collectors;


public class ProblemManager implements Disposable {
    @Getter
    private final List<InlineProblem> activeProblems = new ArrayList<>();

    private final InlineDrawer inlineDrawer = new InlineDrawer();

    private final SettingsState settingsState = SettingsState.getInstance();

    private final Logger logger = Logger.getInstance(ProblemManager.class);

    public void dispose() {
        reset();
    }

    public void removeProblem(InlineProblem problem) {
        inlineDrawer.undrawErrorLineHighlight(problem);
        inlineDrawer.undrawInlineProblemLabel(problem);

        if (!Collections.synchronizedList(activeProblems).remove(problem)) {
            logger.warn("Removal of problem failed, resetting");
            resetForEditor(problem.getTextEditor().getEditor());
            return;
        }
    }

    /**
     * To add problems, if there are existing problems in the same line, they will be removed and re-added to ensure the
     * correct order (ordered by severity)
     * @param problem problem to add
     */
    public void addProblem(InlineProblem problem) {
        List<InlineProblem> problemsInLine = getProblemsInLineForProblem(problem);
        problemsInLine.add(problem);

        problemsInLine = problemsInLine.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getSeverity(), p1.getSeverity()))
                .collect(Collectors.toList());

        problemsInLine.forEach(p -> {
            if (p != problem)
                removeProblem(p);
        });

        /* This only works when using a method reference, if we move the code from the addProblemPrivate func into a lambda
        *  it does not work like expected, that is because there are differences in the evaluation and the way it is called */
        problemsInLine.forEach(this::addProblemPrivate);
    }

    private void addProblemPrivate(InlineProblem problem) {
        DrawDetails drawDetails = new DrawDetails(problem, problem.getTextEditor().getEditor());

        if (problem.getTextEditor().getEditor().getDocument().getLineCount() <= problem.getLine()) {
            logger.warn("Line count is less or equal than problem line, problem not added");
            return;
        }

        inlineDrawer.drawProblemLabel(problem, drawDetails);
        inlineDrawer.drawProblemLineHighlight(problem, drawDetails);

        Collections.synchronizedList(activeProblems).add(problem);
    }

    public boolean shouldProblemBeIgnored(int severity) {
        if (severity >= HighlightSeverity.ERROR.myVal) {
            return !settingsState.isHighlightErrors() && !settingsState.isShowErrors();
        }
        else if (severity >= HighlightSeverity.WARNING.myVal) {
            return !settingsState.isHighlightWarnings() && !settingsState.isShowWarnings();
        }
        else if (severity >= HighlightSeverity.WEAK_WARNING.myVal) {
            return !settingsState.isHighlightWeakWarnings() && !settingsState.isShowWeakWarnings();
        }
        else if (severity >= HighlightSeverity.INFORMATION.myVal) {
            return !settingsState.isHighlightInfos() && !settingsState.isShowInfos();
        }

        return true;
    }

    public void applyCustomSeverity(InlineProblem problem) {
        int severity = problem.getSeverity();

        for (int additionalSeverity : settingsState.getAdditionalErrorSeverities()) {
            if (additionalSeverity == severity) {
                problem.setSeverity(HighlightSeverity.ERROR.myVal);
                return;
            }
        }

        for (int additionalSeverity : settingsState.getAdditionalWarningSeverities()) {
            if (additionalSeverity == severity) {
                problem.setSeverity(HighlightSeverity.WARNING.myVal);
                return;
            }
        }

        for (int additionalSeverity : settingsState.getAdditionalWeakWarningSeverities()) {
            if (additionalSeverity == severity) {
                problem.setSeverity(HighlightSeverity.WEAK_WARNING.myVal);
                return;
            }
        }

        for (int additionalSeverity : settingsState.getAdditionalInfoSeverities()) {
            if (additionalSeverity == severity) {
                problem.setSeverity(HighlightSeverity.INFO.myVal);
                return;
            }
        }
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
        updateFromNewActiveProblems(problems, List.copyOf(activeProblems));
    }

    public void updateFromNewActiveProblemsForProjectAndFile(List<InlineProblem> problems, Project project, String filePath) {
        final List<InlineProblem> activeProblemsSnapShot = activeProblems.stream()
                .filter(p -> p.getProject().equals(project) && p.getFile().equals(filePath))
                .collect(Collectors.toList());

        updateFromNewActiveProblems(problems, activeProblemsSnapShot);
    }

    private List<InlineProblem> getProblemsInLineForProblem(InlineProblem problem) {
        return activeProblems.stream()
                .filter(p -> Objects.equals(p.getTextEditor(), problem.getTextEditor()) && p.getLine() == problem.getLine())
                .collect(Collectors.toList());
    }

    /**
     * Updates the active problems based on a list of new problems, problems can also be added and removed one by one,
     * like the MarkupModelProblemListener does, but if the feature "Show only highest severity per line" is enabled,
     * this function needs to be used.
     */
    private void updateFromNewActiveProblems(List<InlineProblem> newProblems, List<InlineProblem> activeProblemsSnapShot) {
        final List<Integer> processedProblemHashCodes = new ArrayList<>();
        List<InlineProblem> usedProblems;

        if (settingsState.isShowOnlyHighestSeverityPerLine()) {

            Map<String, InlineProblem> filteredMap = new HashMap<>();

            for (InlineProblem problem : newProblems) {
                String key = problem.getTextEditor().getFile().getPath() + problem.getLine();

                if (filteredMap.containsKey(key)) {
                    if (filteredMap.get(key).getSeverity() < problem.getSeverity()) {
                        filteredMap.replace(key, problem);
                    }
                }
                else {
                    filteredMap.put(key, problem);
                }
            }

            usedProblems = new ArrayList<>(filteredMap.values());
        }
        else {
            usedProblems = newProblems;
        }

        activeProblemsSnapShot.stream()
                .filter(p -> !usedProblems.contains(p))
                .forEach(p -> {
                    processedProblemHashCodes.add(p.hashCode());
                    removeProblem(p);
                });

        usedProblems.stream()
                .filter(p -> !activeProblemsSnapShot.contains(p) && !processedProblemHashCodes.contains(p.hashCode()))
                .forEach(this::addProblem);
    }
}
