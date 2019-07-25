package android.support.customtabs.trusted;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs an intent to start a Trusted Web Activity (see {@link TrustedWebUtils} for more
 * details).
 */
public class TrustedWebActivityIntentBuilder {
    private final Uri mUri;
    private final CustomTabsIntent.Builder mIntentBuilder = new CustomTabsIntent.Builder();

    @Nullable
    private List<String> mAdditionalTrustedOrigins;

    @Nullable
    private Bundle mSplashScreenParams;

    /**
     * Creates a Builder given the required parameters.
     * @param uri The web page to launch as Trusted Web Activity.
     */
    public TrustedWebActivityIntentBuilder(Uri uri) {
        mUri = uri;
    }

    /**
     * Sets the color applied to the toolbar and the status bar, see
     * {@link CustomTabsIntent.Builder#setToolbarColor}.
     *
     * When a Trusted Web Activity is on the verified origin, the toolbar is hidden, so the color
     * applies only to the status bar. When it's on an unverified origin, the toolbar is shown, and
     * the color applies to both toolbar and status bar.
     */
    @NonNull
    public TrustedWebActivityIntentBuilder setToolbarColor(@ColorInt int color) {
        mIntentBuilder.setToolbarColor(color);
        return this;
    }

    /**
     * Sets the navigation bar color, see {@link CustomTabsIntent.Builder#setNavigationBarColor}.
     */
    @NonNull
    public TrustedWebActivityIntentBuilder setNavigationBarColor(@ColorInt int color) {
        mIntentBuilder.setNavigationBarColor(color);
        return this;
    }

    /**
     * Sets the color scheme, see {@link CustomTabsIntent.Builder#setColorScheme}.
     * In Trusted Web Activities color scheme may effect such UI elements as info bars and context
     * menus.
     *
     * @param colorScheme Must be one of {@link CustomTabsIntent#COLOR_SCHEME_SYSTEM},
     * {@link CustomTabsIntent#COLOR_SCHEME_LIGHT}, and {@link CustomTabsIntent#COLOR_SCHEME_DARK}.
     */
    @NonNull
    public TrustedWebActivityIntentBuilder setColorScheme(int colorScheme) {
        mIntentBuilder.setColorScheme(colorScheme);
        return this;
    }

    /**
     * Sets {@link CustomTabColorSchemeParams} for the given color scheme.
     * This allows, for example, to set two navigation bar colors - for light and dark scheme.
     * Trusted Web Activity will automatically apply the correct color according to current system
     * settings. For more details see {@link CustomTabsIntent.Builder#setColorSchemeParams}.
     */
    @NonNull
    public TrustedWebActivityIntentBuilder setColorSchemeParams(int colorScheme,
            @NonNull CustomTabColorSchemeParams params) {
        mIntentBuilder.setColorSchemeParams(colorScheme, params);
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
    public TrustedWebActivityIntentBuilder setAdditionalTrustedOrigins(List<String> origins) {
        mAdditionalTrustedOrigins = origins;
        return this;
    }

    /**
     * Sets the parameters of a splash screen shown while the web page is loading, such as
     * background color. See {@link TrustedWebUtils.SplashScreenParamKey} for a list of supported
     * parameters.
     *
     * To provide the image for the splash screen, use {@link TrustedWebUtils#transferSplashImage},
     * prior to launching the intent.
     *
     * It is recommended to also show the same splash screen in the app as soon as possible,
     * prior to establishing a CustomTabConnection. The Trusted Web Activity provider should
     * ensure seamless transition of the splash screen from the app onto the top of webpage
     * being loaded.
     *
     * The splash screen will be removed on the first paint of the page, or when the page load
     * fails.
     */
    public TrustedWebActivityIntentBuilder setSplashScreenParams(Bundle splashScreenParams) {
        mSplashScreenParams = splashScreenParams;
        return this;
    }

    /**
     * Builds the Intent.
     *
     * @param session The {@link CustomTabsSession} to use for launching a Trusted Web Activity.
     */
    public Intent build(CustomTabsSession session) {
        if (session == null) {
            throw new NullPointerException("CustomTabsSession is required for launching a TWA");
        }

        mIntentBuilder.setSession(session);
        Intent intent = mIntentBuilder.build().intent;
        intent.setData(mUri);
        intent.putExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
        if (mAdditionalTrustedOrigins != null) {
            intent.putExtra(TrustedWebUtils.EXTRA_ADDITIONAL_TRUSTED_ORIGINS,
                    new ArrayList<>(mAdditionalTrustedOrigins));
        }

        if (mSplashScreenParams != null) {
            intent.putExtra(TrustedWebUtils.EXTRA_SPLASH_SCREEN_PARAMS, mSplashScreenParams);
        }
        return intent;
    }

    /**
     * Builds a {@link CustomTabsIntent} based on provided parameters.
     * Can be useful for falling back to Custom Tabs when Trusted Web Activity providers are
     * unavailable.
     */
    @NonNull
    public CustomTabsIntent buildCustomTabsIntent() {
        return mIntentBuilder.build();
    }

    /**
     * Returns the {@link Uri} to be launched with this Builder.
     */
    public Uri getUrl() {
        return mUri;
    }

}
