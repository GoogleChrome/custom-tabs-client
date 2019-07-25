package android.support.customtabs.trusted;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

/**
 * Utilities used by helper classes that are setting up and launching Trusted Web Activities.
 */
public class Utils {

    /** Sets status bar color. Makes the icons dark if necessary. */
    public static void setStatusBarColor(Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        activity.getWindow().setStatusBarColor(color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && shouldUseDarkIconsOnBackground(color)) {
            addSystemUiVisibilityFlag(activity, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    /** Sets navigation bar color. Makes the icons dark if necessary */
    public static void setNavigationBarColor(Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        activity.getWindow().setNavigationBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && shouldUseDarkIconsOnBackground(color)) {
            addSystemUiVisibilityFlag(activity, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private static void addSystemUiVisibilityFlag(Activity activity, int flag) {
        View root = activity.getWindow().getDecorView().getRootView();
        int visibility = root.getSystemUiVisibility();
        visibility |= flag;
        root.setSystemUiVisibility(visibility);
    }

    /**
     * Determines whether to use dark icons on a background with given color by comparing the
     * contrast ratio (https://www.w3.org/TR/WCAG20/#contrast-ratiodef) to a threshold.
     * This criterion matches the one used by Chrome:
     * https://chromium.googlesource.com/chromium/src/+/90ac05ba6cb9ab5d5df75f0cef62c950be3716c3/chrome/android/java/src/org/chromium/chrome/browser/util/ColorUtils.java#215
     */
    private static boolean shouldUseDarkIconsOnBackground(@ColorInt int backgroundColor) {
        float luminance = 0.2126f * luminanceOfColorComponent(Color.red(backgroundColor))
                + 0.7152f * luminanceOfColorComponent(Color.green(backgroundColor))
                + 0.0722f * luminanceOfColorComponent(Color.blue(backgroundColor));
        float contrast = Math.abs((1.05f) / (luminance + 0.05f));
        return contrast < 3;
    }

    private static float luminanceOfColorComponent(float c) {
        c /= 255f;
        return (c < 0.03928f) ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    /**
     * Converts drawable located at given resource id into a Bitmap.
     */
    @Nullable
    public static Bitmap convertDrawableToBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) {
            return null;
        }
        drawable = DrawableCompat.wrap(drawable);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
