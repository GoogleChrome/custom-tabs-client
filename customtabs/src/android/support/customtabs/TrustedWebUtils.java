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

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.BundleCompat;

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
     * Boolean extra that triggers a {@link CustomTabsIntent} launch to be in a fullscreen UI with
     * no browser controls.
     *
     * @see TrustedWebUtils#launchAsTrustedWebActivity
     */
    public static final String EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY =
            "android.support.customtabs.extra.LAUNCH_AS_TRUSTED_WEB_ACTIVITY";

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
}
