package android.support.customtabs.trusted;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

/**
 * Utility functions for changing status bar and navigation bar colors
 */
public class StatusAndNavBarUtils {

    public static void setStatusBarColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        activity.getWindow().setStatusBarColor(color);
    }

    public static void setDarkStatusBarIcons(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        View rootView = activity.getWindow().getDecorView().getRootView();
        int systemUiVisibility = rootView.getSystemUiVisibility();
        systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        rootView.setSystemUiVisibility(systemUiVisibility);
    }

    public static void setNavigationBarColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        activity.getWindow().setNavigationBarColor(color);
    }

    /**
     * Determines whether to use dark status bar icons by comparing the contrast ratio of the color
     * relative to white (https://www.w3.org/TR/WCAG20/#contrast-ratiodef) to a threshold.
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
