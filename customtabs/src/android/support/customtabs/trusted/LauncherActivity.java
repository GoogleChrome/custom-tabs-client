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
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

/**
 * A convenience class to make using Trusted Web Activities easier. You can extend this class for
 * basic modifications to the behaviour.
 *
 * If you just want to wrap a website in a Trusted Web Activity you should:
 * 1) Copy the manifest for this (the svgomg) project.
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
    private static final String TAG = "LauncherActivity";
    private static final String DEFAULT_URL_METADATA =
            "android.support.customtabs.trusted.DEFAULT_URL";
    private static final List<String> CHROME_PACKAGES = Arrays.asList(
            "com.google.android.apps.chrome",  // Chrome local build.
            "org.chromium.chrome",  // Chromium local build.
            "com.chrome.canary",  // Chrome Canary.
            "com.chrome.dev");  // Chrome Dev.

    /**
     * Connects to the CustomTabsService.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String chromePackage = CustomTabsClient.getPackageName(this, CHROME_PACKAGES, false);
        if (chromePackage == null) {
            Log.d(TAG, "No valid build of Chrome found, exiting.");
            Toast.makeText(this, "Please install Chrome Dev/Canary.", Toast.LENGTH_LONG).show();
            finishCompat();
            return;
        }

        CustomTabsClient.bindCustomTabsService(this, chromePackage, mServiceConnection);
    }

    /**
     * Creates a {@link CustomTabsSession} that cleans up the current activity once navigation has
     * occurred on the Trusted Web Activity. Override this if you want any special session specific
     * behaviour.
     */
    protected CustomTabsSession getSession(CustomTabsClient client) {
        return client.newSession(new CustomTabsCallback() {
            private boolean mCalledFinish;
            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
                if (mCalledFinish) return;

                Log.d(TAG, "Trusted Web Activity launched successfully, closing LauncherActivity.");
                finishCompat();
                mCalledFinish = true;
            }
        });
    }

    /**
     * Creates a {@link CustomTabsIntent} to launch the Trusted Web Activity, adding flags to put
     * the Trusted Web Activity on a separate Android Task. Override this if you want any special
     * launching behaviour.
     */
    protected CustomTabsIntent getCustomTabsIntent(CustomTabsSession session) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(session).build();

        // When opening a TWA, there are items on the Recents screen.
        // Workaround seems to be using the Intent.FLAG_ACTIVITY_NEW_DOCUMENT to create a new
        // document on Recents.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }

        return customTabsIntent;
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

        try {
            ActivityInfo info = getPackageManager().getActivityInfo(
                    new ComponentName(this, getClass()), PackageManager.GET_META_DATA);

            if (info.metaData != null && info.metaData.containsKey(DEFAULT_URL_METADATA)) {
                uri = Uri.parse(info.metaData.getString(DEFAULT_URL_METADATA));
                Log.d(TAG, "Using URL from Manifest (" + uri + ").");
                return uri;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Will only happen if the package provided (the one we are running in) is not
            // installed - so should never happen.
        }

        return Uri.parse("https://www.example.com/");
    }

    final private CustomTabsServiceConnection mServiceConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            // Warmup must be called for Trusted Web Activity verification to work.
            client.warmup(0L);

            CustomTabsSession session = getSession(client);
            CustomTabsIntent intent = getCustomTabsIntent(session);
            Uri url = getLaunchingUrl();

            Log.d(TAG, "Launching Trusted Web Activity.");
            TrustedWebUtils.launchAsTrustedWebActivity(LauncherActivity.this, session, intent, url);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void finishCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
