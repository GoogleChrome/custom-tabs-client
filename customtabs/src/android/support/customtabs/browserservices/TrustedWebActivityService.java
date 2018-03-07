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
import android.support.annotation.CallSuper;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.NotifyNotificationBundle;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.CancelNotificationBundle;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.ResultBundle;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Locale;

// TODO(peconn): Javadoc
public class TrustedWebActivityService extends Service {
    // TODO(peconn): Check manifest entry is good.
    public static final String INTENT_ACTION =
            "android.support.customtabs.browserservices.TRUSTED_WEB_ACTIVITY_SERVICE";


    private NotificationManager mNotificationManager;

    private final ITrustedWebActivityService.Stub mBinder =
            new ITrustedWebActivityService.Stub() {
        @Override
        public Bundle notifyNotificationWithChannel(Bundle bundle) throws RemoteException {
            NotifyNotificationBundle args = new NotifyNotificationBundle(bundle);

            boolean success = TrustedWebActivityService.this.notifyNotificationWithChannel(
                    args.mPlatformTag, args.mPlatformId, args.mNotification, args.mChannelName);

            return new ResultBundle(success).toBundle();
        }

        @Override
        public void cancelNotification(Bundle bundle) throws RemoteException {
            CancelNotificationBundle args = new CancelNotificationBundle(bundle);

            TrustedWebActivityService.this.cancelNotification(args.mPlatformTag, args.mPlatformId);
        }

        @Override
        public int getSmallIconId() throws RemoteException {
            return TrustedWebActivityService.this.getSmallIconId();
        }
    };

    @Override
    @CallSuper
    public void onCreate() {
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    protected boolean notifyNotificationWithChannel(String platformTag, int platformId,
            Notification notification, String channelName) {
        ensureOnCreateCalled();

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = channelNameToId(channelName);
            // Create the notification channel, (no-op if already created).
            mNotificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_DEFAULT));

            // Check that the channel is enabled.
            if (mNotificationManager.getNotificationChannel(channelId).getImportance() ==
                    NotificationManager.IMPORTANCE_NONE) {
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
        ensureOnCreateCalled();
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

    private void ensureOnCreateCalled() {
        if (mNotificationManager != null) return;
        throw new RuntimeException("TrustedWebActivityService has not been properly initialized. "
                + "Did onCreate() call super.onCreate()?");
    }
}
