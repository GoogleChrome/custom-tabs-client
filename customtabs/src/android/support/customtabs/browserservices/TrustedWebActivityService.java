package android.support.customtabs.browserservices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.CancelNotificationArgs;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.NotifyNotificationArgs;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.NotifyNotificationResultArgs;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.ResultArgs;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.GetSmallIconIdResultArgs;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.Locale;

// TODO(peconn): Javadoc
public class TrustedWebActivityService extends Service {
    public static final String INTENT_ACTION =
            "android.support.customtabs.browserservices.TRUSTED_WEB_ACTIVITY_SERVICE";
    public static final String SMALL_ICON_META_DATA_NAME =
            "android.support.customtabs.browserservices.SMALL_ICON";

    private static final String PREFS_FILE = "TrustedWebActivityVerifiedProvider";
    private static final String PREFS_VERIFIED_PROVIDER = "Provider";

    private NotificationManager mNotificationManager;

    private final ITrustedWebActivityService.Stub mBinder =
            new ITrustedWebActivityService.Stub() {
        @Override
        public Bundle notifyNotificationWithChannel(Bundle bundle) throws RemoteException {
            if (!checkCaller()) return new NotifyNotificationResultArgs(false, false).toBundle();

            NotifyNotificationArgs args = NotifyNotificationArgs.fromBundle(bundle);

            boolean success = TrustedWebActivityService.this.notifyNotificationWithChannel(
                    args.mPlatformTag, args.mPlatformId, args.mNotification, args.mChannelName);

            return new NotifyNotificationResultArgs(true, success).toBundle();
        }

        @Override
        public Bundle cancelNotification(Bundle bundle) throws RemoteException {
            if (!checkCaller()) return new ResultArgs(false).toBundle();
            checkCaller();

            CancelNotificationArgs args = CancelNotificationArgs.fromBundle(bundle);

            TrustedWebActivityService.this.cancelNotification(args.mPlatformTag, args.mPlatformId);
            return new ResultArgs(true).toBundle();
        }

        @Override
        public Bundle getSmallIconId() throws RemoteException {
            if (!checkCaller()) return new GetSmallIconIdResultArgs(false, -1).toBundle();

            return new GetSmallIconIdResultArgs(true,
                    TrustedWebActivityService.this.getSmallIconId()).toBundle();
        }

        private boolean checkCaller() throws RemoteException {
            String[] packages = getPackageManager().getPackagesForUid(getCallingUid());

            // We need to read Preferences. This should only be called on the Binder thread which
            // is designed to handle long running, blocking tasks, so disk I/O should be OK.
            StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
            String verifiedPackage = getPreferences(TrustedWebActivityService.this)
                    .getString(PREFS_VERIFIED_PROVIDER, null);
            StrictMode.setThreadPolicy(policy);

            for (String p: packages) {
                // if (p.equals(verifiedPackage)) return true;
            }

            // throw new RemoteException("Caller is not verified as Trusted Web Activity provider.");
            return false;
        }
    };

    @Override
    @CallSuper
    public void onCreate() {
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Displays the |notification| on a channel named |channelName| with the given |tag| and |id|.
     * The channel id will be programmatically created from |channelName|, and the channel will be
     * created if it doesn't already exist.
     *
     * Returns false if notifications or the given notification channel are blocked by the user.
     */
    protected boolean notifyNotificationWithChannel(String platformTag, int platformId,
            Notification notification, String channelName) {
        ensureOnCreateCalled();

        Log.d("Peter", "Notify...");
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
        Log.d("Peter", "Sent notification");
        return true;
    }

    /**
     * Cancels a notification with the given tag and id.
     */
    protected void cancelNotification(String platformTag, int platformId) {
        ensureOnCreateCalled();
        mNotificationManager.cancel(platformTag, platformId);
    }

    protected int getSmallIconId() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            return info.metaData.getInt(SMALL_ICON_META_DATA_NAME, -1);
        } catch (PackageManager.NameNotFoundException e) {
            // Will only happen if the package provided (the one we are running in) is not
            // installed - so should never happen.
            return -1;
        }
    }

    @Override
    final public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Should *not* be called on UI Thread, as accessing Preferences may hit disk.
     */
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    public static final void setVerifiedProviderForTesting(Context context,
            @Nullable String provider) {
        setVerifiedProvider(context, provider);
    }

    /* package */ static final void setVerifiedProvider(final Context context,
            @Nullable String provider) {
        final String providerEmptyChecked = provider.isEmpty() ? null : provider;

        // Perform on a background thread as accessing Preferences may cause disk access.
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                SharedPreferences.Editor editor = getPreferences(context).edit();
                editor.putString(PREFS_VERIFIED_PROVIDER, providerEmptyChecked);
                editor.apply();
                return null;
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static String channelNameToId(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_channel_id";
    }

    private void ensureOnCreateCalled() {
        if (mNotificationManager != null) return;
        throw new IllegalStateException("TrustedWebActivityService has not been properly "
                + "initialized. Did onCreate() call super.onCreate()?");
    }
}
