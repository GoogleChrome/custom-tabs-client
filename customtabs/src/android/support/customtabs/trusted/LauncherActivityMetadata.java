package android.support.customtabs.trusted;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


/**
 * Parses and holds on to metadata parameters associated with {@link LauncherActivity}.
 */
public class LauncherActivityMetadata {

    /**
     * Url to launch in a Trusted Web Activity, unless other url provided in a VIEW intent.
     */
    private static final String METADATA_DEFAULT_URL =
            "android.support.customtabs.trusted.DEFAULT_URL";

    /**
     * Status bar color to use for Trusted Web Activity.
     */
    private static final String METADATA_STATUS_BAR_COLOR_ID =
            "android.support.customtabs.trusted.STATUS_BAR_COLOR";

    /**
     * Id of the Drawable to use as a splash screen.
     */
    private static final String METADATA_SPLASH_SCREEN_DRAWABLE_ID =
            "android.support.customtabs.trusted.SPLASH_SCREEN_DRAWABLE_ID";


    private final static int DEFAULT_STATUS_BAR_COLOR_ID = android.R.color.white;

    @Nullable public final String defaultUrl;
    public final int statusBarColorId;
    public final int splashScreenDrawableId;

    private LauncherActivityMetadata() {
        defaultUrl = null;
        statusBarColorId = DEFAULT_STATUS_BAR_COLOR_ID;
        splashScreenDrawableId = 0;
    }

    private LauncherActivityMetadata(@NonNull Bundle metaData) {
        defaultUrl = metaData.getString(METADATA_DEFAULT_URL);
        statusBarColorId = metaData.getInt(METADATA_STATUS_BAR_COLOR_ID,
                DEFAULT_STATUS_BAR_COLOR_ID);
        splashScreenDrawableId = metaData.getInt(METADATA_SPLASH_SCREEN_DRAWABLE_ID, 0);
    }

    /**
     * Creates LauncherActivityMetadata instance based on metadata of the passed Activity.
     */
    public static LauncherActivityMetadata parse(Activity activity) {
        Bundle metaData = null;
        try {
            metaData = activity.getPackageManager().getActivityInfo(
                    new ComponentName(activity, activity.getClass()),
                    PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            // Will only happen if the package provided (the one we are running in) is not
            // installed - so should never happen.
        }
        return metaData == null ? new LauncherActivityMetadata()
                : new LauncherActivityMetadata(metaData);
    }
}
