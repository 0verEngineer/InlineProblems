package org.overengineer.inlineproblems;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.overengineer.inlineproblems.entities.DrawDetails;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.*;
import java.util.stream.Collectors;


public class ProblemManager implements Disposable {
    private static final Comparator<InlineProblem> HIGHEST_SEVERITY_FIRST =
            (p1, p2) -> Integer.compare(p2.getSeverity(), p1.getSeverity());

    @Getter
    private final List<InlineProblem> activeProblems = new ArrayList<>();

    private final InlineDrawer inlineDrawer = new InlineDrawer();

    private final SettingsState settingsState = SettingsState.getInstance();

    private final Logger logger = Logger.getInstance(ProblemManager.class);

    public void dispose() {
        reset();
    }

    public void removeProblem(InlineProblem problem) {
        List<InlineProblem> problemsInLine = null;
        if (settingsState.isShowAnyGutterIcons()) {
            problemsInLine = getProblemsInLineForProblemSorted(problem);
        }

        inlineDrawer.undrawErrorLineHighlight(problem, problemsInLine);
        inlineDrawer.undrawInlineProblemLabel(problem);

        if (!activeProblems.remove(problem)) {
            logger.warn("Removal of problem failed, resetting");
            resetForEditor(problem.getTextEditor().getEditor());
        }
    }

    /**
     * To add problems, if there are existing problems in the same line, they will be removed and re-added to ensure the
     * correct order (ordered by severity)
     * @param problem problem to add
     */
    public void addProblem(InlineProblem problem) {
        problem.setDrawDetails(new DrawDetails(problem, problem.getTextEditor().getEditor()));

        List<InlineProblem> problemsInLine = getProblemsInLineForProblem(problem);
        problemsInLine.add(problem);
        problemsInLine.sort(HIGHEST_SEVERITY_FIRST);

        problemsInLine.forEach(p -> {
            if (p != problem)
                removeProblem(p);
        });

        // Limit problems per line
        int maxProblemsPerLine = settingsState.getMaxProblemsPerLine();
        if (maxProblemsPerLine > 0 && problemsInLine.size() > maxProblemsPerLine) {
            problemsInLine.subList(maxProblemsPerLine, problemsInLine.size()).clear();
        }

        /* This only works when using a method reference, if we move the code from the addProblemPrivate func into a lambda
        *  it does not work like expected, that is because there are differences in the evaluation and the way it is called */
        problemsInLine.forEach(this::addProblemPrivate);

        inlineDrawer.drawLineHighlighterAndGutterIcon(problemsInLine);
    }

    private void addProblemPrivate(InlineProblem problem) {
        if (problem.getTextEditor().getEditor().getDocument().getLineCount() <= problem.getLine()) {
            logger.warn("Line count is less or equal than problem line, problem not added");
            return;
        }

        inlineDrawer.drawProblemLabel(problem);
        activeProblems.add(problem);
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
                problem.setSeverity(HighlightSeverity.INFORMATION.myVal);
                return;
            }
        }
    }

    public void reset() {
        final List<InlineProblem> activeProblemSnapShot = List.copyOf(activeProblems);

        runInInlayBatchMode(
                collectEditors(activeProblemSnapShot, List.of()),
                0,
                () -> activeProblemSnapShot.forEach(this::removeProblem)
        );
    }

    /**
     * Removes all problems of the given project including their drawn elements. To be called
     * while the project is closing, the editors are still alive at that point.
     */
    public void resetForProject(Project project) {
        final List<InlineProblem> problemsToRemove = activeProblems.stream()
                .filter(p -> Objects.equals(p.getProject(), project))
                .collect(Collectors.toList());

        runInInlayBatchMode(
                collectEditors(problemsToRemove, List.of()),
                0,
                () -> problemsToRemove.forEach(this::removeProblem)
        );
    }

    /**
     * Drops all problems that belong to an editor or project that is already gone. Their inlays
     * and highlighters died with the editor, so nothing has to be undrawn - undrawing would
     * even mean touching a disposed editor.
     */
    public void removeObsoleteProblems() {
        final List<InlineProblem> obsoleteProblems = activeProblems.stream()
                .filter(ProblemManager::isObsolete)
                .collect(Collectors.toList());

        if (obsoleteProblems.isEmpty()) {
            return;
        }

        activeProblems.removeAll(obsoleteProblems);
        logger.debug("Dropped " + obsoleteProblems.size() + " problem(s) of closed editors");
    }

    private static boolean isObsolete(InlineProblem problem) {
        Project project = problem.getProject();
        if (project == null || project.isDisposed()) {
            return true;
        }

        return !problem.getTextEditor().isValid() || problem.getTextEditor().getEditor().isDisposed();
    }

    public void resetForEditor(Editor editor) {
        final List<InlineProblem> problemsToRemove = activeProblems.stream()
                .filter(aP -> aP.getTextEditor().getEditor().equals(editor))
                .collect(Collectors.toList());

        runInInlayBatchMode(
                collectEditors(problemsToRemove, List.of()),
                0,
                () -> problemsToRemove.forEach(this::removeProblem)
        );
    }

    public void updateFromNewActiveProblems(List<InlineProblem> problems) {
        updateFromNewActiveProblems(problems, List.copyOf(activeProblems));
    }

    /**
     * Diffs the problems of a single editor. Filtering by project and file instead of by editor
     * would break a split view: the snapshot would contain the problems of both editors showing
     * the file, while the scan only ever covers one of them, so the two editors would keep
     * removing each others problems.
     */
    public void updateFromNewActiveProblemsForTextEditor(List<InlineProblem> problems, TextEditor textEditor) {
        final List<InlineProblem> activeProblemsSnapShot = activeProblems.stream()
                .filter(p -> Objects.equals(p.getTextEditor(), textEditor))
                .collect(Collectors.toList());

        updateFromNewActiveProblems(problems, activeProblemsSnapShot);
    }

    /**
     * @return a mutable list, the caller is allowed to modify it (InlineDrawer removes the
     *         problem that is being undrawn from it)
     */
    private List<InlineProblem> getProblemsInLineForProblem(InlineProblem problem) {
        return activeProblems.stream()
                .filter(p -> Objects.equals(p.getTextEditor(), problem.getTextEditor()) && p.getLine() == problem.getLine())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<InlineProblem> getProblemsInLineForProblemSorted(InlineProblem problem) {
        List<InlineProblem> problemsInLine = getProblemsInLineForProblem(problem);
        problemsInLine.sort(HIGHEST_SEVERITY_FIRST);

        return problemsInLine;
    }

    /**
     * Updates the active problems based on a list of new problems, problems can also be added and removed one by one,
     * like the MarkupModelProblemListener does, but if the feature "Show only highest severity per line" is enabled,
     * this function needs to be used.
     */
    private void updateFromNewActiveProblems(List<InlineProblem> newProblems, List<InlineProblem> activeProblemsSnapShot) {
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

        final List<InlineProblem> problemsToApply = usedProblems;

        /* Every added or removed inlay makes the editor recalculate its preferred size
         * (EditorSizeManager.validateSize, the hotspot in the traces of issue #96). In batch mode
         * that happens once for the whole diff instead of once per inlay, which is what makes a
         * bulk change of a few thousand problems affordable. */
        runInInlayBatchMode(
                collectEditors(activeProblemsSnapShot, problemsToApply),
                0,
                () -> applyProblemDiff(problemsToApply, activeProblemsSnapShot)
        );
    }

    private List<Editor> collectEditors(List<InlineProblem> first, List<InlineProblem> second) {
        final Set<Editor> editors = new LinkedHashSet<>();

        for (InlineProblem problem : first) {
            editors.add(problem.getTextEditor().getEditor());
        }

        for (InlineProblem problem : second) {
            editors.add(problem.getTextEditor().getEditor());
        }

        editors.removeIf(Editor::isDisposed);

        return new ArrayList<>(editors);
    }

    /** Nests one InlayModel.execute per involved editor around the operation. */
    private void runInInlayBatchMode(List<Editor> editors, int index, Runnable operation) {
        if (index >= editors.size()) {
            operation.run();
            return;
        }

        editors.get(index)
                .getInlayModel()
                .execute(true, () -> runInInlayBatchMode(editors, index + 1, operation));
    }

    private void applyProblemDiff(List<InlineProblem> usedProblems, List<InlineProblem> activeProblemsSnapShot) {
        /* Hash based lookups, the lists can hold thousands of problems and both loops below used
         * to run a linear search per element. */
        final Set<InlineProblem> usedProblemSet = new HashSet<>(usedProblems);
        final Map<InlineProblem, InlineProblem> knownProblems = new HashMap<>();

        for (InlineProblem problem : activeProblemsSnapShot) {
            knownProblems.putIfAbsent(problem, problem);
        }

        activeProblemsSnapShot.stream()
                .filter(p -> !usedProblemSet.contains(p))
                .forEach(this::removeProblem);

        for (InlineProblem problem : usedProblems) {
            InlineProblem knownProblem = knownProblems.get(problem);

            /* The problem is already drawn and only its offsets may have shifted. Redrawing it
             * would dispose and recreate its inlay for nothing, and every inlay change makes the
             * editor recalculate its preferred size. */
            if (knownProblem != null) {
                knownProblem.refreshPositionFrom(problem);
                continue;
            }

            addProblem(problem);

            // Also keeps a second, identical problem in the same batch from being drawn twice
            knownProblems.put(problem, problem);
        }
    }
}
