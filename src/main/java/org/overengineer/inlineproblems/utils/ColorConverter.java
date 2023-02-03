package org.overengineer.inlineproblems.utils;

import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public class ColorConverter extends Converter<Color> {
    @Nullable
    @Override
    public Color fromString(@NotNull String value) {
        if (value.isEmpty() || value.isBlank()) {
            return null;
        }

        try {
            return Color.decode(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public @Nullable String toString(@NotNull Color value) {
        return "#" + Integer.toHexString(value.getRGB()).substring(2);
    }
}
