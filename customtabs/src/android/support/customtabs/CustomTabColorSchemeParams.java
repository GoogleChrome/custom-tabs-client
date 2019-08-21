package androidx.browser.customtabs;

import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_NAVIGATION_BAR_COLOR;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_SECONDARY_TOOLBAR_COLOR;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_TOOLBAR_COLOR;

import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contains visual parameters of a Custom Tab that may depend on the color scheme.
 * @see CustomTabsIntent.Builder#setColorSchemeParams(int, CustomTabColorSchemeParams)
 */
public final class CustomTabColorSchemeParams {

    /**
     * Toolbar color. See {@link CustomTabsIntent.Builder#setToolbarColor(int)}.
     */
    @Nullable @ColorInt public final Integer toolbarColor;

    /**
     * Secondary toolbar color. See {@link CustomTabsIntent.Builder#setSecondaryToolbarColor(int)}.
     */
    @Nullable @ColorInt public final Integer secondaryToolbarColor;

    /**
     * Navigation bar color. See {@link CustomTabsIntent.Builder#setNavigationBarColor(int)}.
     */
    @Nullable @ColorInt public final Integer navigationBarColor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    CustomTabColorSchemeParams(
            @Nullable @ColorInt Integer toolbarColor,
            @Nullable @ColorInt Integer secondaryToolbarColor,
            @Nullable @ColorInt Integer navigationBarColor) {
        this.toolbarColor = toolbarColor;
        this.secondaryToolbarColor = secondaryToolbarColor;
        this.navigationBarColor = navigationBarColor;
    }

    /**
     * Packs the parameters into a {@link Bundle}.
     * For backward compatibility and ease of use, the names of keys and the structure of the Bundle
     * are the same as that of Intent extras prior to introducing the themes.
     */
    @NonNull
    Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (toolbarColor != null) {
            bundle.putInt(EXTRA_TOOLBAR_COLOR, toolbarColor);
        }
        if (secondaryToolbarColor != null) {
            bundle.putInt(EXTRA_SECONDARY_TOOLBAR_COLOR, secondaryToolbarColor);
        }
        if (navigationBarColor != null) {
            bundle.putInt(EXTRA_NAVIGATION_BAR_COLOR, navigationBarColor);
        }
        return bundle;
    }

    /**
     * Unpacks parameters from a {@link Bundle}. Sets all parameters to null if provided bundle is
     * null.
     */
    @NonNull
    static CustomTabColorSchemeParams fromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle(0);
        }
        // Using bundle.get() instead of bundle.getInt() to default to null without calling
        // bundle.containsKey().
        return new CustomTabColorSchemeParams(
                (Integer) bundle.get(EXTRA_TOOLBAR_COLOR),
                (Integer) bundle.get(EXTRA_SECONDARY_TOOLBAR_COLOR),
                (Integer) bundle.get(EXTRA_NAVIGATION_BAR_COLOR));
    }

    /**
     * Replaces the null fields with values from provided defaults.
     */
    @NonNull
    CustomTabColorSchemeParams withDefaults(@NonNull CustomTabColorSchemeParams defaults) {
        return new CustomTabColorSchemeParams(
                toolbarColor == null ? defaults.toolbarColor : toolbarColor,
                secondaryToolbarColor == null ? defaults.secondaryToolbarColor
                        : secondaryToolbarColor,
                navigationBarColor == null ? defaults.navigationBarColor : navigationBarColor);
    }

    /**
     * Builder class for {@link CustomTabColorSchemeParams} objects.
     */
    public static final class Builder {
        @Nullable @ColorInt private Integer mToolbarColor;
        @Nullable @ColorInt private Integer mSecondaryToolbarColor;
        @Nullable @ColorInt private Integer mNavigationBarColor;

        /**
         * @see CustomTabsIntent.Builder#setToolbarColor(int)
         */
        @NonNull
        public Builder setToolbarColor(@ColorInt int color) {
            mToolbarColor = color;
            return this;
        }

        /**
         * @see CustomTabsIntent.Builder#setSecondaryToolbarColor(int)
         */
        @NonNull
        public Builder setSecondaryToolbarColor(@ColorInt int color) {
            mSecondaryToolbarColor = color;
            return this;
        }

        /**
         * @see CustomTabsIntent.Builder#setNavigationBarColor(int)
         */
        @NonNull
        public Builder setNavigationBarColor(@ColorInt int color) {
            mNavigationBarColor = color;
            return this;
        }

        /**
         * Combines all the options that have been into a {@link CustomTabColorSchemeParams}
         * object.
         */
        @NonNull
        public CustomTabColorSchemeParams build() {
            return new CustomTabColorSchemeParams(mToolbarColor, mSecondaryToolbarColor,
                    mNavigationBarColor);
        }
    }
}

