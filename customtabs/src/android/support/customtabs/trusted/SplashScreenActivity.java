// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.support.customtabs.trusted;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity for showing splash screens for Trusted Web Activities.
 * See details about how to use in {@link LauncherActivity}.
 */
public class SplashScreenActivity extends AppCompatActivity {

    public static final String EXTRA_IS_TOP_SPLASH = "EXTRA_IS_TOP_SPLASH";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LauncherActivity.CLOSE_ACTION.equals(getIntent().getAction())) {
            finish();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(LauncherActivity.LOCAL_BROADCAST_REMOVE_SPLASH_SCREEN));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getBooleanExtra(EXTRA_IS_TOP_SPLASH, false)) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(
                    LauncherActivity.LOCAL_BROADCAST_TOP_SPLASH_SCREEN_SHOWN));
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.setAction(LauncherActivity.CLOSE_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
            overridePendingTransition(0, 0);
        }
    };

}
