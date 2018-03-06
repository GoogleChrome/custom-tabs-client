package android.support.customtabs.browserservices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Locale;

// TODO(peconn): Javadoc
public class TrustedWebActivityService extends Service {
    // TODO(peconn): Check manifest entry is good.
    public static final String INTENT_ACTION =
            "android.support.customtabs.browserservices.TRUSTED_WEB_ACTIVITY_SERVICE";

    public static final String KEY_PLATFORM_TAG =
            "android.support.customtabs.browserservices.PLATFORM_TAG";
    public static final String KEY_PLATFORM_ID =
            "android.support.customtabs.browserservices.PLATFORM_ID";
    public static final String KEY_NOTIFICATION =
            "android.support.customtabs.browserservices.NOTIFICATION";
    public static final String KEY_CHANNEL_NAME =
            "android.support.customtabs.browserservices.CHANNEL_NAME";
    public static final String KEY_SUCCESS =
            "android.support.customtabs.browserservices.SUCCESS";

    private NotificationManager mNotificationManager;

    private final ITrustedWebActivityService.Stub mBinder =
            new ITrustedWebActivityService.Stub() {
        @Override
        public Bundle notifyNotificationWithChannel(Bundle args) throws RemoteException {
            if (!args.containsKey(KEY_PLATFORM_TAG) ||
                    !args.containsKey(KEY_PLATFORM_ID) ||
                    !args.containsKey(KEY_NOTIFICATION) ||
                    !args.containsKey(KEY_CHANNEL_NAME)) {
                throw new RemoteException("Bundle must contain KEY_PLATFORM_TAG, "
                        + "KEY_PLATFORM_ID, KEY_NOTIFICATION and KEY_CHANNEL_NAME");
            }

            String platformTag = args.getString(KEY_PLATFORM_TAG);
            int platformId = args.getInt(KEY_PLATFORM_ID);
            Notification notification = args.getParcelable(KEY_NOTIFICATION);
            String channelName = args.getParcelable(KEY_CHANNEL_NAME);

            boolean success = TrustedWebActivityService.this.notifyNotificationWithChannel(
                    platformTag, platformId, notification, channelName);

            Bundle returnBundle = new Bundle();
            returnBundle.putBoolean(KEY_SUCCESS, success);
            return returnBundle;
        }

        @Override
        public void cancelNotification(Bundle args) throws RemoteException {
            if (!args.containsKey(KEY_PLATFORM_ID) || !args.containsKey(KEY_PLATFORM_TAG)) {
                throw new RemoteException("Bundle must contain KEY_PLATFORM_ID and "
                        + "KEY_PLATFORM_TAG");
            }

            String platformTag = args.getString(KEY_PLATFORM_TAG);
            int platformId = args.getInt(KEY_PLATFORM_ID);
            TrustedWebActivityService.this.cancelNotification(platformTag, platformId);
        }

        @Override
        public int getSmallIconId() throws RemoteException {
            return TrustedWebActivityService.this.getSmallIconId();
        }
    };

    @Override
    public void onCreate() {
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    protected boolean notifyNotificationWithChannel(String platformTag, int platformId,
            Notification notification, String channelName) {
        assert mNotificationManager != null : "onCreate must call super.onCreate";

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = channelNameToId(channelName);
            // Create the notification channel, (no-op if already created).
            mNotificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_DEFAULT));

            // Check that the channel is enabled.
            if (NotificationManager.IMPORTANCE_NONE ==
                    mNotificationManager.getNotificationChannel(channelId).getImportance()) {
                return false;
            }

            // Set our notification to have that channel.
            Notification.Builder builder = Notification.Builder.recoverBuilder(this, notification);
            builder.setChannelId(channelId);
            notification = builder.build();
        }

        mNotificationManager.notify(platformTag, platformId, notification);
        return true;
    }

    protected void cancelNotification(String platformTag, int platformId) {
        assert mNotificationManager != null : "onCreate must call super.onCreate";
        mNotificationManager.cancel(platformTag, platformId);
    }

    protected int getSmallIconId() {
        // TODO(peconn): Grab the icon from the manifest.
        return -1;
    }

    @Override
    final public IBinder onBind(Intent intent) {
        return verifyCaller(intent) ? mBinder : null;
    }

    private boolean verifyCaller(Intent intent) {
        // TODO(peconn): Only allow binding from registered Custom Tabs provider.
        return true;
    }

    public static final void setVerifiedProviderForTesting(String provider) {
        setVerifiedProvider(provider);
    }

    /* package */ static final void setVerifiedProvider(String provider) {
        // TODO(peconn): Persist.
    }

    private static String channelNameToId(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_channel_id";
    }

    /**
     * Packs the arguments for
     * {@link ITrustedWebActivityService#cancelNotification(android.os.Bundle)} in a Bundle.
     */
    public static Bundle createCancelNotificationBundle(String platformTag, int platformId) {
        Bundle args = new Bundle();
        args.putString(KEY_PLATFORM_TAG, platformTag);
        args.putInt(KEY_PLATFORM_ID, platformId);
        return args;
    }

    /**
     * Packs the arguments for
     * {@link ITrustedWebActivityService#notifyNotificationWithChannel(Bundle)} in a Bundle.
     */
    public static Bundle createNotifyNotificationBundle(String platformTag, int platformId,
            Notification notification, String channel) {
        Bundle args = new Bundle();
        args.putString(KEY_PLATFORM_TAG, platformTag);
        args.putInt(KEY_PLATFORM_ID, platformId);
        args.putParcelable(KEY_NOTIFICATION, notification);
        args.putString(KEY_CHANNEL_NAME, channel);
        return args;
    }

    public static boolean getNotifyNotificationSuccess(Bundle bundle) {
        return bundle.getBoolean(KEY_SUCCESS);
    }
}
