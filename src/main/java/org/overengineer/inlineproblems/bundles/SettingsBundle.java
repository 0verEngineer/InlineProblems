package org.overengineer.inlineproblems.bundles;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.Locale;
import java.util.ResourceBundle;
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

    private static final ResourceBundle DEFAULT_BUNDLE = ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH);

    private static final SettingsBundle INSTANCE = new SettingsBundle();

    private SettingsBundle() {
        super(BUNDLE);
        try {
            ResourceBundle.getBundle(BUNDLE, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(Locale.ENGLISH);
        }
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.messageOrDefault(key, DEFAULT_BUNDLE.getString(key), params);
    }

    @NotNull
    public static Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        var result = INSTANCE.messageOrNull(key, params);
        if (result == null) {
            return () -> DEFAULT_BUNDLE.getString(key);
        }
        return () -> result;
    }
}