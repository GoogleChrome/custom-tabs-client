// Copyright 2017 Google Inc. All Rights Reserved.
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

package org.chromium.twa.svgomg;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.v7.app.AppCompatActivity;

public class TwaLauncherActivity extends AppCompatActivity
        implements CustomTabActivityHelper.ConnectionCallback {

    private static final String TARGET_URL = "https://svgomg.firebaseapp.com";
    private static final String CHROME_PACKAGE = "com.chrome.canary";

    private CustomTabActivityHelper mCustomTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twa_launcher);

        mCustomTabActivityHelper = new CustomTabActivityHelper();
        mCustomTabActivityHelper.setConnectionCallback(this);
        mCustomTabActivityHelper.bindCustomTabsService(this, CHROME_PACKAGE);
    }

    public void openTwa() {
        CustomTabsSession session = mCustomTabActivityHelper.getSession();
        Uri uri = Uri.parse(TARGET_URL);

        // Validates assetlinks.
        session.validateRelationship(CustomTabsService.RELATION_HANDLE_ALL_URLS, uri, null);

        // Set an empty transition from TwaLauncherActivity to the TWA splash screen.
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(session)
                .setStartAnimations(this, 0, 0)
                .build();

        customTabsIntent.intent.setPackage(CHROME_PACKAGE);

        // When opening a TWA, there are items on the Recents screen.
        // Workaround seems to be using the Intent.FLAG_ACTIVITY_NEW_DOCUMENT to create a new
        // document on Recents.
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);


        TrustedWebUtils.launchAsTrustedWebActivity(this, customTabsIntent, uri);

        // This should be called when the navigation callback for when the TWA is open
        // is received. The callbacks don't seem to be working at the moment, so using this
        // to improve the transition
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mCustomTabActivityHelper.unbindCustomTabsService(TwaLauncherActivity.this);
                // Call finishAndRemoveTask to keep a single documento on Recents.
                finishAndRemoveTask();
            }
        }, 1000L);
    }

    @Override
    public void onCustomTabsConnected() {
        openTwa();
    }

    @Override
    public void onCustomTabsDisconnected() {

    }
}
