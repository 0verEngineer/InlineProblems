package org.overengineer.inlineproblems;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.overengineer.inlineproblems.entities.DrawDetails;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.*;
import java.util.stream.Collectors;


public class ProblemManager implements Disposable {
    private final List<InlineProblem> activeProblems = new ArrayList<>();

    private final Map<Integer, List<InlineProblem>> hiddenProblemsCache = new HashMap<>();

    private final InlineDrawer inlineDrawer = new InlineDrawer();

    private final SettingsState settingsState = SettingsState.getInstance();

    private final Logger logger = Logger.getInstance(ProblemManager.class);

    public void dispose() {
        reset();
    }

    public void removeProblem(InlineProblem problem) {

        if (settingsState.isShowOnlyHighestSeverityPerLine()) {
            removeProblemWithHiddenProblemsCacheActive(problem, true);
        }
        else {
            removeProblemPrivate(problem);
        }
    }

    private void removeProblemPrivate(InlineProblem problem) {
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
        if (settingsState.isShowOnlyHighestSeverityPerLine()) {
            addProblemShowOnlyHighestSeverity(problem);
        }
        else {
            addProblemSorted(problem);
        }
    }

    private void addProblemPrivate(InlineProblem problem) {
        DrawDetails drawDetails = new DrawDetails(problem, problem.getTextEditor().getEditor());

        inlineDrawer.drawProblemLabel(problem, drawDetails);
        inlineDrawer.drawProblemLineHighlight(problem, drawDetails);

        activeProblems.add(problem);
    }

    /**
     * Used to remove problems when the hiddenProblemsCache is active, i.e. from the MarkupModelListener when the user
     * has the setting "Show only highest severity problem per line" enabled
     */
    private void removeProblemWithHiddenProblemsCacheActive(InlineProblem problem, boolean addCachedProblemAfterRemoval) {
        List<InlineProblem> activeProblems = getProblemsInLine(problem.getLine());
        if (activeProblems.size() > 1) {
            logger.warn("Multiple activeProblems found in ProblemManager.removeProblemFromMarkupModelListener");
        }
        else if (activeProblems.size() == 0) {
            logger.warn("No activeProblems found in ProblemManager.removeProblemFromMarkupModelListener");
        }
        else {
            // The key of the map is the hashCode of the problem with the highest severity
            var activeProblem = activeProblems.get(0);

            if (hiddenProblemsCache.containsKey(activeProblem.hashCode())) {
                InlineProblem problemToRemoveFromHiddenCache = findProblemByRangeHighlighterHashCodeFromList(
                        problem.getRangeHighlighterHashCode(),
                        hiddenProblemsCache.get(activeProblem.hashCode())
                );

                if (problemToRemoveFromHiddenCache != null) {
                    logger.info("Problem removal not needed, problem is cached, removing from cache instead " + problem.getText());
                    hiddenProblemsCache.get(activeProblem.hashCode()).remove(problemToRemoveFromHiddenCache);
                    return;
                }
            }

            removeProblemPrivate(problem);

            if (addCachedProblemAfterRemoval) {
                var notShownProblems = hiddenProblemsCache.get(activeProblem.hashCode());

                // Adds the first of the cached hidden problems
                if (notShownProblems != null && notShownProblems.size() > 0) {

                    logger.info("Re-adding problem: " + notShownProblems.get(0).getText());
                    InlineProblem problemToShow = notShownProblems.get(0);
                    addProblemPrivate(problemToShow);

                    // Update key (hashcode) of cached problems map
                    hiddenProblemsCache.remove(activeProblem.hashCode());
                    hiddenProblemsCache.put(problemToShow.hashCode(), notShownProblems);
                }
            }
        }
    }

    /**
     * To add problems, if there are existing problems in the same line, they will be removed and re-added to ensure the
     * correct order (ordered by severity)
     * @param problem problem to add
     */
    public void addProblemSorted(InlineProblem problem) {
        List<InlineProblem> problemsInLine = getProblemsInLine(problem.getLine());
        problemsInLine.add(problem);

        problemsInLine = problemsInLine.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getSeverity(), p1.getSeverity()))
                .collect(Collectors.toList());

        problemsInLine.forEach(p -> {
            if (p != problem)
                removeProblemPrivate(p);
        });

        problemsInLine.forEach(this::addProblemPrivate);
    }

    public void addProblemShowOnlyHighestSeverity(InlineProblem problem) {
        List<InlineProblem> problemsInLine = getProblemsInLine(problem.getLine());

        if (!problemsInLine.isEmpty()) {
            InlineProblem highestSeverityProblem = problemsInLine.stream()
                    .max(Comparator.comparingInt(InlineProblem::getSeverity))
                    .get();

            if (highestSeverityProblem.getSeverity() > problem.getSeverity()) {
                // Here no new key (hashcode) is needed because the active problem hasn't changed, so we pass in the same hashcode
                addProblemToHiddenProblemsCache(problem, highestSeverityProblem.hashCode());

                return;
            }
            else {
                removeProblemPrivate(highestSeverityProblem);
                addProblemToHiddenProblemsCache(highestSeverityProblem, problem.hashCode());
            }
        }

        addProblemPrivate(problem);
    }

    /**
     * Adds a problem into the not hidden problems cache, also changes the key of the cache map so that the currently
     * active window's hashcode is always the key
     */
    private void addProblemToHiddenProblemsCache(InlineProblem problem, int hashCode) {
        if (hiddenProblemsCache.containsKey(problem.hashCode())) {
            List<InlineProblem> problems = hiddenProblemsCache.get(problem.hashCode());
            problems.add(problem);

            problems = problems.stream()
                    .sorted((p1, p2) -> Integer.compare(p2.getSeverity(), p1.getSeverity()))
                    .collect(Collectors.toList());

            hiddenProblemsCache.remove(problem.hashCode());
            hiddenProblemsCache.put(hashCode, problems);
        }
        else {
            hiddenProblemsCache.put(hashCode, new ArrayList<>(List.of(problem)));
        }
    }

    public void reset() {
        final List<InlineProblem> activeProblemSnapShot = List.copyOf(activeProblems);
        hiddenProblemsCache.clear();
        activeProblemSnapShot.forEach(this::removeProblemPrivate);
    }

    public void resetForEditor(Editor editor) {
        final List<InlineProblem> activeProblemsSnapShot = List.copyOf(activeProblems);

        activeProblemsSnapShot.stream()
                .filter(aP -> aP.getTextEditor().getEditor().equals(editor))
                .forEach(p -> {
                    removeProblemPrivate(p);
                    hiddenProblemsCache.remove(p.hashCode());
                });
    }

    private List<InlineProblem> getActiveProblemsWithHiddenProblemsCache() {
        List<InlineProblem> activeProblems = new ArrayList<>(this.activeProblems);

        hiddenProblemsCache.forEach((k, v) -> activeProblems.addAll(v));

        return activeProblems;
    }

    public void updateFromNewActiveProblems(List<InlineProblem> problems) {
        final List<InlineProblem> activeProblemsSnapShot = getActiveProblemsWithHiddenProblemsCache();

        updateFromNewActiveProblems(problems, activeProblemsSnapShot);
    }

    public void updateFromNewActiveProblemsForProjectAndFile(List<InlineProblem> problems, Project project, String filePath) {
        final List<InlineProblem> activeProblemsSnapShot = getActiveProblemsWithHiddenProblemsCache().stream()
                .filter(p -> p.getProject().equals(project) && p.getFile().equals(filePath))
                .collect(Collectors.toList());

        updateFromNewActiveProblems(problems, activeProblemsSnapShot);
    }

    public List<InlineProblem> getProblemsInLine(int line) {
        return activeProblems.stream()
                .filter(p -> p.getLine() == line)
                .collect(Collectors.toList());
    }

    private void updateFromNewActiveProblems(List<InlineProblem> newProblems, List<InlineProblem> activeProblemsSnapShot) {
        final List<InlineProblem> processedProblems = new ArrayList<>();

        activeProblemsSnapShot.stream()
                .filter(p -> !newProblems.contains(p))
                .forEach(p -> {processedProblems.add(p); removeProblem(p);});

        newProblems.stream()
                .filter(p -> !activeProblemsSnapShot.contains(p) && !processedProblems.contains(p))
                .forEach(this::addProblem);
    }

    private InlineProblem findActiveProblemByRangeHighlighterHashCode(int hashCode) {
        return findProblemByRangeHighlighterHashCodeFromList(hashCode, activeProblems);
    }

    private InlineProblem findProblemByRangeHighlighterHashCodeFromList(int hashCode, List<InlineProblem> list) {
        return list.stream()
                .filter(p -> p.getRangeHighlighterHashCode() == hashCode)
                .findFirst()
                .orElse(null);
    }
}
