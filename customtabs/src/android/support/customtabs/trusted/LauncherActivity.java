// Copyright 2018 Google Inc. All Rights Reserved.
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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * A convenience class to make using Trusted Web Activities easier. You can extend this class for
 * basic modifications to the behaviour.
 *
 * If you just want to wrap a website in a Trusted Web Activity you should:
 * 1) Copy the manifest for the svgomg project.
 * 2) Set up Digital Asset Links [1] for your site and app.
 * 3) Set the DEFAULT_URL metadata in the manifest and the browsable intent filter to point to your
 *    website.
 *
 * You can skip (2) if you just want to try out TWAs but not on your own website, but you must
 * add the {@code --disable-digital-asset-link-verification-for-url=https://svgomg.firebaseapp.com}
 * to Chrome for this to work [2].
 *
 * You may also go beyond this and add notification delegation, which causes push notifications to
 * be shown by your app instead of Chrome. This is detailed in the javadoc for
 * {@link TrustedWebActivityService}.
 *
 * If you just want default behaviour your Trusted Web Activity client app doesn't even need any
 * Java code - you just set everything up in the Android Manifest!
 *
 * This activity also supports showing a splash screen while the Trusted Web Activity provider is
 * warming up and is loading the page in Trusted Web Activity. This is supported in Chrome 75+.
 *
 * Splash screens support in Chrome is based on transferring the splash screen via FileProvider [3].
 * To set up splash screens, you need to:
 * 1) Set up a FileProvider in the Manifest as described in [3]. The file provider paths should be
 * as follows: <paths><files-path path="twa_splash/" name="twa_splash"/></paths>
 * 2) Provide splash-screen related metadata (see descriptions in {@link LauncherActivityMetadata}),
 * including the authority of your FileProvider.
 *
 * Splash screen is first shown here in LauncherActivity, then seamlessly moved onto the browser.
 * Showing splash screen in the app first is optional, but highly recommended, because on slow
 * devices (e.g. Android Go) it can take seconds to boot up a browser.
 *
 * Note: despite the opaque splash screen, LauncherActivity should still have a transparent style.
 * That way it can gracefully fall back to being a transparent "trampoline" activity in the
 * following cases:
 * - Splash screens are not supported by the picked browser.
 * - The TWA is already running, and LauncherActivity merely needs to deliver a new Intent to it.
 *
 * [1] https://developers.google.com/digital-asset-links/v1/getting-started
 * [2] https://www.chromium.org/developers/how-tos/run-chromium-with-flags#TOC-Setting-Flags-for-Chrome-on-Android
 * [3] https://developer.android.com/reference/android/support/v4/content/FileProvider
 */
public class LauncherActivity extends AppCompatActivity {
    private static final String TAG = "TWALauncherActivity";

    private static final String BROWSER_WAS_LAUNCHED_KEY =
            "android.support.customtabs.trusted.BROWSER_WAS_LAUNCHED_KEY";

    private static final int SESSION_ID = 96375;

    @Nullable private TwaCustomTabsServiceConnection mServiceConnection;

    @Nullable private SplashImageTransferTask mSplashImageTransferTask;

    private LauncherActivityMetadata mMetadata;

    private boolean mBrowserWasLaunched;

    private String mCustomTabsProviderPackage;

    private boolean mShouldShowSplashScreen;

    /** We only want to show the update prompt once per instance of this application. */
    private static boolean sChromeVersionChecked;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.getBoolean(BROWSER_WAS_LAUNCHED_KEY)) {
            // This activity died in the background after launching Trusted Web Activity, then
            // the user closed the Trusted Web Activity and ended up here.
            finish();
            return;
        }

        mMetadata = LauncherActivityMetadata.parse(this);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(getPackageManager());

        // TODO(peconn): Separate logic for different launch strategies (Browser vs Custom Tab vs
        // TWA) into different classes.
        if (action.launchMode != TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY) {
            // CustomTabsIntent will fall back to launching the Browser if there are no Custom Tabs
            // providers installed.
            CustomTabsIntent intent = new CustomTabsIntent.Builder()
                    .setToolbarColor(getColorCompat(mMetadata.statusBarColorId))
                    .build();

            if (action.provider != null) {
                intent.intent.setPackage(action.provider);
            }

            intent.launchUrl(this, getLaunchingUrl());

            mBrowserWasLaunched = true;
            return;
        }

        mCustomTabsProviderPackage = action.provider;

        if (!sChromeVersionChecked) {
            TrustedWebUtils.promptForChromeUpdateIfNeeded(this, mCustomTabsProviderPackage);
            sChromeVersionChecked = true;
        }

        mShouldShowSplashScreen = shouldShowSplashScreen();

        if (mShouldShowSplashScreen) {
            showSplashScreen();
            customizeStatusAndNavBarDuringSplashScreen();
        }

        mServiceConnection = new TwaCustomTabsServiceConnection();
        CustomTabsClient.bindCustomTabsService(
                this, mCustomTabsProviderPackage, mServiceConnection);
    }

    private boolean shouldShowSplashScreen() {
        // Splash screen was not requested.
        if (mMetadata.splashImageDrawableId == 0) return false;

        // If this activity isn't task root, then a TWA is already running, and we're passing a new
        // intent into it. Don't show splash screen in this case.
        if (!isTaskRoot()) return false;

        return TrustedWebUtils.splashScreensAreSupported(this, mCustomTabsProviderPackage);
    }

    /**
     * Splash screen is shown both before the Trusted Web Activity is launched - in this activity,
     * and for some time after that - in browser, on top of web page being loaded.
     * This method shows the splash screen in the LauncherActivity. Override to customize the
     * splash screen view.
     */
    private void showSplashScreen() {
        ImageView view = new ImageView(this);
        view.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        view.setImageResource(mMetadata.splashImageDrawableId);
        view.setBackgroundColor(getColorCompat(mMetadata.splashScreenBackgroundColorId));
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        setContentView(view);
    }

    /**
     * Sets the colors of status and navigation bar to match the ones seen after the splash screen
     * is transferred to the browser. Override to customize these colors.
     */
    protected void customizeStatusAndNavBarDuringSplashScreen() {
        int statusBarColor = getColorCompat(mMetadata.statusBarColorId);
        StatusAndNavBarUtils.setStatusBarColor(this, statusBarColor);

        // Custom tabs may in future support customizing status bar icon color and nav bar color.
        // For now, we apply the colors Chrome uses.
        if (StatusAndNavBarUtils.shouldUseDarkStatusBarIcons(statusBarColor)) {
            StatusAndNavBarUtils.setDarkStatusBarIcons(this);
        }
        StatusAndNavBarUtils.setWhiteNavigationBar(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mBrowserWasLaunched) {
            finish(); // The user closed the Trusted Web Activity and ended up here.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        if (mSplashImageTransferTask != null) {
            mSplashImageTransferTask.cancel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BROWSER_WAS_LAUNCHED_KEY, mBrowserWasLaunched);
    }

    /**
     * Creates a {@link CustomTabsSession}. Default implementation returns a CustomTabsSession using
     * a constant session id, see {@link CustomTabsClient#newSession(CustomTabsCallback, int)}, so
     * that if an instance of Trusted Web Activity associated with this app is already running, the
     * new Intent will be routed to it, allowing for seamless page transitions. The user will be
     * able to navigate to the previous page with the back button.
     *
     * Override this if you want any special session specific behaviour. To launch separate Trusted
     * Web Activity instances, return CustomTabsSessions either without session ids (see
     * {@link CustomTabsClient#newSession(CustomTabsCallback)}) or with different ones on each
     * call.
     */
    protected CustomTabsSession getSession(CustomTabsClient client) {
        return client.newSession(null, SESSION_ID);
    }

    /**
     * Returns the URL that the Trusted Web Activity should be launched to. By default this
     * implementation checks to see if the Activity was launched with an Intent with data, if so
     * attempt to launch to that URL. If not, read the
     * "android.support.customtabs.trusted.DEFAULT_URL" metadata from the manifest.
     *
     * Override this for special handling (such as ignoring or sanitising data from the Intent).
     */
    protected Uri getLaunchingUrl() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            Log.d(TAG, "Using URL from Intent (" + uri + ").");
            return uri;
        }

        if (mMetadata.defaultUrl != null) {
            Log.d(TAG, "Using URL from Manifest (" + mMetadata.defaultUrl + ").");
            return Uri.parse(mMetadata.defaultUrl);
        }

        return Uri.parse("https://www.example.com/");
    }

    private int getColorCompat(int resourceId) {
        return ContextCompat.getColor(this, resourceId);
    }

    private class TwaCustomTabsServiceConnection extends CustomTabsServiceConnection {
        CustomTabsClient mClient;
        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            mClient = client;
            if (TrustedWebUtils.warmupIsRequired(
                    LauncherActivity.this, mCustomTabsProviderPackage)) {
                client.warmup(0);
            }

            CustomTabsSession session = getSession(client);
            TrustedWebActivityBuilder builder =
                    new TrustedWebActivityBuilder(LauncherActivity.this, session, getLaunchingUrl())
                            .setStatusBarColor(getColorCompat(mMetadata.statusBarColorId));
            if (mShouldShowSplashScreen) {
                launchTwaAfterTransferringSplashImage(builder, session);
            } else {
                launchTwa(builder);
            }
        }

        private void launchTwaAfterTransferringSplashImage(TrustedWebActivityBuilder builder,
                CustomTabsSession session) {
            mSplashImageTransferTask = new SplashImageTransferTask(LauncherActivity.this,
                    mMetadata.splashImageDrawableId, mMetadata.fileProviderAuthority, session,
                    mCustomTabsProviderPackage);

            mSplashImageTransferTask.execute(success -> onSplashImageTransferred(builder, success));
        }

        private void onSplashImageTransferred(TrustedWebActivityBuilder builder, boolean success) {
            if (!success) {
                Log.d(TAG, "Failed to transfer splash image.");
                launchTwa(builder);
                return;
            }
            Bundle splashScreenParams = new Bundle();
            splashScreenParams.putInt(TrustedWebUtils.KEY_SPLASH_SCREEN_BACKGROUND_COLOR,
                    getColorCompat(mMetadata.splashScreenBackgroundColorId));
            builder.setSplashScreenParams(splashScreenParams);
            launchTwa(builder);
            overridePendingTransition(0, 0); // Avoid window animations during transition.
        }

        private void launchTwa(TrustedWebActivityBuilder builder) {
            Log.d(TAG, "Launching Trusted Web Activity.");
            builder.launchActivity();

            // Remember who we connect to as the package that is allowed to delegate notifications
            // to us.
            TrustedWebActivityService.setVerifiedProvider(
                    LauncherActivity.this, mCustomTabsProviderPackage);
            mBrowserWasLaunched = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    }
}
