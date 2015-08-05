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
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.chromium.customtabsclient.shared.CustomTabsHelper;

/**
 * Example client activity for a Chrome Custom Tanb.
 */
public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "CustomTabsClientExample";
    private EditText mEditText;
    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mClient;
    private CustomTabsServiceConnection mConnection;
    private String mPackageNameToBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mEditText = (EditText) findViewById(R.id.edit);
        Button connectButton = (Button) findViewById(R.id.connect_button);
        Button warmupButton = (Button) findViewById(R.id.warmup_button);
        Button mayLaunchButton = (Button) findViewById(R.id.may_launch_button);
        Button launchButton = (Button) findViewById(R.id.launch_button);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        mEditText.requestFocus();
        connectButton.setOnClickListener(this);
        warmupButton.setOnClickListener(this);
        mayLaunchButton.setOnClickListener(this);
        launchButton.setOnClickListener(this);

        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                CustomTabsHelper.getPackages()));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (TextUtils.isEmpty(parent.getItemAtPosition(position).toString())) {
                    onNothingSelected(parent);
                }
                mPackageNameToBind = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mPackageNameToBind = null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        unbindCustomTabsService();
        super.onDestroy();
    }

    private CustomTabsSession getSession() {
        if (mClient == null) {
            mCustomTabsSession = null;
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient.newSession(new CustomTabsCallback() {
                @Override
                public void onNavigationEvent(int navigationEvent, Bundle extras) {
                    Log.w(TAG, "onNavigationEvent: Code = " + navigationEvent);
                }
            });
        }
        return mCustomTabsSession;
    }

    private void bindCustomTabsService() {
        if (mClient != null) return;
        if (TextUtils.isEmpty(mPackageNameToBind)) {
            mPackageNameToBind = CustomTabsHelper.getPackageNameToUse(this);
            if (mPackageNameToBind == null) return;
        }
        final View connectButton = findViewById(R.id.connect_button);
        final View warmupButton = findViewById(R.id.warmup_button);
        final View mayLaunchButton = findViewById(R.id.may_launch_button);
        final View launchButton = findViewById(R.id.launch_button);
        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                connectButton.setEnabled(false);
                warmupButton.setEnabled(true);
                mayLaunchButton.setEnabled(true);
                launchButton.setEnabled(true);
                mClient = client;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                connectButton.setEnabled(true);
                warmupButton.setEnabled(false);
                mayLaunchButton.setEnabled(false);
                launchButton.setEnabled(false);
                mClient = null;
            }
        };
        boolean ok = CustomTabsClient.bindCustomTabsService(this, mPackageNameToBind, mConnection);
        if (ok) {
            connectButton.setEnabled(false);
        } else {
            mConnection = null;
        }
    }

    private void unbindCustomTabsService() {
        if (mConnection == null) return;
        unbindService(mConnection);
        mClient = null;
        mCustomTabsSession = null;
    }

    @Override
    public void onClick(View v) {
        String url = mEditText.getText().toString();
        int viewId = v.getId();

        if (viewId == R.id.connect_button) {
            bindCustomTabsService();
        } else if (viewId == R.id.warmup_button) {
            boolean success = false;
            if (mClient != null) success = mClient.warmup(0);
            if (!success) findViewById(R.id.warmup_button).setEnabled(false);
        } else if (viewId == R.id.may_launch_button) {
            CustomTabsSession session = getSession();
            boolean success = false;
            if (mClient != null) success = session.mayLaunchUrl(Uri.parse(url), null, null);
            if (!success) findViewById(R.id.may_launch_button).setEnabled(false);
        } else if (viewId == R.id.launch_button) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(getSession());
            builder.setToolbarColor(Color.BLUE).setShowTitle(true);
            prepareMenuItems(builder);
            prepareActionButton(builder);
            builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
            builder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);
            builder.setCloseButtonIcon(
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back));
            CustomTabsIntent customTabsIntent = builder.build();
            CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent);
            customTabsIntent.launchUrl(this, Uri.parse(url));
        }
    }

    private void prepareMenuItems(CustomTabsIntent.Builder builder) {
        Intent menuIntent = new Intent();
        menuIntent.setClass(getApplicationContext(), this.getClass());
        // Optional animation configuration when the user clicks menu items.
        Bundle menuBundle = ActivityOptions.makeCustomAnimation(this, android.R.anim.slide_in_left,
                android.R.anim.slide_out_right).toBundle();
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, menuIntent, 0,
                menuBundle);
        builder.addMenuItem("Menu entry 1", pi);
    }

    private void prepareActionButton(CustomTabsIntent.Builder builder) {
        // An example intent that sends an email.
        Intent actionIntent = new Intent(Intent.ACTION_SEND);
        actionIntent.setType("*/*");
        actionIntent.putExtra(Intent.EXTRA_EMAIL, "example@example.com");
        actionIntent.putExtra(Intent.EXTRA_SUBJECT, "example");
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, actionIntent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        builder.setActionButton(icon, "send email", pi);
    }
}
