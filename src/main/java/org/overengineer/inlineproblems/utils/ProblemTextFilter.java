package org.overengineer.inlineproblems.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Matches problem texts against the user configured filter list.
 * <p>
 * A filter entry is interpreted as
 * <ul>
 *     <li>a regular expression, if it starts with {@code re:} (partial match)</li>
 *     <li>a glob, if it contains {@code *} or {@code ?} (whole text match)</li>
 *     <li>a problem text beginning otherwise, which is the legacy behaviour</li>
 * </ul>
 * Matching is case insensitive in all three cases.
 * <p>
 * The parsed list is cached and only rebuilt when the raw setting changes, because the filter is
 * evaluated for every problem of every scan and, with the MarkupModelListener, for every single
 * highlighter event.
 */
public final class ProblemTextFilter {

    private static final String REGEX_PREFIX = "re:";
    private static final int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Logger logger = Logger.getInstance(ProblemTextFilter.class);

    private static volatile String cachedRawFilterList = null;
    private static volatile List<Predicate<String>> cachedFilters = List.of();

    private ProblemTextFilter() {
    }

    /**
     * @param problemText the problem description
     * @return true if the problem should be ignored because it matches one of the filters
     */
    public static boolean isFiltered(String problemText) {
        if (problemText == null) {
            return true;
        }

        List<Predicate<String>> filters = getFilters();
        if (filters.isEmpty()) {
            return false;
        }

        String normalizedText = problemText.stripLeading().toLowerCase(Locale.ROOT);

        for (Predicate<String> filter : filters) {
            if (filter.test(normalizedText)) {
                return true;
            }
        }

        return false;
    }

    private static List<Predicate<String>> getFilters() {
        String rawFilterList = SettingsState.getInstance().getProblemFilterList();

        if (!Objects.equals(rawFilterList, cachedRawFilterList)) {
            cachedFilters = parse(rawFilterList);
            cachedRawFilterList = rawFilterList;
        }

        return cachedFilters;
    }

    private static List<Predicate<String>> parse(String rawFilterList) {
        if (rawFilterList == null || rawFilterList.isBlank()) {
            return List.of();
        }

        List<Predicate<String>> filters = new ArrayList<>();

        for (String entry : rawFilterList.split(";")) {
            String trimmedEntry = entry.trim();

            /* Blank entries have to be dropped: an empty filter would match every problem text
             * through startsWith("") and hide all problems. */
            if (trimmedEntry.isEmpty()) {
                continue;
            }

            Predicate<String> filter = toFilter(trimmedEntry);
            if (filter != null) {
                filters.add(filter);
            }
        }

        return List.copyOf(filters);
    }

    private static Predicate<String> toFilter(String entry) {
        if (entry.regionMatches(true, 0, REGEX_PREFIX, 0, REGEX_PREFIX.length())) {
            return toRegexFilter(entry.substring(REGEX_PREFIX.length()).trim(), entry);
        }

        if (entry.indexOf('*') >= 0 || entry.indexOf('?') >= 0) {
            return toGlobFilter(entry);
        }

        String textBeginning = entry.toLowerCase(Locale.ROOT);
        return text -> text.startsWith(textBeginning);
    }

    private static Predicate<String> toRegexFilter(String regex, String entry) {
        if (regex.isEmpty()) {
            return null;
        }

        try {
            Pattern pattern = Pattern.compile(regex, PATTERN_FLAGS);
            return text -> pattern.matcher(text).find();
        }
        catch (PatternSyntaxException e) {
            logger.warn("Ignoring problem filter '" + entry + "', it is not a valid regular expression", e);
            return null;
        }
    }

    private static Predicate<String> toGlobFilter(String glob) {
        Pattern pattern = Pattern.compile(globToRegex(glob), PATTERN_FLAGS);
        return text -> pattern.matcher(text).matches();
    }

    /**
     * Translates a glob into a regular expression. Everything but {@code *} and {@code ?} is
     * quoted, so a filter text can safely contain regex meta characters like {@code (} or
     * {@code .}, which problem texts are full of.
     */
    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        StringBuilder literal = new StringBuilder();

        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);

            if (c != '*' && c != '?') {
                literal.append(c);
                continue;
            }

            if (literal.length() > 0) {
                regex.append(Pattern.quote(literal.toString()));
                literal.setLength(0);
            }

            regex.append(c == '*' ? ".*" : ".");
        }

        if (literal.length() > 0) {
            regex.append(Pattern.quote(literal.toString()));
        }

        return regex.toString();
    }
}
