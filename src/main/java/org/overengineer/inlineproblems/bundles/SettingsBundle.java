package org.overengineer.inlineproblems.bundles;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * The SettingsBundle class is a dynamic bundle for managing internationalized messages.
 * It extends the DynamicBundle class.
 *
 * @author kuwei
 */
public final class SettingsBundle extends DynamicBundle {
    @NonNls
    public static final String BUNDLE = "messages.SettingsBundle";
    private static final SettingsBundle INSTANCE = new SettingsBundle();

    private SettingsBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}