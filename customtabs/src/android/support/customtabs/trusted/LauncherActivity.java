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
import android.content.pm.PackageManager;
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
 * At the moment this only works with Chrome Dev, Beta and local builds (launch progress [3]).
 *
 * [1] https://developers.google.com/digital-asset-links/v1/getting-started
 * [2] https://www.chromium.org/developers/how-tos/run-chromium-with-flags#TOC-Setting-Flags-for-Chrome-on-Android
 * [3] https://www.chromestatus.com/feature/4857483210260480
 */
public class LauncherActivity extends AppCompatActivity {
    private static final String TAG = "TWALauncherActivity";
    private static final String METADATA_DEFAULT_URL =
            "android.support.customtabs.trusted.DEFAULT_URL";
    private static final String METADATA_STATUS_BAR_COLOR =
            "android.support.customtabs.trusted.STATUS_BAR_COLOR";

    private static final String BROWSER_WAS_LAUNCHED_KEY =
            "android.support.customtabs.trusted.BROWSER_WAS_LAUNCHED_KEY";

    private static final int SESSION_ID = 96375;

    @Nullable private TwaCustomTabsServiceConnection mServiceConnection;

    @Nullable private String mDefaultUrl;

    private int mStatusBarColor;

    private boolean mBrowserWasLaunched;

    private String mCustomTabsProviderPackage;

    /** We only want to show the update prompt once per instance of this application. */
    private static boolean sChromeVersionChecked;

    /**
     * Connects to the CustomTabsService.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.getBoolean(BROWSER_WAS_LAUNCHED_KEY)) {
            // This activity died in the background after launching Trusted Web Activity, then
            // the user closed the Trusted Web Activity and ended up here.
            finish();
            return;
        }

        parseMetadata();

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(getPackageManager());

        // TODO(peconn): Separate logic for different launch strategies (Browser vs Custom Tab vs
        // TWA) into different classes.
        if (action.launchMode != TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY) {
            // CustomTabsIntent will fall back to launching the Browser if there are no Custom Tabs
            // providers installed.
            CustomTabsIntent intent = new CustomTabsIntent.Builder()
                    .setToolbarColor(mStatusBarColor)
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

        mServiceConnection = new TwaCustomTabsServiceConnection();
        CustomTabsClient.bindCustomTabsService(
                this, mCustomTabsProviderPackage, mServiceConnection);
    }

    private void parseMetadata() {
        try {
            Bundle metaData = getPackageManager().getActivityInfo(
                    new ComponentName(this, getClass()), PackageManager.GET_META_DATA).metaData;
            if (metaData == null) {
                return;
            }
            mDefaultUrl = metaData.getString(METADATA_DEFAULT_URL);
            mStatusBarColor = metaData.getInt(METADATA_STATUS_BAR_COLOR, android.R.color.white);
        } catch (PackageManager.NameNotFoundException e) {
            // Will only happen if the package provided (the one we are running in) is not
            // installed - so should never happen.
        }
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
     * Creates a {@link CustomTabsIntent} to launch the Trusted Web Activity.
     * By default, Trusted Web Activity will be launched in the same Android Task.
     * Override this if you want any special launching behaviour.
     */
    protected CustomTabsIntent getCustomTabsIntent(CustomTabsSession session) {
        return new CustomTabsIntent.Builder(session)
                .setToolbarColor(mStatusBarColor)
                .build();
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
            if (TrustedWebUtils.warmupIsRequired(
                    LauncherActivity.this, mCustomTabsProviderPackage)) {
                client.warmup(0);
            }

            CustomTabsSession session = getSession(client);
            CustomTabsIntent intent = getCustomTabsIntent(session);
            Uri url = getLaunchingUrl();

            Log.d(TAG, "Launching Trusted Web Activity.");
            TrustedWebUtils.launchAsTrustedWebActivity(LauncherActivity.this, intent, url);

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
