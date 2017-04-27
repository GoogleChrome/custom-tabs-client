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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.ContextCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Class holding the {@link Intent} and start bundle for a Browser Actions Activity.
 *
 * <p>
 * <strong>Note:</strong> The constants below are public for the browser implementation's benefit.
 * You are strongly encouraged to use {@link BrowserActionsIntent.Builder}.</p>
 */
public class BrowserActionsIntent {
    private final static String TEST_URL = "https://www.example.com";

    /**
     * Extra that specifies {@link PendingIntent} indicating which Application sends the {@link
     * BrowserActionsIntent}.
     */
    public final static String EXTRA_APP_ID = "android.support.customtabs.browseractions.APP_ID";

    /**
     * Indicates that the user explicitly opted out of Browser Actions in the calling application.
     */
    public static final String ACTION_BROWSER_ACTIONS_OPEN =
            "android.support.customtabs.browseractions.browser_action_open";

    /**
     * Extra bitmap that specifies the icon of a custom item shown in the Browser Actions menu.
     */
    public static final String KEY_ICON = "android.support.customtabs.browseractions.ICON";

    /**
     * Extra string that specifies the title of a custom item shown in the Browser Actions menu.
     */
    public static final String KEY_TITLE = "android.support.customtabs.browseractions.TITLE";

    /**
     * Extra PendingIntent to be launched when a custom item is selected in the Browser Actions
     * menu.
     */
    public static final String KEY_ACTION = "android.support.customtabs.browseractions.ACTION";

    /**
     * Extra that specifies {@link BrowserActionsUrlType} type of url for the Browser Actions menu.
     */
    public static final String EXTRA_TYPE = "android.support.customtabs.browseractions.extra.TYPE";

    /**
     * Extra that specifies List<Bundle> used for adding custom items to the Browser Actions menu.
     */
    public static final String EXTRA_MENU_ITEMS =
            "android.support.customtabs.browseractions.extra.MENU_ITEMS";

    /**
     * The maximum allowed number of custom items.
     */
    public static final int MAX_CUSTOM_ITEMS = 5;

    /**
     * Defines the types of url for Browser Actions menu.
     */
    @IntDef({URL_TYPE_NONE, URL_TYPE_IMAGE, URL_TYPE_VIDEO, URL_TYPE_AUDIO, URL_TYPE_FILE,
            URL_TYPE_PLUGIN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BrowserActionsUrlType {}
    public static final int URL_TYPE_NONE = 0;
    public static final int URL_TYPE_IMAGE = 1;
    public static final int URL_TYPE_VIDEO = 2;
    public static final int URL_TYPE_AUDIO = 3;
    public static final int URL_TYPE_FILE = 4;
    public static final int URL_TYPE_PLUGIN = 5;

    /**
     * An {@link Intent} used to start the Browser Actions Activity.
     */
    private final Intent mIntent;

    /**
     * Gets the Intent of {@link BrowserActionsIntent}.
     * @return the Intent of {@link BrowserActionsIntent}.
     */
    public Intent getIntent() {
        return mIntent;
    }

    private BrowserActionsIntent(@NonNull Intent intent) {
        this.mIntent = intent;
    }

    /**
     * Builder class for opening a Browser Actions context menu.
     */
    public static final class Builder {
        private final Intent mIntent = new Intent(BrowserActionsIntent.ACTION_BROWSER_ACTIONS_OPEN);
        private Context mContext;
        private Uri mUri;
        @BrowserActionsUrlType
        private int mType;
        private ArrayList<Bundle> mMenuItems = null;

        /**
         * Constructs a {@link BrowserActionsIntent.Builder} object associated with default setting
         * for a selected url.
         * @param context The context requesting the Browser Actions context menu.
         * @param uri The selected url for Browser Actions menu.
         */
        public Builder(Context context, Uri uri) {
            mContext = context;
            mUri = uri;
            mType = URL_TYPE_NONE;
            mMenuItems = new ArrayList<>();
        }

        /**
         * Sets the type of Browser Actions context menu.
         * @param type {@link BrowserActionsUrlType}.
         */
        public Builder setUrlType(@BrowserActionsUrlType int type) {
            mType = type;
            return this;
        }

        /**
         * Sets the custom items list.
         * Only maximum {@link BrowserActionsIntent.MAX_CUSTOM_ITEMS} custom items are allowed,
         * otherwise throws an {@link IllegalStateException}.
         * @param items The list of {@link BrowserActionItem} for custom items.
         */
        public Builder setCustomItems(ArrayList<BrowserActionItem> items) {
            if (items.size() >= MAX_CUSTOM_ITEMS) {
                throw new IllegalStateException(
                        "Exceeded maximum toolbar item count of " + MAX_CUSTOM_ITEMS);
            }
            for (int i = 0; i < items.size(); i++) {
                mMenuItems.add(getBundleFromItem(items.get(i)));
            }
            return this;
        }

        /**
         * Populates a {@link Bundle} to hold a custom item for Browser Actions menu.
         * @param item A custom item for Browser Actions menu.
         * @return The Bundle of custom item.
         */
        private Bundle getBundleFromItem(BrowserActionItem item) {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_TITLE, item.getTitle());
            bundle.putParcelable(KEY_ACTION, item.getAction());
            if (item.getIcon() != null) bundle.putParcelable(KEY_ICON, item.getIcon());
            return bundle;
        }

        /**
         * Combines all the options that have been set and returns a new {@link
         * BrowserActionsIntent} object.
         */
        public BrowserActionsIntent build() {
            mIntent.setData(mUri);
            mIntent.putExtra(EXTRA_TYPE, mType);
            mIntent.putParcelableArrayListExtra(EXTRA_MENU_ITEMS, mMenuItems);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(), 0);
            mIntent.putExtra(EXTRA_APP_ID, pendingIntent);
            return new BrowserActionsIntent(mIntent);
        }
    }

    /**
     * Open a Browser Actions menu with default settings.
     * It first checks if any Browser Actions provider is available to create a context menu.
     * If not, open a Browser Actions menu locally from support library.
     * @param context The context requesting for a Browser Actions menu.
     * @param uri The url for Browser Actions menu.
     */
    public static void openBrowserAction(Context context, Uri uri) {
        if (hasBrowserActionsIntentHandler(context)) {
            BrowserActionsIntent intent = new BrowserActionsIntent.Builder(context, uri).build();
            ContextCompat.startActivity(context, intent.getIntent(), null);
        } else {
            openFallbackBrowserActionsMenu(
                    context, uri, URL_TYPE_NONE, new ArrayList<BrowserActionItem>());
        }
    }

    /**
     * Open a Browser Actions menu with custom items.
     * It first checks if any Browser Actions provider is available to create a context menu.
     * If not, open a Browser Actions menu locally from support library.
     * @param context The context requesting for a Browser Actions menu.
     * @param uri The url for Browser Actions menu.
     * @param type The type of the url for context menu to be opened.
     * @param items List of custom items to be added to Browser Actions menu.
     */
    public static void openBrowserAction(
            Context context, Uri uri, int type, ArrayList<BrowserActionItem> items) {
        if (hasBrowserActionsIntentHandler(context)) {
            BrowserActionsIntent intent = new BrowserActionsIntent.Builder(context, uri)
                                                      .setUrlType(type)
                                                      .setCustomItems(items)
                                                      .build();
            ContextCompat.startActivity(context, intent.getIntent(), null);
        } else {
            openFallbackBrowserActionsMenu(context, uri, type, items);
        }
    }

    /**
     * Check whether any Browser Actions provider is available to handle the {@link
     * BrowserActionsIntent}.
     * @param context The context requesting for a Browser Actions menu.
     * @return true If a Browser Actions provider is available handle the intent.
     */
    private static boolean hasBrowserActionsIntentHandler(Context context) {
        Intent intent =
                new Intent(BrowserActionsIntent.ACTION_BROWSER_ACTIONS_OPEN, Uri.parse(TEST_URL));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfoList =
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        return resolveInfoList.size() > 0 ? true : false;
    }

    /**
     * Open a Browser Actions menu from support library.
     * @param context The context requesting for a Browser Actions menu.
     * @param uri The url for Browser Actions menu.
     * @param type The type of the url for context menu to be opened.
     * @param items List of custom items to add to Browser Actions menu.
     */
    private static void openFallbackBrowserActionsMenu(
            Context context, Uri uri, int type, ArrayList<BrowserActionItem> items) {
        return;
    }

    /**
     * Get the package name of the creator application.
     * @param intent The {@link BrowserActionsIntent}.
     * @return The creator package name.
     */
    @SuppressWarnings("deprecation")
    public static String getCreatorPackageName(Intent intent) {
        PendingIntent pendingIntent = intent.getParcelableExtra(BrowserActionsIntent.EXTRA_APP_ID);
        if (pendingIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return pendingIntent.getCreatorPackage();
            } else {
                return pendingIntent.getTargetPackage();
            }
        }
        return null;
    }
}
