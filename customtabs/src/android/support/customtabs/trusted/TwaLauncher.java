// Copyright 2019 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package android.support.customtabs.trusted;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.customtabs.trusted.TwaProviderPicker.LaunchMode;
import android.support.customtabs.trusted.splashscreens.SplashScreenStrategy;
import android.support.v4.content.ContextCompat;
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
     */
    public void launch(Uri url) {
        launch(new TrustedWebActivityIntentBuilder(url), null, null);
    }

    /**
     * Similar to {@link #launch(Uri)}, but allows more customization.
     *
     * @param twaBuilder {@link TrustedWebActivityIntentBuilder} containing the url to open, along with
     * optional parameters: status bar color, additional trusted origins, etc.
     * @param splashScreenStrategy {@link SplashScreenStrategy} to use for showing splash screens,
     * null if splash screen not needed.
     * @param completionCallback Callback triggered when the url has been opened.
     */
    public void launch(TrustedWebActivityIntentBuilder twaBuilder,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (mDestroyed) {
            throw new IllegalStateException("TwaLauncher already destroyed");
        }

        if (mLaunchMode == LaunchMode.TRUSTED_WEB_ACTIVITY) {
            launchTwa(twaBuilder, splashScreenStrategy, completionCallback);
        } else {
            launchCct(twaBuilder, completionCallback);
        }
    }

    private void launchCct(TrustedWebActivityIntentBuilder twaBuilder,
            @Nullable Runnable completionCallback) {
        // CustomTabsIntent will fall back to launching the Browser if there are no Custom Tabs
        // providers installed.
        CustomTabsIntent intent = twaBuilder.buildCustomTabsIntent();
        if (mProviderPackage != null) {
            intent.intent.setPackage(mProviderPackage);
        }
        intent.launchUrl(mContext, twaBuilder.getUrl());
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    private void launchTwa(TrustedWebActivityIntentBuilder twaBuilder,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (splashScreenStrategy != null) {
            splashScreenStrategy.onTwaLaunchInitiated(mProviderPackage, twaBuilder);
        }

        Runnable onSessionCreatedRunnable = () ->
                launchWhenSessionEstablished(twaBuilder, splashScreenStrategy, completionCallback);

        if (mSession != null) {
            onSessionCreatedRunnable.run();
            return;
        }

        Runnable onSessionCreationFailedRunnable = () -> {
            // The provider has been unable to create a session for us, we can't launch a
            // Trusted Web Activity. We could either exit, forcing the user to try again,
            // hopefully successfully this time or we could launch a Custom Tab giving the user
            // a subpar experience (compared to a TWA).
            // We'll open in a CCT, but pay attention to what users want.
            launchCct(twaBuilder, completionCallback);
        };

        if (mServiceConnection == null) {
            mServiceConnection = new TwaCustomTabsServiceConnection();
        }

        mServiceConnection.setSessionCreationRunnables(
                onSessionCreatedRunnable, onSessionCreationFailedRunnable);
        CustomTabsClient.bindCustomTabsService(mContext, mProviderPackage, mServiceConnection);
    }

    private void launchWhenSessionEstablished(TrustedWebActivityIntentBuilder twaBuilder,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (mSession == null) {
            throw new IllegalStateException("mSession is null in launchWhenSessionEstablished");
        }

        if (splashScreenStrategy != null) {
            splashScreenStrategy.configureTwaBuilder(twaBuilder, mSession,
                    () -> launchWhenSplashScreenReady(twaBuilder, completionCallback));
        } else {
            launchWhenSplashScreenReady(twaBuilder, completionCallback);
        }
    }

    private void launchWhenSplashScreenReady(TrustedWebActivityIntentBuilder builder,
            @Nullable Runnable completionCallback) {
        Log.d(TAG, "Launching Trusted Web Activity.");
        Intent intent = builder.build(mSession);
        ContextCompat.startActivity(mContext, intent, null);
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
        private Runnable mOnSessionCreatedRunnable;
        private Runnable mOnSessionCreationFailedRunnable;

        private void setSessionCreationRunnables(@Nullable Runnable onSuccess,
                @Nullable Runnable onFailure) {
            mOnSessionCreatedRunnable = onSuccess;
            mOnSessionCreationFailedRunnable = onFailure;
        }

        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            if (TrustedWebUtils.warmupIsRequired(mContext, mProviderPackage)) {
                client.warmup(0);
            }
            mSession = client.newSession(null, mSessionId);

            if (mSession != null && mOnSessionCreatedRunnable != null) {
                mOnSessionCreatedRunnable.run();
            } else if (mSession == null && mOnSessionCreationFailedRunnable != null) {
                mOnSessionCreationFailedRunnable.run();
            }

            mOnSessionCreatedRunnable = null;
            mOnSessionCreationFailedRunnable = null;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSession = null;
        }
    }
}
