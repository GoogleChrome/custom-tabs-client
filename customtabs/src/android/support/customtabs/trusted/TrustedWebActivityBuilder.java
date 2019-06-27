package android.support.customtabs.trusted;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabColorSchemeParams;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs and launches an intent to start a Trusted Web Activity.
 */
public class TrustedWebActivityBuilder {
    private final Context mContext;
    private final Uri mUri;

    @Nullable @ColorInt private Integer mStatusBarColor;
    @Nullable @ColorInt private Integer mNavigationBarColor;
    @Nullable private Integer mColorScheme;
    @Nullable private SparseArray<CustomTabColorSchemeParams> mColorSchemeParams;

    @Nullable
    private List<String> mAdditionalTrustedOrigins;

    @Nullable
    private Bundle mSplashScreenParams;

    /**
     * Creates a Builder given the required parameters.
     * @param context {@link Context} to use.
     * @param uri The web page to launch as Trusted Web Activity.
     */
    public TrustedWebActivityBuilder(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    /**
     * Sets the status bar color to be seen while the Trusted Web Activity is running.
     */
    @NonNull
    public TrustedWebActivityBuilder setStatusBarColor(int color) {
        mStatusBarColor = color;
        return this;
    }

    /**
     * Sets the navigation bar color, see {@link CustomTabsIntent.Builder#setNavigationBarColor}.
     */
    @NonNull
    public TrustedWebActivityBuilder setNavigationBarColor(@ColorInt int color) {
        mNavigationBarColor = color;
        return this;
    }

    /**
     * Sets the color scheme, see {@link CustomTabsIntent.Builder#setColorScheme}.
     * In Trusted Web Activities color scheme may effect such UI elements as info bars and context
     * menus.
     */
    @NonNull
    public TrustedWebActivityBuilder setColorScheme(int colorScheme) {
        mColorScheme = colorScheme;
        return this;
    }

    /**
     * Sets {@link CustomTabColorSchemeParams} for the given color scheme.
     * This allows, for example, to set two status bar colors - for light and dark scheme. Trusted
     * Web Activity will automatically apply the correct color according to current system settings.
     * For more details see {@link CustomTabsIntent.Builder#setColorSchemeParams}.
     *
     * Note: to set status bar color in the params, use
     * {@link CustomTabColorSchemeParams.Builder#setToolbarColor}.
     */
    @NonNull
    public TrustedWebActivityBuilder setColorSchemeParams(int colorScheme,
            @NonNull CustomTabColorSchemeParams params) {
        if (mColorSchemeParams == null) {
            mColorSchemeParams = new SparseArray<>();
        }
        mColorSchemeParams.put(colorScheme, params);
        return this;
    }

    /**
     * Sets a list of additional trusted origins that the user may navigate or be redirected to
     * from the starting uri.
     *
     * For example, if the user starts at https://www.example.com/page1 and is redirected to
     * https://m.example.com/page2, and both origins are associated with the calling application
     * via the Digital Asset Links, then pass "https://www.example.com/page1" as uri and
     * Arrays.asList("https://m.example.com") as additionalTrustedOrigins.
     *
     * Alternatively, use {@link CustomTabsSession#validateRelationship} to validate additional
     * origins asynchronously, but that would delay launching the Trusted Web Activity.
     *
     * Note: Chrome supports additionalTrustedOrigins only in version 74 and up.
     * For older versions please use {@link CustomTabsSession#validateRelationship}.
     */
    public TrustedWebActivityBuilder setAdditionalTrustedOrigins(List<String> origins) {
        mAdditionalTrustedOrigins = origins;
        return this;
    }

    /**
     * Sets the parameters of a splash screen shown while the web page is loading, such as
     * background color. See {@link TrustedWebUtils.SplashScreenParamKey} for a list of supported
     * parameters.
     *
     * To provide the image for the splash screen, use {@link TrustedWebUtils#transferSplashImage},
     * prior to calling {@link #launchActivity} on the builder.
     *
     * It is recommended to also show the same splash screen in the app as soon as possible,
     * prior to establishing a CustomTabConnection. The Trusted Web Activity provider should
     * ensure seamless transition of the splash screen from the app onto the top of webpage
     * being loaded.
     *
     * The splash screen will be removed on the first paint of the page, or when the page load
     * fails.
     */
    public TrustedWebActivityBuilder setSplashScreenParams(Bundle splashScreenParams) {
        mSplashScreenParams = splashScreenParams;
        return this;
    }

    /**
     * Launches a Trusted Web Activity. Once it is launched, browser side implementations may
     * have their own fallback behavior (e.g. showing the page in a custom tab UI with toolbar).
     *
     * @param session The {@link CustomTabsSession} to use for launching a Trusted Web Activity.
     */
    public void launchActivity(CustomTabsSession session) {
        if (session == null) {
            throw new NullPointerException("CustomTabsSession is required for launching a TWA");
        }

        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder(session);
        if (mStatusBarColor != null) {
            // Toolbar color applies also to the status bar.
            intentBuilder.setToolbarColor(mStatusBarColor);
        }
        if (mNavigationBarColor != null) {
            intentBuilder.setNavigationBarColor(mNavigationBarColor);
        }
        if (mColorScheme != null) {
            intentBuilder.setColorScheme(mColorScheme);
        }
        if (mColorSchemeParams != null) {
            for (int i = 0; i < mColorSchemeParams.size(); i++) {
                intentBuilder.setColorSchemeParams(mColorSchemeParams.keyAt(i),
                        mColorSchemeParams.valueAt(i));
            }
        }

        Intent intent = intentBuilder.build().intent;
        intent.setData(mUri);
        intent.putExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
        if (mAdditionalTrustedOrigins != null) {
            intent.putExtra(TrustedWebUtils.EXTRA_ADDITIONAL_TRUSTED_ORIGINS,
                    new ArrayList<>(mAdditionalTrustedOrigins));
        }

        if (mSplashScreenParams != null) {
            intent.putExtra(TrustedWebUtils.EXTRA_SPLASH_SCREEN_PARAMS, mSplashScreenParams);
        }
        ContextCompat.startActivity(mContext, intent, null);
    }

    /**
     * Returns the {@link Uri} to be launched with this Builder.
     */
    public Uri getUrl() {
        return mUri;
    }

    /**
     * Returns the color set via {@link #setStatusBarColor(int)} or null if not set.
     */
    @Nullable
    public Integer getStatusBarColor() {
        return mStatusBarColor;
    }

    /**
     * Returns the color set via {@link #setNavigationBarColor} or {@code null} if not set.
     */
    @Nullable
    @ColorInt
    public Integer getNavigationBarColor() {
        return mNavigationBarColor;
    }

    /**
     * Returns the color scheme set via {@link #setColorScheme} or {@code null} if not set.
     */
    @Nullable
    public Integer getColorScheme() {
        return mColorScheme;
    }

    /**
     * Returns the color scheme params set via {@link #setColorSchemeParams} for the given scheme
     * or {@code null} if not set.
     */
    @Nullable
    public CustomTabColorSchemeParams getColorSchemeParams(int colorScheme) {
        return mColorSchemeParams == null ? null : mColorSchemeParams.get(colorScheme);
    }

}
