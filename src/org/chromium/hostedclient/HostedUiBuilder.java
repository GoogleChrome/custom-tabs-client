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

package org.chromium.hostedclient;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Helper class to build the custom Hosted Activity UI.
 */
public class HostedUiBuilder {
    private static final String HOSTED_TRIGGER_FLAG = "com.android.chrome.append_task";
    private static final String EXTRA_HOSTED_EXIT_ANIMATION_BUNDLE = "hosted:exit_animation_bundle";
    private static final String EXTRA_HOSTED_TOOLBAR_COLOR = "hosted:toolbar_color";
    private static final String EXTRA_HOSTED_MENU_ITEMS = "hosted:menu_items";
    private static final String EXTRA_HOSTED_ACTION_BUTTON_BUNDLE = "hosted:action_button_bundle";
    private static final String KEY_HOSTED_ICON = "hosted:icon";
    private static final String KEY_HOSTED_MENU_TITLE = "hosted:menu_title";
    private static final String KEY_HOSTED_PENDING_INTENT = "hosted:pending_intent";

    private final Intent mIntent;
    private Bundle mStartBundle;
    private final ArrayList<Bundle> mMenuItems;

    public HostedUiBuilder() {
        mIntent = new Intent();
        mStartBundle = null;
        mMenuItems = new ArrayList<Bundle>();
        mIntent.putExtra(HOSTED_TRIGGER_FLAG, true);
        mIntent.setPackage(HostedActivityManager.CHROME_PACKAGE);
        mIntent.setAction(Intent.ACTION_VIEW);
    }

    /**
     * Sets the toolbar color.
     *
     * @param color The color.
     */
    public HostedUiBuilder setToolbarColor(int color) {
        mIntent.putExtra(EXTRA_HOSTED_TOOLBAR_COLOR, color);
        return this;
    }

    /**
     * Adds a menu item.
     *
     * @param label Menu label.
     * @param pendingIntent Pending intent delivered when the menu item is clicked.
     */
    public HostedUiBuilder addMenuItem(String label, PendingIntent pendingIntent) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_HOSTED_MENU_TITLE, label);
        bundle.putParcelable(KEY_HOSTED_PENDING_INTENT, pendingIntent);
        mMenuItems.add(bundle);
        return this;
    }

    /**
     * Set the action button.
     *
     * @param bitmap The icon.
     * @param pendingIntent pending intent delivered when the button is clicked.
     */
    public HostedUiBuilder setActionButton(Bitmap bitmap, PendingIntent pendingIntent) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_HOSTED_ICON, bitmap);
        bundle.putParcelable(KEY_HOSTED_PENDING_INTENT, pendingIntent);
        mIntent.putExtra(EXTRA_HOSTED_ACTION_BUTTON_BUNDLE, bundle);
        return this;
    }

    /**
     * Sets the start animations,
     *
     * @param context Application context.
     * @param enterResId Resource ID of the "enter" animation for the browser.
     * @param exitResId Resource ID of the "exit" animation for the application.
     */
    public HostedUiBuilder setStartAnimations(Context context, int enterResId, int exitResId) {
        mStartBundle =
                ActivityOptions.makeCustomAnimation(context, enterResId, exitResId).toBundle();
        return this;
    }

    /**
     * Sets the exit animations,
     *
     * @param context Application context.
     * @param enterResId Resource ID of the "enter" animation for the application.
     * @param exitResId Resource ID of the "exit" animation for the browser.
     */
    public HostedUiBuilder setExitAnimations(Context context, int enterResId, int exitResId) {
        Bundle bundle =
                ActivityOptions.makeCustomAnimation(context, enterResId, exitResId).toBundle();
        mIntent.putExtra(EXTRA_HOSTED_EXIT_ANIMATION_BUNDLE, bundle);
        return this;
    }

    Intent getIntent() {
        mIntent.putParcelableArrayListExtra(EXTRA_HOSTED_MENU_ITEMS, mMenuItems);
        return mIntent;
    }

    Bundle getStartBundle() {
        return mStartBundle;
    }
}
