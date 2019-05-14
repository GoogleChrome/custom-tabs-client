package android.support.customtabs;

import static android.support.customtabs.CustomTabsIntent.EXTRA_SECONDARY_TOOLBAR_COLOR;
import static android.support.customtabs.CustomTabsIntent.EXTRA_TOOLBAR_COLOR;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Contains visual parameters of a Custom Tab that may depend on the color scheme.
 * @see CustomTabsIntent.Builder#setColorSchemeParameters(int, CustomTabColorSchemeParams)
 */
public class CustomTabColorSchemeParams {
    @Nullable public final Integer toolbarColor;
    @Nullable public final Integer secondaryToolbarColor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    CustomTabColorSchemeParams(
            @Nullable Integer toolbarColor,
            @Nullable Integer secondaryToolbarColor) {
        this.toolbarColor = toolbarColor;
        this.secondaryToolbarColor = secondaryToolbarColor;
    }

    /**
     * Packs the parameters into a {@link Bundle}.
     * For backward compatibility and ease of use, the names of keys and the structure of the Bundle
     * are the same as that of Intent extras prior to introducing the themes.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (toolbarColor != null) {
            bundle.putInt(EXTRA_TOOLBAR_COLOR, toolbarColor);
        }
        if (secondaryToolbarColor != null) {
            bundle.putInt(EXTRA_SECONDARY_TOOLBAR_COLOR, secondaryToolbarColor);
        }
        return bundle;
    }

    /**
     * Unpacks parameters from a {@link Bundle}. Sets all parameters to null if provided bundle is
     * null.
     */
    @NonNull
    public static CustomTabColorSchemeParams fromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle(0);
        }
        // Using bundle.get() instead of bundle.getInt() to default to null without calling
        // bundle.containsKey().
        return new CustomTabColorSchemeParams(
                (Integer) bundle.get(EXTRA_TOOLBAR_COLOR),
                (Integer) bundle.get(EXTRA_SECONDARY_TOOLBAR_COLOR));
    }

    /**
     * Replaces the null fields with values from provided defaults.
     */
    @NonNull
    public CustomTabColorSchemeParams withDefaults(@NonNull CustomTabColorSchemeParams defaults) {
        return new CustomTabColorSchemeParams(
                toolbarColor == null ? defaults.toolbarColor : toolbarColor,
                secondaryToolbarColor == null ? defaults.secondaryToolbarColor
                        : secondaryToolbarColor);
    }

    /**
     * Builder class for {@link CustomTabColorSchemeParams} objects.
     */
    public static class Builder {
        @Nullable private Integer mToolbarColor;
        @Nullable private Integer mSecondaryToolbarColor;

        /**
         * @see CustomTabsIntent.Builder#setToolbarColor(int)
         */
        @NonNull
        public Builder setToolbarColor(int color) {
            mToolbarColor = color;
            return this;
        }

        /**
         * @see CustomTabsIntent.Builder#setSecondaryToolbarColor(int)
         */
        @NonNull
        public Builder setSecondaryToolbarColor(int color) {
            mSecondaryToolbarColor = color;
            return this;
        }

        /**
         * Combines all the options that have been into a {@link CustomTabColorSchemeParams}
         * object.
         */
        @NonNull
        public CustomTabColorSchemeParams build() {
            return new CustomTabColorSchemeParams(mToolbarColor, mSecondaryToolbarColor);
        }
    }
}

