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

package android.support.customtabs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.BundleCompat;

import java.util.Arrays;
import java.util.List;

/**
 * Class for utilities and convenience calls for opening a qualifying web page as a
 * Trusted Web Activity.
 *
 * Trusted Web Activity is a fullscreen UI with no visible browser controls that hosts web pages
 * meeting certain criteria. The full list of qualifications is at the implementing browser's
 * discretion, but minimum recommended set is for the web page :
 *  <ul>
 *      <li>To have declared delegate_permission/common.handle_all_urls relationship with the
 *      launching client application ensuring 1:1 trust between the Android native and web
 *      components. See https://developers.google.com/digital-asset-links/ for details.</li>
 *      <li>To work as a reliable, fast and engaging standalone component within the launching app's
 *      flow.</li>
 *      <li>To be accessible and operable even when offline.</li>
 *  </ul>
 *
 *  Fallback behaviors may also differ with implementation. Possibilities are launching the page in
 *  a custom tab, or showing it in browser UI. Browsers are encouraged to use
 *  {@link CustomTabsCallback#onRelationshipValidationResult(int, Uri, boolean, Bundle)}
 *  for sending details of the verification results.
 */
public class TrustedWebUtils {

    /**
     * List of packages currently supporting Trusted Web Activities.
     */
    public static final List<String> SUPPORTED_CHROME_PACKAGES = Arrays.asList(
            "com.google.android.apps.chrome",  // Chrome local build.
            "org.chromium.chrome",  // Chromium local build.
            "com.chrome.canary",  // Chrome Canary.
            "com.chrome.dev");  // Chrome Dev.

    /**
     * Boolean extra that triggers a {@link CustomTabsIntent} launch to be in a fullscreen UI with
     * no browser controls.
     *
     * @see TrustedWebUtils#launchAsTrustedWebActivity
     */
    public static final String EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY =
            "android.support.customtabs.extra.LAUNCH_AS_TRUSTED_WEB_ACTIVITY";

    public static final String ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA =
            "android.support.customtabs.action.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA";

    private TrustedWebUtils() {}

    /**
     * Launches the given {@link CustomTabsIntent} as a Trusted Web Activity. Once the Trusted Web
     * Activity is launched, browser side implementations may have their own fallback behavior (e.g.
     * Showing the page in a custom tab UI with toolbar) based on qualifications listed above or
     * more.
     *
     * @param context {@link Context} to use while launching the {@link CustomTabsIntent}.
     * @param session The {@link CustomTabsSession} used to create the intent.
     * @param intent The {@link CustomTabsIntent} to use for launching the Trusted Web Activity.
     *               Note that all customizations in the given associated with browser toolbar
     *               controls will be ignored.
     * @param uri The web page to launch as Trusted Web Activity.
     */
    public static void launchAsTrustedWebActivity(Context context, CustomTabsSession session,
            CustomTabsIntent intent, Uri uri) {
        session.validateRelationship(CustomTabsService.RELATION_HANDLE_ALL_URLS, uri, null);

        IBinder binder = BundleCompat.getBinder(intent.intent.getExtras(),
                CustomTabsIntent.EXTRA_SESSION);
        if (binder != session.getBinder()) {
            throw new IllegalArgumentException("Given CustomTabsIntent should be associated with "
                    + "the given CustomTabsSession");
        }

        intent.intent.putExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
        intent.launchUrl(context, uri);
    }

    /**
     * Open the site settings for given url in the web browser. The url must belong to the origin
     * associated with the calling application via the Digital Asset Links. Prior to calling, one
     * must establish a connection to {@link CustomTabsService} and create a
     * {@link CustomTabsSession}.
     *
     * It is also required to do {@link CustomTabsClient#warmup} and
     * {@link CustomTabsSession#validateRelationship} before calling this method.
     *
     * @param context {@link Context} to use while launching site-settings activity.
     * @param session The {@link CustomTabsSession} used to verify the origin.
     * @param uri The {@link Uri} for which site-settings are to be shown.
     */
    public static void launchBrowserSiteSettings(Context context, CustomTabsSession session,
            Uri uri) {
        Intent intent = new Intent(TrustedWebUtils.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA);
        intent.setPackage(session.getComponentName().getPackageName());
        intent.setData(uri);

        Bundle bundle = new Bundle();
        BundleCompat.putBinder(bundle, CustomTabsIntent.EXTRA_SESSION, session.getBinder());
        intent.putExtras(bundle);
        PendingIntent id = session.getId();
        if (id != null) {
            intent.putExtra(CustomTabsIntent.EXTRA_SESSION_ID, id);
        }
        context.startActivity(intent);
    }
}
