/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.customtabs.browseractions;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

/**
 * A wrapper class holding custom item of Browser Actions menu.
 * The Bitmap is optional for a BrowserActionItem.
 */
public class BrowserActionItem {
    private final String mTitle;
    private final PendingIntent mAction;
    private Bitmap mIcon;

    /**
     * Constructor for BrowserActionItem with icon, string and action provided.
     * @param icon The icon shown for a custom item.
     * @param title The string shown for a custom item.
     * @param action The PendingIntent executed when a custom item is selected
     */
    public BrowserActionItem(@NonNull String title, @NonNull PendingIntent action, Bitmap icon) {
        mTitle = title;
        mAction = action;
        mIcon = icon;
    }

    /**
     * Constructor for BrowserActionItem with only string and action provided.
     * @param title The icon shown for a custom item.
     * @param action The string shown for a custom item.
     */
    public BrowserActionItem(@NonNull String title, @NonNull PendingIntent action) {
        this(title, action, null);
    }

    /**
     * Sets the icon of a custom item.
     * @param icon The icon for a custom item.
     */
    public void setIcon(Bitmap icon) {
        mIcon = icon;
    }

    /**
     * @return the icon of of a custom item.
     */
    public Bitmap getIcon() {
        return mIcon;
    }

    /**
     * @return the title of a custom item.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * @return the action of a custom item.
     */
    public PendingIntent getAction() {
        return mAction;
    }
}
