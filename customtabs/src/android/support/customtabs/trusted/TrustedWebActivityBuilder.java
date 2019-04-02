package android.support.customtabs.trusted;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs and launches an intent to start a Trusted Web Activity.
 */
public class TrustedWebActivityBuilder {
    private final Context mContext;
    private final CustomTabsIntent.Builder mIntentBuilder;
    private final Uri mUri;

    @Nullable
    private List<String> mAdditionalTrustedOrigins;

    @Nullable
    private Bundle mSplashScreenParams;

    /**
     * Creates a Builder given the required parameters.
     * @param context {@link Context} to use.
     * @param session The {@link CustomTabsSession} to use for launching a Trusted Web Activity.
     * @param uri The web page to launch as Trusted Web Activity.
     */
    public TrustedWebActivityBuilder(Context context, CustomTabsSession session, Uri uri) {
        mContext = context;
        mIntentBuilder = new CustomTabsIntent.Builder(session);
        mUri = uri;
    }

    /**
     * Sets the status bar color to be seen while the Trusted Web Activity is running.
     */
    public TrustedWebActivityBuilder setStatusBarColor(int color) {
        mIntentBuilder.setToolbarColor(color); // Toolbar color applies also to the status bar.
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
     */
    public void launchActivity() {
        Intent intent = mIntentBuilder.build().intent;
        if (!intent.hasExtra(CustomTabsIntent.EXTRA_SESSION)) {
            throw new IllegalArgumentException(
                    "The CustomTabsIntent should be associated with a CustomTabsSession");
        }
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
}
