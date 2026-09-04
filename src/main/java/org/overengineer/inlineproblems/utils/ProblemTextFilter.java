package org.overengineer.inlineproblems.utils;

import org.overengineer.inlineproblems.settings.SettingsState;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Matches problem texts against the user configured filter list.
 * <p>
 * The parsed list is cached and only rebuilt when the raw setting changes, because the filter is
 * evaluated for every problem of every scan and, with the MarkupModelListener, for every single
 * highlighter event.
 */
public final class ProblemTextFilter {

    private static volatile String cachedRawFilterList = null;
    private static volatile List<String> cachedFilters = List.of();

    private ProblemTextFilter() {
    }

    /**
     * @param problemText the problem description
     * @return true if the problem should be ignored because its text starts with one of the filters
     */
    public static boolean isFiltered(String problemText) {
        if (problemText == null) {
            return true;
        }

        List<String> filters = getFilters();
        if (filters.isEmpty()) {
            return false;
        }

        String normalizedText = problemText.stripLeading().toLowerCase(Locale.ROOT);

        for (String filter : filters) {
            if (normalizedText.startsWith(filter)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> getFilters() {
        String rawFilterList = SettingsState.getInstance().getProblemFilterList();

        if (!Objects.equals(rawFilterList, cachedRawFilterList)) {
            cachedFilters = parse(rawFilterList);
            cachedRawFilterList = rawFilterList;
        }

        return cachedFilters;
    }

    private static List<String> parse(String rawFilterList) {
        if (rawFilterList == null || rawFilterList.isBlank()) {
            return List.of();
        }

        /* Blank entries have to be dropped: an empty filter would match every problem text
         * through startsWith("") and hide all problems. */
        return Arrays.stream(rawFilterList.split(";"))
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .map(f -> f.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableList());
    }
}
