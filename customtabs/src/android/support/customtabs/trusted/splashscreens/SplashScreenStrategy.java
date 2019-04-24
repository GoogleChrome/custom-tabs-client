package android.support.customtabs.trusted.splashscreens;

import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.trusted.TrustedWebActivityBuilder;

/**
 * Defines behavior of the splash screen shown when launching a TWA.
 */
public interface SplashScreenStrategy {

    /**
     * Called immediately in the beginning of TWA launching process (before establishing
     * connection with CustomTabsService). Can be used to display splash screen on the client app's
     * side.
     */
    void onTwaLaunchInitiated();

    /**
     * Called when TWA is ready to be launched.
     * @param builder {@link TrustedWebActivityBuilder} to be supplied with splash screen related
     * parameters.
     * @param session {@link CustomTabsSession} with which the TWA will launch.
     * @param onReadyCallback Callback to be triggered when splash screen preparation is finished.
     * TWA is launched immediately upon triggering this callback.
     */
    void configureTwaBuilder(TrustedWebActivityBuilder builder, CustomTabsSession session,
            Runnable onReadyCallback);

    /**
     * Performs clean-up.
     */
    void destroy();

}
