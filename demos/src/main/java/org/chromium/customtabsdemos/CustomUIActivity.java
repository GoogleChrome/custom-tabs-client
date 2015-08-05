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
package org.chromium.customtabsdemos;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * Opens Chrome Custom Tabs with a customized UI.
 */
public class CustomUIActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CustChromeTabActivity";

    private EditText mUrlEditText;
    private EditText mCustomTabColorEditText;
    private CheckBox mShowActionButtonCheckbox;
    private CheckBox mAddMenusCheckbox;
    private CheckBox mShowTitleCheckBox;
    private CheckBox mCustomBackButtonCheckBox;
    private CustomTabActivityHelper mCustomTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_ui);

        mCustomTabActivityHelper = new CustomTabActivityHelper();
        findViewById(R.id.start_custom_tab).setOnClickListener(this);

        mUrlEditText = (EditText) findViewById(R.id.url);
        mCustomTabColorEditText = (EditText) findViewById(R.id.custom_toolbar_color);
        mShowActionButtonCheckbox = (CheckBox) findViewById(R.id.custom_show_action_button);
        mAddMenusCheckbox = (CheckBox) findViewById(R.id.custom_add_menus);
        mShowTitleCheckBox = (CheckBox) findViewById(R.id.show_title);
        mCustomBackButtonCheckBox = (CheckBox) findViewById(R.id.custom_back_button);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCustomTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCustomTabActivityHelper.unbindCustomTabsService(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.start_custom_tab:
                openCustomTab();
                break;
            default:
                //Unknown View Clicked
        }
    }

    private void openCustomTab() {
        String url = mUrlEditText.getText().toString();

        int color = Color.BLUE;
        try {
            color = Color.parseColor(mCustomTabColorEditText.getText().toString());
        } catch (NumberFormatException ex) {
            Log.i(TAG, "Unable to parse Color: " + mCustomTabColorEditText.getText());
        }

        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
        intentBuilder.setToolbarColor(color);

        if (mShowActionButtonCheckbox.isChecked()) {
            //Generally you do not want to decode bitmaps in the UI thread.
            String shareLabel = getString(R.string.label_action_share);
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    android.R.drawable.ic_menu_share);
            PendingIntent pendingIntent = createPendingIntent();
            intentBuilder.setActionButton(icon, shareLabel, pendingIntent);
        }

        if (mAddMenusCheckbox.isChecked()) {
            String menuItemTitle = getString(R.string.menu_item_title);
            PendingIntent menuItemPendingIntent = createPendingIntent();
            intentBuilder.addMenuItem(menuItemTitle, menuItemPendingIntent);
        }

        intentBuilder.setShowTitle(mShowTitleCheckBox.isChecked());

        if (mCustomBackButtonCheckBox.isChecked()) {
            intentBuilder.setCloseButtonIcon(
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back));
        }

        intentBuilder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
        intentBuilder.setExitAnimations(this, android.R.anim.slide_in_left,
                android.R.anim.slide_out_right);

        CustomTabActivityHelper.openCustomTab(
                this, intentBuilder.build(), Uri.parse(url), new WebviewFallback());
    }

    private PendingIntent createPendingIntent() {
        Intent actionIntent = new Intent(Intent.ACTION_SEND);
        actionIntent.setType("*/*");
        actionIntent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.sample_email));
        actionIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sample_subject));
        return  PendingIntent.getActivity(getApplicationContext(), 0, actionIntent, 0);
    }
}
