// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package android.support.customtabs.browseractions;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * The class to get information of context menu shown on Browser Actions fallback dialog.
 */
public class BrowserActionsFallbackMenuItem {
    /**
     * Defines the type of menu item.
     */
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({ITEM_TYPE_PREDEFINED, ITEM_TYPE_CUSTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FallbackMenuItemType {}
    public static final int ITEM_TYPE_PREDEFINED = 0;
    public static final int ITEM_TYPE_CUSTOM = 1;

    @IdRes
    private final int mMenuId;
    private final String mTitle;
    @FallbackMenuItemType
    private final int mMenuItemType;
    private int mIconId;
    private Uri mIconUri;
    private PendingIntent mAction;

    /**
     * Constructs a fallback menu item from a custom Browser Actions item.
     * @param menuId The id of the menu item.
     * @param item The {@link BrowserActionItem} used to construct the menu item.
     */
    public BrowserActionsFallbackMenuItem(int menuId, BrowserActionItem item) {
        mMenuId = menuId;
        mMenuItemType = ITEM_TYPE_CUSTOM;
        mTitle = item.getTitle();
        mIconId = item.getIconId();
        mIconUri = item.getIconUri();
        mAction = item.getAction();
    }

    /**
     * Constructs a predefined fallback menu item. The item will have no icon and no custom
     * PendingIntent action.
     * @param menuId The id of the menu item.
     * @param title The title of the menu item.
     */
    public BrowserActionsFallbackMenuItem(int menuId, String title) {
        mMenuId = menuId;
        mMenuItemType = ITEM_TYPE_PREDEFINED;
        mTitle = title;
    }

    /**
     * Gets the title of the context menu item.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Gets the id of the context menu item.
     */
    @IdRes
    public int getMenuId() {
        return mMenuId;
    }

    /**
     * Gets the type of the context menu item.
     */
    @FallbackMenuItemType
    public int getMenuItemType() {
        return mMenuItemType;
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
     * Gets the {@link PendigIntent} action of context menu item.
     * Return null for predefined menu item.
     */
    PendingIntent getAction() {
        return mAction;
    }
}
