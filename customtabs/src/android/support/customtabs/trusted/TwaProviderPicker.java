package android.support.customtabs.trusted;

import static android.support.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.TrustedWebUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Chooses a Browser/Custom Tabs/Trusted Web Activity provider and appropriate launch mode.
 * All browsers on the users device are considered, in order of Android's preference (so the user's
 * default will come first). We look for browsers that support Trusted Web Activities first, then
 * ones that support Custom Tabs falling back to any browser if neither of those criteria are
 * matched.
 *
 * A browser is an app that has an Activity that answers the following Intent:
 * <pre>
 * new Intent()
 *         .setAction(Intent.ACTION_VIEW)
 *         .addCategory(Intent.CATEGORY_BROWSABLE)
 *         .setData(Uri.parse("http://"));
 * </pre>
 *
 * A Custom Tabs provider is an app that has a Service that answers the following Intent:
 * <pre>
 * new Intent()
 *         .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
 * </pre>
 *
 * A Trusted Web Activity provider is an app that has a Service that answers the following Intent:
 *  * <pre>
 * new Intent()
 *         .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
 *         .addCategory(CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY);
 * </pre>
 */
public class TwaProviderPicker {
    @IntDef({TRUSTED_WEB_ACTIVITY, CUSTOM_TAB, BROWSER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LaunchMode {
    }

    /** The webapp should be launched as a Trusted Web Activity. */
    public static final int TRUSTED_WEB_ACTIVITY = 0;
    /** The webapp should be launched as a simple (no Session) Custom Tab. */
    public static final int CUSTOM_TAB = 1;
    /** The webapp should be opened in the browser. */
    public static final int BROWSER = 2;

    /**
     * The result of {@link #pickProvider}, holding the launch mode and package (which may be null
     * when the launchMode is BROWSER.
     */
    public static class Action {
        /** How the webapp should be launched. */
        @LaunchMode
        public final int launchMode;
        /** The provider, may be null when {@code launchMode == BROWSER}. */
        @Nullable
        public final String provider;

        /** Creates this object with the given parameters. */
        public Action(@LaunchMode int launchMode, @Nullable String provider) {
            this.launchMode = launchMode;
            this.provider = provider;
        }
    }

    /**
     * Chooses an appropriate provider (see class description) and the launch mode that browser
     * supports.
     */
    public static Action pickProvider(PackageManager pm) {
        Intent queryBrowsersIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("http://"));

        String bestCctProvider = null;
        String bestBrowserProvider = null;

        // These packages will be in order of Android's preference.
        List<ResolveInfo> possibleProviders
                = pm.queryIntentActivities(queryBrowsersIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo possibleProvider : possibleProviders) {
            String providerName = possibleProvider.resolvePackageName;

            switch (highestLaunchModeSupported(pm, providerName)) {
                case TRUSTED_WEB_ACTIVITY:
                    return new Action(TRUSTED_WEB_ACTIVITY, providerName);
                case CUSTOM_TAB:
                    if (bestCctProvider == null) bestCctProvider = providerName;
                    break;
                case BROWSER:
                    if (bestBrowserProvider == null) bestBrowserProvider = providerName;
                    break;
            }
        }

        if (bestCctProvider != null) return new Action(CUSTOM_TAB, bestCctProvider);
        return new Action(BROWSER, bestBrowserProvider);
    }

    @LaunchMode
    private static int highestLaunchModeSupported(PackageManager pm, String packageName) {
        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(packageName);
        ResolveInfo resolveInfo =
                pm.resolveService(serviceIntent, PackageManager.GET_RESOLVED_FILTER);

        if (resolveInfo == null) return BROWSER;

        if (TrustedWebUtils.SUPPORTED_CHROME_PACKAGES.contains(packageName) &&
                !TrustedWebUtils.chromeNeedsUpdate(pm, packageName)) {
            // Chrome 72-74 support Trusted Web Activites but don't yet have the TWA category on
            // their CustomTabsService.
            return TRUSTED_WEB_ACTIVITY;
        }

        if (resolveInfo.filter != null &&
                resolveInfo.filter.hasCategory(TRUSTED_WEB_ACTIVITY_CATEGORY)) {
            return TRUSTED_WEB_ACTIVITY;
        }

        return CUSTOM_TAB;
    }
}
