package org.overengineer.inlineproblems;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

public class GutterRenderer extends GutterIconRenderer {

    private final String text;
    private final Icon icon;

    public GutterRenderer(String text, @NotNull Icon icon) {
        this.text = text;
        this.icon = icon;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GutterRenderer) {
            GutterRenderer other = (GutterRenderer) obj;
            return Objects.equals(getIcon(), other.getIcon()) && Objects.equals(getTooltipText(), other.getTooltipText());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIcon(), getTooltipText());
    }

    @Override
    public @NotNull Icon getIcon() {
        return icon;
    }

    @Override
    public @NotNull String getTooltipText() {
        return text;
    }
}
