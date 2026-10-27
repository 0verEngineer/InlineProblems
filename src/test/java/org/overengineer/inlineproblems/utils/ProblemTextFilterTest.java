package org.overengineer.inlineproblems.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ProblemTextFilterTest {

    @Test
    void prefixMatchesTextBegin() {
        assertTrue(ProblemTextFilter.isFiltered("Field 'x' is never used", "field"));
        assertTrue(ProblemTextFilter.isFiltered("Unused import statement", "unu"));
        assertFalse(ProblemTextFilter.isFiltered("Field 'x' is never used", "never"));
        assertFalse(ProblemTextFilter.isFiltered("Unused import statement", "import"));
    }

    @ParameterizedTest
    @CsvSource({
            "Field 'x' is never used,todo;fIELD",
            "Unused import statement,UNU",
            "Field 'x' is never used,re:NEVER",
            "Unused import statement,*sTaT*;todo"
    })
    void caseIsIgnored(String problemText, String rawFilterList) {
        assertTrue(ProblemTextFilter.isFiltered(problemText, rawFilterList));
    }

    @Test
    void blankFilterTextIsIgnored() {
        assertFalse(ProblemTextFilter.isFiltered("Unused import statement", ""));
        assertFalse(ProblemTextFilter.isFiltered("Unused import statement", ";;"));
        assertFalse(ProblemTextFilter.isFiltered("Unused import statement", "; ;"));
        assertFalse(ProblemTextFilter.isFiltered("Cannot resolve method 'foo(int)'", "re:"));
    }

    @Test
    void leadingSpacesAreIgnored() {
        assertTrue(ProblemTextFilter.isFiltered("Field 'x' is never used", "   field"));
        assertTrue(ProblemTextFilter.isFiltered("Unused import statement", "  *use*"));
        assertTrue(ProblemTextFilter.isFiltered("      Unused import statement", "Unu"));
    }

    @Test
    void globMatchesTextPattern() {
        assertTrue(ProblemTextFilter.isFiltered("Field 'x' is never used", "fie*"));
        assertTrue(ProblemTextFilter.isFiltered("Unused import statement", "*t st*"));
        assertFalse(ProblemTextFilter.isFiltered("Field 'x' is never used", "field?"));
        assertTrue(ProblemTextFilter.isFiltered("Unused import statement", "*statement"));
    }

    @Test
    void globIgnoresRegexSymbols() {
        assertTrue(ProblemTextFilter.isFiltered("Cannot resolve method 'foo(int)'", "*foo(int)?"));
        assertTrue(ProblemTextFilter.isFiltered("Cannot resolve method 'foo(int)'", "*foo(*"));
        assertTrue(ProblemTextFilter.isFiltered("'Thread.sleep()' called in loop", "*ad.sleep*"));
        assertFalse(ProblemTextFilter.isFiltered("'Thread.sleep' called in loop", "*sleep()*"));
        assertFalse(ProblemTextFilter.isFiltered("'ThreadXsleep()' called in loop", "*ad.sleep*"));
    }

    @Test
    void regexMatchesTextPattern() {
        assertTrue(ProblemTextFilter.isFiltered("'ThreadXsleep()' called in loop", "re:ad.sleep"));
    }

    @Test
    void regexIdentifierCaseIsIgnored() {
        assertTrue(ProblemTextFilter.isFiltered("'ThreadXsleep()' called in loop", "RE:ad.sleep"));
    }

    @Test
    void invalidRegexIsIgnored() {
        assertDoesNotThrow(() -> ProblemTextFilter.isFiltered("Cannot resolve method 'foo(int)'", "re:foo("));
        assertTrue(ProblemTextFilter.isFiltered("Unused import statement", "re:imp(;unused"));
    }

    @Test
    void nullTextIsIgnored() {
        assertTrue(ProblemTextFilter.isFiltered(null, ";;"));
    }

    @Test
    void nullFilterListIsIgnored() {
        assertFalse(ProblemTextFilter.isFiltered("Unused import statement", null));
    }

    @Test
    void changedFilterListIsApplied() {
        assertTrue(ProblemTextFilter.isFiltered("Field 'x' is never used", "field"));
        assertFalse(ProblemTextFilter.isFiltered("Field 'x' is never used", "*fiel"));
        assertFalse(ProblemTextFilter.isFiltered("Field 'x' is never used", "*fiel"));
        assertTrue(ProblemTextFilter.isFiltered("Field 'x' is never used", "field"));
    }
}
