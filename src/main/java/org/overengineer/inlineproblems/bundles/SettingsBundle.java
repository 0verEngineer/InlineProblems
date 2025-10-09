package org.overengineer.inlineproblems.bundles;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/* The SettingsBundle class is a dynamic bundle for managing internationalized messages.
 * It extends the DynamicBundle class.
 */
public final class SettingsBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.SettingsBundle";
    private static final SettingsBundle INSTANCE = new SettingsBundle();

    private SettingsBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static Supplier<String> messagePointer(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}