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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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
 *
 * This class also supports showing a splash screen while Chrome is warming up and is loading the
 * page in Trusted Web Activity (this requires Chrome 74+). To set up splash screens you should:
 *
 * 1) Enable the feature by setting {@link #METADATA_SHOW_SPLASH_SCREEN} to "true" for
 * LauncherActivity in your manifest.
 *
 * 2) Include {@link SplashScreenActivity} in your manifest, and specify a theme for it. The theme
 * should include the drawable for your splash screen and <strong>must</strong> be translucent.
 * For example:
 * <pre>{@code
 *   <style name="Theme.SplashScreenActivity" parent="Theme.AppCompat.NoActionBar">
 *       <item name="android:windowBackground">@drawable/your_splash_screen_drawable</item>
 *       <item name="android:windowIsTranslucent">true</item>
 *   </style>
 * }</pre>
 *
 * Note: despite the windowIsTranslucent flag, the drawable itself doesn't have to be translucent.
 *
 * At the moment Trusted Web Activities only work with Chrome Dev, Beta and local builds (launch
 * progress [3]).
 *
 * [1] https://developers.google.com/digital-asset-links/v1/getting-started
 * [2] https://www.chromium.org/developers/how-tos/run-chromium-with-flags#TOC-Setting-Flags-for-Chrome-on-Android
 * [3] https://www.chromestatus.com/feature/4857483210260480
 */
public class LauncherActivity extends android.app.Activity {
    /** Action used for communication with {@link SplashScreenActivity}. */
    public static final String CLOSE_ACTION = "android.support.customtabs.trusted.CLOSE_ACTION";

    private static final String TAG = "LauncherActivity";
    private static final String METADATA_DEFAULT_URL =
            "android.support.customtabs.trusted.DEFAULT_URL";

    private static final String METADATA_SHOW_SPLASH_SCREEN =
            "android.support.customtabs.trusted.SHOW_SPLASH_SCREEN";

    private static final String TWA_WAS_LAUNCHED_KEY =
            "android.support.customtabs.trusted.TWA_WAS_LAUNCHED_KEY";

    private static final int SESSION_ID = 96375;

    @Nullable private TwaCustomTabsServiceConnection mServiceConnection;

    @Nullable private String mDefaultUrl;
    private boolean mShowSplashScreen;

    private boolean mTwaWasLaunched;

    /** We only want to show the update prompt once per instance of this application. */
    private static boolean sChromeVersionChecked;

    /**
     * Connects to the CustomTabsService.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.e("ABCD", "LauncherActivity#onCreate");
        String chromePackage = "com.google.android.apps.chrome";

        if (chromePackage == null) {
            TrustedWebUtils.showNoPackageToast(this);
            finish();
            return;
        }

        if (!sChromeVersionChecked) {
            TrustedWebUtils.promptForChromeUpdateIfNeeded(this, chromePackage);
            sChromeVersionChecked = true;
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean(TWA_WAS_LAUNCHED_KEY)) {
            // This activity died in the background after launching Trusted Web Activity, then
            // the user closed the Trusted Web Activity and ended up here.
            finish();
            return;
        }

        parseMetadata();

        android.util.Log.e("ABCD", "bindCustomTabsService");
        mServiceConnection = new TwaCustomTabsServiceConnection();
        CustomTabsClient.bindCustomTabsService(this, chromePackage, mServiceConnection);
    }


    private void parseMetadata() {
        android.widget.FrameLayout fl = new android.widget.FrameLayout(this);
        fl.setBackgroundColor(android.graphics.Color.BLUE);
        setContentView(fl);
        android.view.LayoutInflater.from(this).inflate(android.support.customtabs2.R.layout.webapp_splash_screen_no_icon, fl, true);

        try {
            Bundle metaData = getPackageManager().getActivityInfo(
                    new ComponentName(this, getClass()), PackageManager.GET_META_DATA).metaData;
            if (metaData == null) {
                return;
            }
            mDefaultUrl = metaData.getString(METADATA_DEFAULT_URL);
            mShowSplashScreen = metaData.getBoolean(METADATA_SHOW_SPLASH_SCREEN);
        } catch (PackageManager.NameNotFoundException e) {
            // Will only happen if the package provided (the one we are running in) is not
            // installed - so should never happen.
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mTwaWasLaunched) {
            //android.util.Log.e("ABCD", "LauncherActivity#finish0");
            //finish(); // The user closed the Trusted Web Activity and ended up here.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (CLOSE_ACTION.equals(intent.getAction())) {
            android.util.Log.e("ABCD", "LauncherActivity#onNewIntent() finish");
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TWA_WAS_LAUNCHED_KEY, mTwaWasLaunched);
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
     * Creates a {@link CustomTabsIntent} to launch the Trusted Web Activity.
     * By default, Trusted Web Activity will be launched in the same Android Task.
     * Override this if you want any special launching behaviour.
     */
    protected CustomTabsIntent getCustomTabsIntent(CustomTabsSession session) {
        android.util.Log.e("ABCD", "Content " + findViewById(android.R.id.content));
        android.os.Bundle b = android.support.v4.app.ActivityOptionsCompat
                                      .makeSceneTransitionAnimation(this,
                                              findViewById(android.R.id.content), "profile")
                                      .toBundle();
        return new CustomTabsIntent.Builder(session).setSharedElementTransition(b).build();
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
        if (mDefaultUrl != null) {
            Log.d(TAG, "Using URL from Manifest (" + mDefaultUrl + ").");
            return Uri.parse(mDefaultUrl);
        }
        return Uri.parse("https://www.example.com/");
    }

    private class TwaCustomTabsServiceConnection extends CustomTabsServiceConnection {
        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            mClient = client;
            if (mAttachedToWindow) {
                launchUrl();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    }

    @Override
    public void onAttachedToWindow() {
        new android.os.Handler().post(new Runnable() {
            @Override
            public void run() {
                if (mClient != null) {
                    launchUrl();
                }
                mAttachedToWindow = true;
            }
        });
    }

    private void launchUrl() {
        android.util.Log.e("ABCD", "onServiceConnected");
        CustomTabsSession session = getSession(mClient);
        CustomTabsIntent intent = getCustomTabsIntent(session);
        Uri url = getLaunchingUrl();

        LauncherActivity.this.setExitSharedElementCallback(new android.app.SharedElementCallback() {
            @Override
            public android.os.Parcelable onCaptureSharedElementSnapshot(
                    android.view.View sharedElement, android.graphics.Matrix viewToGlobalMatrix,
                    android.graphics.RectF screenBounds) {
                android.util.Log.e("ABCD", "captureSnapshot");
                android.os.Parcelable p = super.onCaptureSharedElementSnapshot(
                        sharedElement, viewToGlobalMatrix, screenBounds);
                return p;
            }
        });

        Log.d(TAG, "Launching Trusted Web Activity.");

        android.transition.Fade fade = new android.transition.Fade();
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);
        getWindow().setExitTransition(fade);

        TrustedWebUtils.launchAsTrustedWebActivity(LauncherActivity.this, intent, url);
        // overridePendingTransition(0, 0);
        mTwaWasLaunched = true;

    }

    private CustomTabsClient mClient;
    private boolean mAttachedToWindow;
    private static boolean s0;
}
