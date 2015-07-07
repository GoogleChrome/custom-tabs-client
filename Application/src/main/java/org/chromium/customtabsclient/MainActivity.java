// Copyright 2015 Google Inc. All Rights Reserved.
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

package org.chromium.customtabsclient;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Example client activity for a Chrome Custom Tab.
 */
public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "CustomTabsClientExample";
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mEditText = (EditText) findViewById(R.id.edit);
        Button warmupButton = (Button) findViewById(R.id.warmup_button);
        Button mayLaunchButton = (Button) findViewById(R.id.may_launch_button);
        Button button = (Button) findViewById(R.id.button);
        mEditText.requestFocus();
        warmupButton.setOnClickListener(this);
        mayLaunchButton.setOnClickListener(this);
        button.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        CustomTabActivityManager customTabManager = CustomTabActivityManager.getInstance(this);
        customTabManager.unbindService();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        CustomTabActivityManager customTabManager = CustomTabActivityManager.getInstance(this);
        String url = mEditText.getText().toString();
        int viewId = v.getId();

        customTabManager.setNavigationCallback(new CustomTabActivityManager.NavigationCallback() {
            @Override
            public void onUserNavigationStarted(String url, Bundle extras) {
                Log.w(TAG, "onUserNavigationStarted: url = " + url);
            }

            @Override
            public void onUserNavigationFinished(String url, Bundle extras) {
                Log.w(TAG, "onUserNavigationFinished: url = " + url);
            }
        });

        if (viewId == R.id.warmup_button) {
            customTabManager.bindService();
            customTabManager.warmup();
        } else if (viewId == R.id.may_launch_button) {
            customTabManager.mayLaunchUrl(url, null);
        } else if (viewId == R.id.button) {
            CustomTabUiBuilder uiBuilder = new CustomTabUiBuilder();
            uiBuilder.setToolbarColor(Color.BLUE);
            uiBuilder.setShowTitle(true);
            prepareMenuItems(uiBuilder);
            prepareActionButton(uiBuilder);
            uiBuilder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
            uiBuilder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);
            customTabManager.loadUrl(url, uiBuilder);
        }
    }

    private void prepareMenuItems(CustomTabUiBuilder uiBuilder) {
        Intent menuIntent = new Intent();
        menuIntent.setClass(getApplicationContext(), this.getClass());
        // Optional animation configuration when the user clicks menu items.
        Bundle menuBundle = ActivityOptions.makeCustomAnimation(this, android.R.anim.slide_in_left,
                android.R.anim.slide_out_right).toBundle();
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, menuIntent, 0,
                menuBundle);
        uiBuilder.addMenuItem("Menu entry 1", pi);
    }

    private void prepareActionButton(CustomTabUiBuilder uiBuilder) {
        // An example intent that sends an email.
        Intent actionIntent = new Intent(Intent.ACTION_SEND);
        actionIntent.setType("*/*");
        actionIntent.putExtra(Intent.EXTRA_EMAIL, "example@example.com");
        actionIntent.putExtra(Intent.EXTRA_SUBJECT, "example");
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, actionIntent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        uiBuilder.setActionButton(icon, pi);
    }
}
