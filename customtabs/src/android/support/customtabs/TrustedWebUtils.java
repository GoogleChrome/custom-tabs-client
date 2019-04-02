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

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.trusted.TrustedWebActivityBuilder;
import android.support.v4.app.BundleCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;
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
    private static final String CHROME_LOCAL_BUILD_PACKAGE = "com.google.android.apps.chrome";
    private static final String CHROMIUM_LOCAL_BUILD_PACKAGE = "org.chromium.chrome";
    private static final String CHROME_CANARY_PACKAGE = "com.chrome.canary";
    private static final String CHROME_DEV_PACKAGE = "com.chrome.dev";
    private static final String CHROME_STABLE_PACKAGE = "com.android.chrome";
    private static final String CHROME_BETA_PACKAGE = "com.chrome.beta";

    /**
     * List of packages currently supporting Trusted Web Activities. This list is designed to be
     * passed into {@link CustomTabsClient#getPackageName}, so the order of this list is the order
     * of preference (we assume that if the user has Chrome Canary or Dev installed, it should be
     * used instead of Chrome Stable). Depending on the call to
     * {@link CustomTabsClient#getPackageName} the user's default browser may take preference over
     * all of these.
     */
    public static final List<String> SUPPORTED_CHROME_PACKAGES = Arrays.asList(
            CHROME_LOCAL_BUILD_PACKAGE,
            CHROMIUM_LOCAL_BUILD_PACKAGE,
            CHROME_CANARY_PACKAGE,
            CHROME_DEV_PACKAGE,
            CHROME_BETA_PACKAGE,
            CHROME_STABLE_PACKAGE);

    /**
     * The versions of Chrome for which we should warn the user if they are out of date. We can't
     * check the version on local builds (the version code is 1) and we assume Canary and Dev users
     * update regularly.
     */
    private static final List<String> VERSION_CHECK_CHROME_PACKAGES = Arrays.asList(
            CHROME_BETA_PACKAGE,
            CHROME_STABLE_PACKAGE);

    /**
     * The version code of Chrome that is built from branch 3626/Chrome M72. This is the version
     * that Trusted Web Activities were released in.
     */
    private static final int SUPPORTING_CHROME_VERSION_CODE = 362600000;

    private static final int NO_PREWARM_CHROME_VERSION_CODE = 368300000;

    /**
     * The resource identifier to be passed to {@link Resources#getIdentifier} specifying the
     * resource name and type of the string to show if launching with an out of date version of
     * Chrome.
     */
    private static final String UPDATE_CHROME_MESSAGE_RESOURCE_ID = "string/update_chrome_toast";

    /**
     * The resource identifier to be passed to {@link Resources#getIdentifier} specifying the
     * resource name and type of the string to show if launching with an out of date version of
     * Chrome.
     */
    private static final String NO_PROVIDER_RESOURCE_ID = "string/no_provider_toast";

    /**
     * Boolean extra that triggers a {@link CustomTabsIntent} launch to be in a fullscreen UI with
     * no browser controls.
     *
     * @see TrustedWebActivityBuilder#launchActivity
     */
    public static final String EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY =
            "android.support.customtabs.extra.LAUNCH_AS_TRUSTED_WEB_ACTIVITY";

    /**
     * @see TrustedWebActivityBuilder#setAdditionalTrustedOrigins
     */
    public static final String EXTRA_ADDITIONAL_TRUSTED_ORIGINS =
            "android.support.customtabs.extra.ADDITIONAL_TRUSTED_ORIGINS";

    /**
     * Extra that stores the {@link Bundle} of splash screen parameters.
     */
    public static final String EXTRA_SPLASH_SCREEN_PARAMS =
            "android.support.customtabs.trusted.EXTRA_SPLASH_SCREEN_PARAMS";

    /**
     * The key for background color of the splash screen.
     */
    public static final String KEY_SPLASH_SCREEN_BACKGROUND_COLOR =
            "android.support.customtabs.trusted.KEY_SPLASH_SCREEN_BACKGROUND_COLOR";

    public static final String ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA =
            "android.support.customtabs.action.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA";

    /**
     * Category to be declared by intent filter of a CustomTabService by providers which support
     * splash screens (version 1).
     * In this version the splash screen is transferred via {@link CustomTabsSession#receiveFile},
     * and then used by Trusted Web Activity when it is launch. Customizing background color is
     * supported by adding an intent extra, see {@link #EXTRA_SPLASH_SCREEN_PARAMS},
     * {@link #KEY_SPLASH_SCREEN_BACKGROUND_COLOR}.
     */
    private static final String CATEGORY_SPLASH_SCREENS =
            "android.support.customtabs.category.TrustedWebActivitySplashScreensV1";

    private TrustedWebUtils() {}

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

    /**
     * If we are about to launch a TWA on Chrome Beta or Stable at a version before TWAs are
     * supported, display a Toast to the user asking them to update.
     * @param context {@link Context} to launch the Toast and access Resources and the
     *                PackageManager.
     * @param chromePackage Chrome package we're about to use.
     */
    public static void promptForChromeUpdateIfNeeded(Context context, String chromePackage) {
        if (!TrustedWebUtils.VERSION_CHECK_CHROME_PACKAGES.contains(chromePackage)) return;
        if (!chromeNeedsUpdate(context.getPackageManager(), chromePackage)) {
            return;
        }

        showToastIfResourceExists(context, UPDATE_CHROME_MESSAGE_RESOURCE_ID);
    }

    /**
     * Show a toast asking the user to install a Custom Tabs provider.
     * @param context {@link Context} to launch the Toast and access Resources.
     */
    public static void showNoPackageToast(Context context) {
        showToastIfResourceExists(context, NO_PROVIDER_RESOURCE_ID);
    }

    /**
     * Returns whether {@link CustomTabsClient#warmup} needs to be called prior to launching a
     * Trusted Web Activity. Starting from version 73 Chrome does not require warmup, which allows
     * to launch Trusted Web Activities faster.
     */
    public static boolean warmupIsRequired(Context context, String packageName) {
        if (CHROME_LOCAL_BUILD_PACKAGE.equals(packageName) ||
                CHROMIUM_LOCAL_BUILD_PACKAGE.equals(packageName)) {
            return false;
        }
        if (!SUPPORTED_CHROME_PACKAGES.contains(packageName)) {
            return false;
        }
        return getVersionCode(context.getPackageManager(), packageName)
                < NO_PREWARM_CHROME_VERSION_CODE;
    }

    /**
     * Returns whether the splash screens feature is supported by the given package.
     * Note: you can call this method prior to connecting to a {@link CustomTabsService}. This way,
     * if true is returned, the splash screen can be shown as soon as possible.
     *
     * TODO(pshmakov): make TwaProviderPicker gather supported features, including splash screens,
     * to avoid extra PackageManager queries.
     */
    public static boolean splashScreensAreSupported(Context context, String packageName) {
        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(packageName);
        ResolveInfo resolveInfo =
               context.getPackageManager().resolveService(serviceIntent,
                       PackageManager.GET_RESOLVED_FILTER);
        if (resolveInfo == null || resolveInfo.filter == null) return false;
        return resolveInfo.filter.hasCategory(CATEGORY_SPLASH_SCREENS);
    }

    /**
     * Transfers the splash image to a Custom Tabs provider. The reading and decoding of the image
     * happens synchronously, so it's recommended to call this method on a worker thread.
     *
     * This method should be called prior to {@link TrustedWebActivityBuilder#launchActivity}.
     * Pass additional parameters, such as background color, using
     * {@link TrustedWebActivityBuilder#setSplashScreenParams(Bundle)}.
     *
     * @param context {@link Context} to use.
     * @param file {@link File} with the image.
     * @param fileProviderAuthority authority of {@link FileProvider} used to generate an URI for
     *                              the file.
     * @param packageName Package name of Custom Tabs provider.
     * @param session {@link CustomTabsSession} established with the Custom Tabs provider.
     * @return True if the image was received and processed successfully.
     */
    public static boolean transferSplashImage(Context context, File file,
            String fileProviderAuthority, String packageName, CustomTabsSession session) {
        Uri uri = FileProvider.getUriForFile(context, fileProviderAuthority, file);
        context.grantUriPermission(packageName, uri, FLAG_GRANT_READ_URI_PERMISSION);
        return session.receiveFile(uri, CustomTabsService.FILE_PURPOSE_TWA_SPLASH_IMAGE, null);
    }

    private static int getVersionCode(PackageManager pm, String packageName) {
        try {
            return pm.getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private static void showToastIfResourceExists(Context context, String resource) {
        int stringId = context.getResources().getIdentifier(resource, null,
                context.getPackageName());
        if (stringId == 0) return;

        Toast.makeText(context, stringId, Toast.LENGTH_LONG).show();
    }

    public static boolean chromeNeedsUpdate(PackageManager pm, String chromePackage) {
        int versionCode = getVersionCode(pm, chromePackage);
        if (versionCode == 0) {
            // Do nothing - the user doesn't get prompted to update, but falling back to Custom
            // Tabs should still work.
            return false;
        }
        return versionCode < SUPPORTING_CHROME_VERSION_CODE;
    }
}
