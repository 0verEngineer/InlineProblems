package org.overengineer.inlineproblems.utils;

import com.intellij.ui.ColorUtil;
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
        /* ColorUtil zero pads every component. The previous implementation used
         * Integer.toHexString(getRGB()).substring(2), which relies on the alpha byte producing
         * the two leading characters. That holds for opaque colors, but for an alpha below 0x10
         * toHexString drops the leading zeros and substring(2) cuts into the red component. */
        return "#" + ColorUtil.toHex(value);
    }
}
