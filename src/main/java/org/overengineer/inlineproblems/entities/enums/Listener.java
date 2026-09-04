package org.overengineer.inlineproblems.entities.enums;

import lombok.Getter;


/**
 * The available problem detection strategies.
 * <p>
 * The declaration order defines the order in the settings combo box, the id is what gets
 * persisted. Do not change existing ids, they are stored in the users settings file.
 */
@Getter
public enum Listener {
    HIGHLIGHT_PROBLEMS_LISTENER(0, "HighlightProblemListener"),
    MARKUP_MODEL_LISTENER(1, "MarkupModelListener (default)"),
    MANUAL_SCANNING(2, "ManualScanner");

    public static final Listener DEFAULT = MARKUP_MODEL_LISTENER;

    private final int id;
    private final String displayName;

    Listener(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static Listener fromId(int id) {
        for (Listener listener : values()) {
            if (listener.id == id) {
                return listener;
            }
        }

        return DEFAULT;
    }
}
