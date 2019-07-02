package android.support.customtabs.trusted.splashscreens;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabColorSchemeParams;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.TrustedWebUtils;
import android.support.customtabs.trusted.TrustedWebActivityBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions that predict system status bar and navigation bar colors that are about to be
 * shown in a Trusted Web Activity based on an instance of {@link TrustedWebActivityBuilder}.
 */
class SystemBarColorPredictionUtils {

    private static Map<String, Boolean> sNavbarColorSupportCache = new HashMap<>();

    /**
     * Makes a best-effort guess about which status bar color will be used when the Trusted Web
     * Activity is launched. Returns null if not possible to predict.
     */
    @Nullable
    static Integer getExpectedStatusBarColor(Context context, String providerPackage,
            TrustedWebActivityBuilder builder) {
        if (providerSupportsColorSchemeParams(context, providerPackage)) {
            int colorScheme = getExpectedColorScheme(context, builder);
            CustomTabColorSchemeParams params = builder.getColorSchemeParams(colorScheme);
            if (params != null && params.toolbarColor != null) {
                return params.toolbarColor;
            }
        }
        return builder.getStatusBarColor();
    }

    /**
     * Makes a best-effort guess about which navigation bar color will be used when the Trusted Web
     * Activity is launched. Returns null if not possible to predict.
     */
    @Nullable
    static Integer getExpectedNavbarColor(Context context, String providerPackage,
            TrustedWebActivityBuilder builder) {
        if (providerSupportsNavBarColorCustomization(context, providerPackage)) {
            if (providerSupportsColorSchemeParams(context, providerPackage)) {
                int colorScheme = getExpectedColorScheme(context, builder);
                CustomTabColorSchemeParams params = builder.getColorSchemeParams(colorScheme);
                if (params != null && params.navigationBarColor != null) {
                    return params.navigationBarColor;
                }
            }
            return builder.getNavigationBarColor();
        }
        if (TrustedWebUtils.SUPPORTED_CHROME_PACKAGES.contains(providerPackage)) {
            // Prior to adding support for nav bar color customization, Chrome had always set
            // the white color.
            return Color.WHITE;
        }
        return null;
    }

    private static boolean providerSupportsNavBarColorCustomization(Context context,
            String providerPackage) {
        Boolean cached = sNavbarColorSupportCache.get(providerPackage);
        if (cached != null) return cached;

        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(providerPackage);
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(serviceIntent,
                PackageManager.GET_RESOLVED_FILTER);
        boolean supports = resolveInfo != null && resolveInfo.filter != null
            && resolveInfo.filter.hasCategory(
                    CustomTabsService.CATEGORY_NAVBAR_COLOR_CUSTOMIZATION);
        sNavbarColorSupportCache.put(providerPackage, supports);
        return supports;
    }

    private static boolean providerSupportsColorSchemeParams(Context context,
            String providerPackage) {
        // There is no category for this. In Chrome the support was added in the same version
        // as navbar color customization.
        if (TrustedWebUtils.SUPPORTED_CHROME_PACKAGES.contains(providerPackage)) {
            return providerSupportsNavBarColorCustomization(context, providerPackage);
        }
        return false;
    }

    private static int getExpectedColorScheme(Context context, TrustedWebActivityBuilder builder) {
        Integer scheme = builder.getColorScheme();
        if (scheme != null && scheme != CustomTabsIntent.COLOR_SCHEME_SYSTEM) {
            return scheme;
        }
        boolean systemIsInDarkMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return systemIsInDarkMode ? CustomTabsIntent.COLOR_SCHEME_DARK :
                CustomTabsIntent.COLOR_SCHEME_LIGHT;
    }
}
