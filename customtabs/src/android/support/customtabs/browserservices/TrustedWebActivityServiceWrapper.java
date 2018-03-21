package android.support.customtabs.browserservices;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;

public class TrustedWebActivityServiceWrapper {
    // Inputs.
    static final String KEY_PLATFORM_TAG =
            "android.support.customtabs.browserservices.PLATFORM_TAG";
    static final String KEY_PLATFORM_ID =
            "android.support.customtabs.browserservices.PLATFORM_ID";
    static final String KEY_NOTIFICATION =
            "android.support.customtabs.browserservices.NOTIFICATION";
    static final String KEY_CHANNEL_NAME =
            "android.support.customtabs.browserservices.CHANNEL_NAME";

    // Outputs.
    static final String KEY_NOTIFICATION_SUCCESS =
            "android.support.customtabs.browserservices.NOTIFICATION_SUCCESS";

    private final ITrustedWebActivityService mService;
    private final ComponentName mComponentName;

    TrustedWebActivityServiceWrapper(ITrustedWebActivityService service,
            ComponentName componentName) {
        mService = service;
        mComponentName = componentName;
    }

    public boolean notify(String platformTag, int platformId, Notification notification,
            String channel) throws RemoteException {
        Bundle args = new NotifyNotificationArgs(platformTag, platformId, notification, channel)
                .toBundle();
        return new ResultArgs(mService.notifyNotificationWithChannel(args)).mSuccess;
    }

    public void cancel(String platformTag, int platformId) throws RemoteException {
        Bundle args = new CancelNotificationArgs(platformTag, platformId).toBundle();
        mService.cancelNotification(args);
    }

    public int getSmallIcon() throws RemoteException {
        return mService.getSmallIconId();
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    static class NotifyNotificationArgs {
        public final String mPlatformTag;
        public final int mPlatformId;
        public final Notification mNotification;
        public final String mChannelName;

        public NotifyNotificationArgs(String platformTag, int platformId,
                Notification notification, String channelName) {
            mPlatformTag = platformTag;
            mPlatformId = platformId;
            mNotification = notification;
            mChannelName = channelName;
        }

        public static NotifyNotificationArgs fromBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_PLATFORM_TAG);
            ensureBundleContains(bundle, KEY_PLATFORM_ID);
            ensureBundleContains(bundle, KEY_NOTIFICATION);
            ensureBundleContains(bundle, KEY_CHANNEL_NAME);

            return new NotifyNotificationArgs(bundle.getString(KEY_PLATFORM_TAG),
                    bundle.getInt(KEY_PLATFORM_ID),
                    (Notification) bundle.getParcelable(KEY_NOTIFICATION),
                    bundle.getString(KEY_CHANNEL_NAME));
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putString(KEY_PLATFORM_TAG, mPlatformTag);
            args.putInt(KEY_PLATFORM_ID, mPlatformId);
            args.putParcelable(KEY_NOTIFICATION, mNotification);
            args.putString(KEY_CHANNEL_NAME, mChannelName);
            return args;
        }
    }

    static class CancelNotificationArgs {
        public final String mPlatformTag;
        public final int mPlatformId;


        public CancelNotificationArgs(String platformTag, int platformId) {
            mPlatformTag = platformTag;
            mPlatformId = platformId;

        }

        public static CancelNotificationArgs fromBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_PLATFORM_TAG);
            ensureBundleContains(bundle, KEY_PLATFORM_ID);

            return new CancelNotificationArgs(bundle.getString(KEY_PLATFORM_TAG),
                    bundle.getInt(KEY_PLATFORM_ID));
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putString(KEY_PLATFORM_TAG, mPlatformTag);
            args.putInt(KEY_PLATFORM_ID, mPlatformId);
            return args;
        }
    }

    public static class ResultArgs {
        public final boolean mSuccess;

        public ResultArgs(boolean success) {
            mSuccess = success;
        }

        public ResultArgs(Bundle bundle) {
            ensureBundleContains(bundle, KEY_NOTIFICATION_SUCCESS);
            mSuccess = bundle.getBoolean(KEY_NOTIFICATION_SUCCESS);
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putBoolean(KEY_NOTIFICATION_SUCCESS, mSuccess);
            return args;
        }
    }

    private static void ensureBundleContains(Bundle args, String key) {
        if (args.containsKey(key)) return;
        throw new IllegalArgumentException("Bundle must contain " + key);
    }
}
