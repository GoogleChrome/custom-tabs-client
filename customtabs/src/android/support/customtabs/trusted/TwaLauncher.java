package android.support.customtabs.trusted;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.customtabs.trusted.TwaProviderPicker.LaunchMode;
import android.support.customtabs.trusted.splashscreens.SplashScreenStrategy;
import android.util.Log;

/**
 * Encapsulates the steps necessary to launch a Trusted Web Activity, such as establishing a
 * connection with {@link android.support.customtabs.CustomTabsService}.
 */
public class TwaLauncher {
    private static final String TAG = "TwaLauncher";

    private static final int DEFAULT_SESSION_ID = 96375;

    private final Context mContext;

    @Nullable
    private final String mProviderPackage;

    @LaunchMode
    private final int mLaunchMode;

    private final int mSessionId;

    @Nullable
    private TwaCustomTabsServiceConnection mServiceConnection;

    @Nullable
    private CustomTabsSession mSession;

    @Nullable
    private Runnable mOnSessionCreatedRunnable;

    private boolean mDestroyed;

    /**
     * Creates an instance that will automatically choose the browser to launch a TWA in.
     * If no browser supports TWA, will launch a usual Custom Tab (see {@link TwaProviderPicker}.
     */
    public TwaLauncher(Context context) {
       this(context, null);
    }

    /**
     * Same as above, but also allows to specify a browser to launch. If specified, it is assumed to
     * support TWAs.
     */
    public TwaLauncher(Context context, @Nullable String providerPackage) {
        this(context, providerPackage, DEFAULT_SESSION_ID);
    }

    /**
     * Same as above, but also accepts a session id. This allows to launch multiple TWAs in the same
     * task.
     */
    public TwaLauncher(Context context, @Nullable String providerPackage, int sessionId) {
        mContext = context;
        mSessionId = sessionId;
        if (providerPackage == null) {
            TwaProviderPicker.Action action =
                    TwaProviderPicker.pickProvider(context.getPackageManager());
            mProviderPackage = action.provider;
            mLaunchMode = action.launchMode;
        } else {
            mProviderPackage = providerPackage;
            mLaunchMode = LaunchMode.TRUSTED_WEB_ACTIVITY;
        }
    }

    /**
     * Opens the specified url in a TWA.
     * When TWA is already running in the current task, the url will be opened in existing TWA,
     * if the same instance TwaLauncher is used. If another instance of TwaLauncher is used,
     * the TWA will be reused only if the session ids match (see constructors).
     *
     * @param url Url to open.
     * @param statusBarColor Status bar color
     * @param splashScreenStrategy {@link SplashScreenStrategy} to use for showing splash screens,
     * null if splash screen not needed.
     * @param completionCallback Callback triggered when the url has been open.
     */
    public void openUrl(Uri url, @ColorInt int statusBarColor,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (mDestroyed) {
            throw new IllegalStateException("TwaLauncher already destroyed");
        }

        if (mLaunchMode == LaunchMode.TRUSTED_WEB_ACTIVITY) {
            launchTwa(url, statusBarColor, splashScreenStrategy, completionCallback);
        } else {
            launchCct(url, statusBarColor, completionCallback);
        }
    }

    private void launchCct(Uri url, @ColorInt int statusBarColor,
            @Nullable Runnable completionCallback) {
        // CustomTabsIntent will fall back to launching the Browser if there are no Custom Tabs
        // providers installed.
        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .setToolbarColor(statusBarColor)
                .build();
        if (mProviderPackage != null) {
            intent.intent.setPackage(mProviderPackage);
        }
        intent.launchUrl(mContext, url);
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    private void launchTwa(Uri url, @ColorInt int statusBarColor,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (splashScreenStrategy != null) {
            splashScreenStrategy.onTwaLaunchInitiated(mProviderPackage, statusBarColor);
        }

        Runnable onSessionCreatedRunnable = () ->
                launchWhenSessionEstablished(url, statusBarColor, splashScreenStrategy,
                        completionCallback);
        if (mSession != null) {
            onSessionCreatedRunnable.run();
            return;
        }

        mOnSessionCreatedRunnable = onSessionCreatedRunnable;
        if (mServiceConnection == null) {
            mServiceConnection = new TwaCustomTabsServiceConnection();
        }
        CustomTabsClient.bindCustomTabsService(mContext, mProviderPackage,
                mServiceConnection);
    }

    private void launchWhenSessionEstablished(Uri url, @ColorInt int statusBarColor,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (mSession == null) {
            throw new IllegalStateException("mSession is null in launchWhenSessionEstablished");
        }

        TrustedWebActivityBuilder builder =
                new TrustedWebActivityBuilder(mContext, mSession, url)
                        .setStatusBarColor(statusBarColor);
        if (splashScreenStrategy != null) {
            splashScreenStrategy.configureTwaBuilder(builder, mSession,
                    () -> launchWhenSplashScreenReady(builder, completionCallback));
        } else {
            launchWhenSplashScreenReady(builder, completionCallback);
        }
    }

    private void launchWhenSplashScreenReady(TrustedWebActivityBuilder builder,
            @Nullable Runnable completionCallback) {
        Log.d(TAG, "Launching Trusted Web Activity.");
        builder.launchActivity();
        // Remember who we connect to as the package that is allowed to delegate notifications
        // to us.
        TrustedWebActivityService.setVerifiedProvider(mContext, mProviderPackage);
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    /**
     * Performs clean-up.
     */
    public void destroy() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mDestroyed = true;
    }

    /**
     * Returns package name of the browser this TwaLauncher is launching.
     */
    @Nullable
    public String getProviderPackage() {
        return mProviderPackage;
    }

    private class TwaCustomTabsServiceConnection extends CustomTabsServiceConnection {
        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            if (TrustedWebUtils.warmupIsRequired(mContext, mProviderPackage)) {
                client.warmup(0);
            }
            mSession = client.newSession(null, mSessionId);
            if (mOnSessionCreatedRunnable != null) {
                mOnSessionCreatedRunnable.run();
                mOnSessionCreatedRunnable = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSession = null;
        }
    }
}
