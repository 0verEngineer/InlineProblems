package org.overengineer.inlineproblems;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.entities.enums.Listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class ProblemManager implements Disposable {
    private final List<InlineProblem> activeProblems = new ArrayList<>();

    private final ListenerManager listenerManager = new ListenerManager();

    private final InlineDrawer inlineDrawer = new InlineDrawer();

    private final Logger logger = Logger.getInstance(ProblemManager.class);

    private static final String RIDER_NAME = "JetBrains Rider";

    public ProblemManager() {
        if (ApplicationInfo.getInstance().getFullApplicationName().startsWith(RIDER_NAME)) {
            // todo unity detection
        }
    }

    public void dispose() {
        reset();
    }

    public void removeProblem(InlineProblem problem) {
        if (problem.getProblemInLineCount() > 0) {
            problem.setProblemInLineCount(problem.getProblemInLineCount() - 1);
            return;
        }

        inlineDrawer.undrawErrorLineHighlight(problem);
        inlineDrawer.undrawInlineProblemLabel(problem);

        if (!activeProblems.remove(problem)) {
            logger.error("Removal of problem failed");
        }
    }

    public boolean removeProblemWithRefreshFromActiveProblems(InlineProblem problem) {
        if (problem.getProblemInLineCount() > 0) {
            problem.setProblemInLineCount(problem.getProblemInLineCount() - 1);
            return true;
        }

        final List<InlineProblem> activeProblemSnapShot = new ArrayList<>(activeProblems);

        List<InlineProblem> problemsToRemove = activeProblemSnapShot.stream()
                .filter(p -> p.equals(problem))
                .toList();

        if (problemsToRemove.size() != 1) {
            logger.warn("Problem to remove not found, resetting");
            reset();
            return false;
        }

        for (var problemToRemove : problemsToRemove) {
            removeProblem(problemToRemove);
        }

        return true;
    }

    public void addProblem(InlineProblem problem) {
        AtomicBoolean problemExistsInLine = new AtomicBoolean(false);

        activeProblems.stream()
                .filter(p -> p.getText().equals(problem.getText()) && p.getLine() == problem.getLine())
                .findFirst()
                .ifPresent(p -> {
                    p.setProblemInLineCount(p.getProblemInLineCount() + 1);
                    problemExistsInLine.set(true);
                });

        if (problemExistsInLine.get())
            return;

        inlineDrawer.drawProblemLabel(problem);
        inlineDrawer.drawProblemLineHighlight(problem);

        activeProblems.add(problem);
    }

    public void reset(int enabledListener, boolean listenerChanged) {
        reset();

        if (listenerChanged && enabledListener == Listeners.MARKUP_MODEL_LISTENER) {
            listenerManager.installMarkupModelListenerOnAllProjects();
        }
    }

    public void reset() {
        final List<InlineProblem> activeProblemSnapShot = new ArrayList<>(activeProblems);
        activeProblemSnapShot.forEach(this::removeProblem);
    }

    public void updateFromNewActiveProblems(List<InlineProblem> problems) {
        final List<InlineProblem> activeProblemsSnapShot = List.copyOf(activeProblems);

        updateFromNewActiveProblems(problems, activeProblemsSnapShot);
    }

    public void updateFromNewActiveProblemsForProjectAndFile(List<InlineProblem> problems, Project project, String filePath) {
        final List<InlineProblem> activeProblemsSnapShot = activeProblems.stream()
                .filter(p -> p.getProject().equals(project) && p.getFile().equals(filePath))
                .toList();

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
}
