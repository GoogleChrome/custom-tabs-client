package android.support.customtabs.trusted.splashscreens;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabColorSchemeParams;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.TrustedWebUtils;
import android.support.customtabs.trusted.TrustedWebActivityIntentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Predicts system status bar and navigation bar colors that are about to be shown in a Trusted Web
 * Activity based on an instance of {@link TrustedWebActivityIntentBuilder}.
 */
class SystemBarColorPredictor {

    private static final int CHROME_76_VERSION = 380900000;

    private Map<String, SupportedFeatures> mSupportedFeaturesCache = new HashMap<>();

    SystemBarColorPredictor() {}

    /**
     * Makes a best-effort guess about which status bar color will be used when the Trusted Web
     * Activity is launched. Returns null if not possible to predict.
     */
    @Nullable
    Integer getExpectedStatusBarColor(Context context, String providerPackage,
            TrustedWebActivityIntentBuilder builder) {
        Intent intent = builder.buildCustomTabsIntent().intent;
        if (providerSupportsColorSchemeParams(context, providerPackage)) {
            int colorScheme = getExpectedColorScheme(context, builder);
            CustomTabColorSchemeParams params = CustomTabsIntent.getColorSchemeParams(intent,
                    colorScheme);
            return params.toolbarColor;
        }
        Bundle extras = intent.getExtras();
        return extras == null ? null : (Integer) extras.get(CustomTabsIntent.EXTRA_TOOLBAR_COLOR);
    }

    /**
     * Makes a best-effort guess about which navigation bar color will be used when the Trusted Web
     * Activity is launched. Returns null if not possible to predict.
     */
    @Nullable
    Integer getExpectedNavbarColor(Context context, String providerPackage,
            TrustedWebActivityIntentBuilder builder) {
        Intent intent = builder.buildCustomTabsIntent().intent;
        if (providerSupportsNavBarColorCustomization(context, providerPackage)) {
            if (providerSupportsColorSchemeParams(context, providerPackage)) {
                int colorScheme = getExpectedColorScheme(context, builder);
                CustomTabColorSchemeParams params = CustomTabsIntent.getColorSchemeParams(intent,
                        colorScheme);
                return params.navigationBarColor;
            }
            Bundle extras = intent.getExtras();
            return extras == null ? null :
                    (Integer) extras.get(CustomTabsIntent.EXTRA_NAVIGATION_BAR_COLOR);
        }
        if (TrustedWebUtils.SUPPORTED_CHROME_PACKAGES.contains(providerPackage)) {
            // Prior to adding support for nav bar color customization, Chrome had always set
            // the white color.
            return Color.WHITE;
        }
        return null;
    }

    private boolean providerSupportsNavBarColorCustomization(Context context,
            String providerPackage) {
        return getSupportedFeatures(context, providerPackage).navbarColorCustomization;
    }

    private boolean providerSupportsColorSchemeParams(Context context, String providerPackage) {
        return getSupportedFeatures(context, providerPackage).colorSchemeCustomization;
    }

    private static int getVersion(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private SupportedFeatures getSupportedFeatures(Context context,
            String providerPackage) {
        SupportedFeatures cached = mSupportedFeaturesCache.get(providerPackage);
        if (cached != null) return cached;

        if (isChrome76(context, providerPackage)) {
            // Chrome 76 supports both features, but doesn't advertise it with categories.
            SupportedFeatures features = new SupportedFeatures(true, true);
            mSupportedFeaturesCache.put(providerPackage, features);
            return features;
        }

        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(providerPackage);
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(serviceIntent,
                PackageManager.GET_RESOLVED_FILTER);

        SupportedFeatures features = new SupportedFeatures(
                hasCategory(resolveInfo, CustomTabsService.CATEGORY_NAVBAR_COLOR_CUSTOMIZATION),
                hasCategory(resolveInfo, CustomTabsService.CATEGORY_COLOR_SCHEME_CUSTOMIZATION)
        );
        mSupportedFeaturesCache.put(providerPackage, features);
        return features;
    }

    private boolean isChrome76(Context context, String providerPackage) {
        return TrustedWebUtils.SUPPORTED_CHROME_PACKAGES.contains(providerPackage)
            && getVersion(context, providerPackage) >= CHROME_76_VERSION;
    }

    private static boolean hasCategory(ResolveInfo info, String category) {
        return info != null && info.filter != null && info.filter.hasCategory(category);
    }

    private static int getExpectedColorScheme(Context context, TrustedWebActivityIntentBuilder builder) {
        Intent intent = builder.buildCustomTabsIntent().intent;
        Bundle extras = intent.getExtras();
        Integer scheme = extras == null ? null :
                (Integer) extras.get(CustomTabsIntent.EXTRA_COLOR_SCHEME);
        if (scheme != null && scheme != CustomTabsIntent.COLOR_SCHEME_SYSTEM) {
            return scheme;
        }
        boolean systemIsInDarkMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return systemIsInDarkMode ? CustomTabsIntent.COLOR_SCHEME_DARK :
                CustomTabsIntent.COLOR_SCHEME_LIGHT;
    }

    // This will be part of feature detection API soon.
    private static class SupportedFeatures {
        public final boolean navbarColorCustomization;
        public final boolean colorSchemeCustomization;

        private SupportedFeatures(boolean navbarColorCustomization,
                boolean colorSchemeCustomization) {
            this.navbarColorCustomization = navbarColorCustomization;
            this.colorSchemeCustomization = colorSchemeCustomization;
        }
    }
}
