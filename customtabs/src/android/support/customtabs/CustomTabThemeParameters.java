package android.support.customtabs;

import static android.support.customtabs.CustomTabsIntent.EXTRA_REMOTEVIEWS;
import static android.support.customtabs.CustomTabsIntent.EXTRA_SECONDARY_TOOLBAR_COLOR;
import static android.support.customtabs.CustomTabsIntent.EXTRA_TOOLBAR_COLOR;

import android.app.PendingIntent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

/**
 * Contains visual parameters of a Custom Tab that may depend on the theme.
 * @see CustomTabsIntent.Builder#setThemeParameters(int, CustomTabThemeParameters)
 */
public class CustomTabThemeParameters {
    @Nullable public final Integer toolbarColor;
    @Nullable public final RemoteViews secondaryToolbarViews;
    @Nullable public final Integer secondaryToolbarColor;

    private CustomTabThemeParameters(@Nullable Integer toolbarColor,
            @Nullable RemoteViews secondaryToolbarViews,
            @Nullable Integer secondaryToolbarColor) {
        this.toolbarColor = toolbarColor;
        this.secondaryToolbarViews = secondaryToolbarViews;
        this.secondaryToolbarColor = secondaryToolbarColor;
    }

    /**
     * Packs the parameters into a {@link Bundle}.
     * For backward compatibility and ease of use, the names of keys and the structure of the Bundle
     * is the same as that of Intent extras prior to introducing the themes.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (toolbarColor != null) {
            bundle.putInt(EXTRA_TOOLBAR_COLOR, toolbarColor);
        }
        if (secondaryToolbarViews != null) {
            bundle.putParcelable(EXTRA_REMOTEVIEWS, secondaryToolbarViews);
        }
        if (secondaryToolbarColor != null) {
            bundle.putInt(EXTRA_SECONDARY_TOOLBAR_COLOR, secondaryToolbarColor);
        }
        return bundle;
    }

    /**
     * Unpacks parameters from a {@link Bundle}.
     */
    public static CustomTabThemeParameters fromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle(0);
        }
        // Using bundle.get() instead of bundle.getInt() to default to null without calling
        // bundle.containsKey().
        return new CustomTabThemeParameters(
                (Integer) bundle.get(EXTRA_TOOLBAR_COLOR),
                bundle.getParcelable(EXTRA_REMOTEVIEWS),
                (Integer) bundle.get(EXTRA_SECONDARY_TOOLBAR_COLOR));
    }

    /**
     * Builder class for {@link CustomTabThemeParameters} objects.
     */
    public static class Builder {
        @Nullable private Integer mToolbarColor;
        @Nullable private RemoteViews mSecondaryToolbarViews;
        @Nullable private Integer mSecondaryToolbarColor;

        /**
         * @see CustomTabsIntent.Builder#setToolbarColor(int)
         */
        public Builder setToolbarColor(int color) {
            mToolbarColor = color;
            return this;
        }

        /**
         * @see CustomTabsIntent.Builder#setSecondaryToolbarColor(int)
         */
        public Builder setSecondaryToolbarColor(int color) {
            mSecondaryToolbarColor = color;
            return this;
        }

        /**
         * @see CustomTabsIntent.Builder#setSecondaryToolbarViews(RemoteViews, int[],
         * PendingIntent)
         * Only the RemoteViews can be updated when theme changes.
         */
        public Builder setSecondaryToolbarViews(RemoteViews remoteViews) {
            mSecondaryToolbarViews = remoteViews;
            return this;
        }

        /**
         * Combines all the options that have been into {@link CustomTabThemeParameters} object.
         */
        public CustomTabThemeParameters build() {
            return new CustomTabThemeParameters(mToolbarColor,
                    mSecondaryToolbarViews,
                    mSecondaryToolbarColor);
        }
    }
}
