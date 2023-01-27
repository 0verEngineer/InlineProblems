package org.overengineer.inlineproblems;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.overengineer.inlineproblems.entities.InlineProblem;
import org.overengineer.inlineproblems.entities.enums.Listeners;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class InlineDrawer implements Disposable {
    private List<InlineProblem> activeProblems = new ArrayList<>();

    private Logger logger = Logger.getInstance(InlineDrawer.class);
    private ListenerManager listenerManager = new ListenerManager();

    private static final String RIDER_NAME = "JetBrains Rider";

    public InlineDrawer() {
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

        undrawErrorLineHighlight(problem);
        undrawInlineProblemLabel(problem);
        activeProblems.remove(problem);
    }

    public boolean removeProblemWithRefreshFromActiveProblems(InlineProblem problem) {
        if (problem.getProblemInLineCount() > 0) {
            problem.setProblemInLineCount(problem.getProblemInLineCount() - 1);
            return true;
        }

        final List<InlineProblem> activeProblemSnapShot = new ArrayList<>(activeProblems);

        List<InlineProblem> problemsToRemove = activeProblemSnapShot.stream()
                        .filter(p -> p.equals(problem))
                                .collect(Collectors.toList());

        if (problemsToRemove.size() != 1) {
            logger.info("Problem to remove not found, resetting");
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

        if (shouldDrawProblemLabel(problem))
            drawProblemLabel(problem);

        if (shouldDrawProblemHighlighter(problem))
            drawProblemLineHighlight(problem);

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

    public void updateFromListOfNewActiveProblems(List<InlineProblem> problems, Project project, String filePath) {

        if (problems.size() == 0 && problems == activeProblems)
            return;

        final List<InlineProblem> processedProblems = new ArrayList<>();
        final List<InlineProblem> activeProblemsSnapShot = activeProblems.stream()
                .filter(p -> p.getProject().equals(project) && p.getFile().equals(filePath))
                .collect(Collectors.toList());

        activeProblemsSnapShot.stream()
                .filter(p -> !problems.contains(p))
                .forEach(p -> {processedProblems.add(p); removeProblem(p);});

        problems.stream()
                .filter(p -> !activeProblemsSnapShot.contains(p) && !processedProblems.contains(p))
                .forEach(this::addProblem);
    }

    private void drawProblemLabel(InlineProblem problem) {
        Editor editor = problem.getEditor();
        SettingsState settings = SettingsState.getInstance();
        var inlayModel = editor.getInlayModel();

        TextRange textRange = new TextRange(
                editor.getDocument().getLineStartOffset(problem.getLine()),
                editor.getDocument().getLineEndOffset(problem.getLine())
        );
        String lineText = editor.getDocument().getText(textRange);

        InlineProblemLabel inlineProblemLabel = new InlineProblemLabel(
                problem,
                getTextColor(problem, editor),
                getBackgroundColor(problem, editor),
                false,
                settings.isDrawBoxesAroundErrorLabels(),
                settings.isRoundedCornerBoxes(),
                editor
        );

        // To get the potential width of the whole line with the problem text including existent inline elements
        int existingInlineElementsWidth = 0;
        var existingElements = editor.getInlayModel().getAfterLineEndElementsForLogicalLine(problem.getLine());
        for (final var existingElement : existingElements) {
            existingInlineElementsWidth += existingElement.getWidthInPixels();
        }

        int editorWidth = editor.getScrollingModel().getVisibleArea().width;

        Font editorFont = editor.getColorsScheme().getFont(EditorFontType.PLAIN);

        int problemWidth = inlineProblemLabel.calcWidthInPixels() +
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
            inlayModel.addAfterLineEndElement(
                    editor.getDocument().getLineEndOffset(problem.getLine()),
                    true,
                    inlineProblemLabel
            );
        }

        problem.setInlineProblemLabelHashCode(inlineProblemLabel.hashCode());
    }

    private void drawProblemLineHighlight(InlineProblem problem) {
        Editor editor = problem.getEditor();
        TextAttributes textAttributes = new TextAttributes(
                editor.getColorsScheme().getDefaultForeground(),
                getHighlightColor(problem, editor),
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

    private void undrawErrorLineHighlight(InlineProblem problem) {
        MarkupModel markupModel = problem.getEditor().getMarkupModel();

        Arrays.stream(markupModel.getAllHighlighters())
                .filter(h -> h.isValid() && h.hashCode() == problem.getProblemLineHighlighterHashCode())
                .forEach(markupModel::removeHighlighter);
    }

    private void undrawInlineProblemLabel(InlineProblem problem) {
        Editor editor = problem.getEditor();
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

    private boolean shouldDrawProblemLabel(InlineProblem problem) {
        SettingsState settings = SettingsState.getInstance();
        int severity = problem.getSeverity();

        if (severity >= HighlightSeverity.ERROR.myVal &&
                settings.isShowErrors())
            return true;
        else if (severity >= HighlightSeverity.WARNING.myVal &&
                severity < HighlightSeverity.ERROR.myVal && settings.isShowWarnings())
            return true;
        else if (severity >= HighlightSeverity.WEAK_WARNING.myVal &&
                severity < HighlightSeverity.WARNING.myVal && settings.isShowWeakWarnings())
            return true;
        else if (severity >= HighlightSeverity.INFORMATION.myVal &&
                severity < HighlightSeverity.WEAK_WARNING.myVal && settings.isShowInfos())
            return true;

        return false;
    }

    private boolean shouldDrawProblemHighlighter(InlineProblem problem) {
        SettingsState settings = SettingsState.getInstance();
        int severity = problem.getSeverity();

        if (severity >= HighlightSeverity.ERROR.myVal &&
                settings.isHighlightErrors())
            return true;
        else if (severity >= HighlightSeverity.WARNING.myVal &&
                severity < HighlightSeverity.ERROR.myVal && settings.isHighlightWarnings())
            return true;
        else if (severity >= HighlightSeverity.WEAK_WARNING.myVal &&
                severity < HighlightSeverity.WARNING.myVal && settings.isHighlightWeakWarnings())
            return true;
        else if (severity >= HighlightSeverity.INFORMATION.myVal &&
                severity < HighlightSeverity.WEAK_WARNING.myVal && settings.isHighlightInfos())
            return true;

        return false;
    }

    private Color getTextColor(InlineProblem problem, Editor editor) {
        SettingsState settings = SettingsState.getInstance();
        Color color = editor.getColorsScheme().getDefaultForeground();

        if (problem.getSeverity() >= HighlightSeverity.ERROR.myVal)
            color = settings.getErrorTextColor();
        else if (problem.getSeverity() >= HighlightSeverity.WARNING.myVal)
            color = settings.getWarningTextColor();
        else if (problem.getSeverity() >= HighlightSeverity.WEAK_WARNING.myVal)
            color = settings.getWeakWarningTextColor();
        else if (problem.getSeverity() >= HighlightSeverity.INFORMATION.myVal)
            color = settings.getInfoTextColor();

        return color;
    }

    private Color getBackgroundColor(InlineProblem problem, Editor editor) {
        SettingsState settings = SettingsState.getInstance();
        Color backgroundColor = editor.getColorsScheme().getDefaultBackground();

        if (problem.getSeverity() >= HighlightSeverity.ERROR.myVal)
            backgroundColor = settings.getErrorBackgroundColor();
        else if (problem.getSeverity() >= HighlightSeverity.WARNING.myVal)
            backgroundColor = settings.getWarningBackgroundColor();
        else if (problem.getSeverity() >= HighlightSeverity.WEAK_WARNING.myVal)
            backgroundColor = settings.getWeakWarningBackgroundColor();
        else if (problem.getSeverity() >= HighlightSeverity.INFORMATION.myVal)
            backgroundColor = settings.getInfoBackgroundColor();

        return backgroundColor;
    }

    private Color getHighlightColor(InlineProblem problem, Editor editor) {
        SettingsState settings = SettingsState.getInstance();
        Color color = editor.getColorsScheme().getDefaultBackground();

        if (shouldDrawProblemHighlighter(problem)) {
            if (problem.getSeverity() >= HighlightSeverity.ERROR.myVal)
                color = settings.getErrorHighlightColor();
            else if (problem.getSeverity() >= HighlightSeverity.WARNING.myVal)
                color = settings.getWarningHighlightColor();
            else if (problem.getSeverity() >= HighlightSeverity.WEAK_WARNING.myVal)
                color = settings.getWeakWarningHighlightColor();
            else if (problem.getSeverity() >= HighlightSeverity.INFORMATION.myVal)
                color = settings.getInfoHighlightColor();
        }

        return color;
    }
}
