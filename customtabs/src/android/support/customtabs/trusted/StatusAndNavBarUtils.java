package android.support.customtabs.trusted;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

/**
 * Utility functions for changing status bar and navigation bar colors.
 * All functions include checks of SDK version.
 */
public class StatusAndNavBarUtils {

    /** Sets status bar color. */
    public static void setStatusBarColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        activity.getWindow().setStatusBarColor(color);
    }

    /** Darkens the color of status bar icons. */
    public static void setDarkStatusBarIcons(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        View rootView = activity.getWindow().getDecorView().getRootView();
        int systemUiVisibility = rootView.getSystemUiVisibility();
        systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        rootView.setSystemUiVisibility(systemUiVisibility);
    }

    /** Whitens the navigation bar. */
    public static void setWhiteNavigationBar(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        activity.getWindow().setNavigationBarColor(Color.WHITE);

        // Make the button icons dark
        View root = activity.findViewById(android.R.id.content);
        int visibility = root.getSystemUiVisibility();
        visibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        root.setSystemUiVisibility(visibility);
    }

    /**
     * Determines whether to use dark status bar icons by comparing the contrast ratio of the color
     * relative to white (https://www.w3.org/TR/WCAG20/#contrast-ratiodef) to a threshold.
     * This criterion matches the one used by Chrome:
     * https://chromium.googlesource.com/chromium/src/+/90ac05ba6cb9ab5d5df75f0cef62c950be3716c3/chrome/android/java/src/org/chromium/chrome/browser/util/ColorUtils.java#215
     */
    public static boolean shouldUseDarkStatusBarIcons(int statusBarColor) {
        float luminance = 0.2126f * luminanceOfColorComponent(Color.red(statusBarColor))
                + 0.7152f * luminanceOfColorComponent(Color.green(statusBarColor))
                + 0.0722f * luminanceOfColorComponent(Color.blue(statusBarColor));
        float contrast = Math.abs((1.05f) / (luminance + 0.05f));
        return contrast < 3;
    }

    private static float luminanceOfColorComponent(float c) {
        c /= 255f;
        return (c < 0.03928f) ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }
}
