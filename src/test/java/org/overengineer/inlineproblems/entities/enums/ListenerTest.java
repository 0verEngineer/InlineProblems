package org.overengineer.inlineproblems.entities.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListenerTest {
    @Test
    void idsAreUnique() {
        Set<Integer> knownIds = new HashSet<>();
        List<Integer> duplicateIds = new ArrayList<>();

        for (Listener listener : Listener.values()) {
            if (!knownIds.add(listener.getId())) {
                duplicateIds.add(listener.getId());
            }
        }

        assertEquals(List.of(), duplicateIds);
    }

    @Test
    void fromIdReturnsCorrectListener() {
        List<Listener> mismatchedListeners = new ArrayList<>();

        for (Listener listener : Listener.values()) {
            if (listener != Listener.fromId(listener.getId())) {
                mismatchedListeners.add(listener);
            }
        }

        assertEquals(List.of(), mismatchedListeners);
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 99, Integer.MAX_VALUE})
    void fromIdReturnsDefaultForUnknownId(int unknownId) {
        assertEquals(Listener.DEFAULT, Listener.fromId(unknownId));
    }
}
