// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package android.support.customtabs.browseractions;

import android.app.PendingIntent;
import android.net.Uri;
import android.support.annotation.DrawableRes;

/**
 * The class to get information of context menu shown on Browser Actions fallback dialog.
 */
public class BrowserActionsFallbackMenuItem {
    private final String mTitle;
    private int mIconId;
    private Uri mIconUri;
    private PendingIntent mPendingIntentAction;
    private Runnable mRunnableAction;

    /**
     * Constructs a fallback menu item from a custom Browser Actions item.
     * @param item The {@link BrowserActionItem} used to construct the menu item.
     */
    public BrowserActionsFallbackMenuItem(BrowserActionItem item) {
        mTitle = item.getTitle();
        mIconId = item.getIconId();
        mIconUri = item.getIconUri();
        mPendingIntentAction = item.getAction();
    }

    /**
     * Constructs a predefined fallback menu item with a Runnable action. The item will have no
     * icon and no custom PendingIntent action.
     * @param title The title of the menu item.
     * @param runnable The {@link Runnable} action to be executed when user choose the item.
     */
    public BrowserActionsFallbackMenuItem(String title, Runnable runnable) {
        mTitle = title;
        mRunnableAction = runnable;
    }

    /**
     * Constructs a predefined fallback menu item with a PendingIntennt action. The item will have
     * no icon and no custom Runnable action.
     * @param title The title of the menu item.
     * @param pendingIntent The {@link PendingIntent} action to be executed when user choose the
     *                      item.
     */
    public BrowserActionsFallbackMenuItem(String title, PendingIntent pendingIntent) {
        mTitle = title;
        mPendingIntentAction = pendingIntent;
    }

    /**
     * Gets the title of the context menu item.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Gets the {@link DrawableRes} of the icon of a context menu item.
     */
    @DrawableRes
    public int getIconId() {
        return mIconId;
    }

    /**
     * Gets the {@link Uri} of the icon of a context menu item.
     */
    Uri getIconUri() {
        return mIconUri;
    }

    /**
     * Gets the {@link PendingIntent} action of context menu item.
     * Return null for predefined menu item.
     */
    PendingIntent getPendingIntentAction() {
        return mPendingIntentAction;
    }

    /**
     * Gets the {@link Runnable} action of context menu item.
     * Return null for predefined menu item.
     */
    Runnable getRunnableAction() {
        return mRunnableAction;
    }
}
