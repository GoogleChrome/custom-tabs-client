package android.support.customtabs.browserservices;

import android.app.Notification;
import android.os.Bundle;

public class TrustedWebActivityServiceWrapper {
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

    public static class NotifyNotificationBundle {
        public final String mPlatformTag;
        public final int mPlatformId;
        public final Notification mNotification;
        public final String mChannelName;

        public NotifyNotificationBundle(String platformTag, int platformId,
                Notification notification, String channelName) {
            mPlatformTag = platformTag;
            mPlatformId = platformId;
            mNotification = notification;
            mChannelName = channelName;
        }

        public NotifyNotificationBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_PLATFORM_TAG);
            ensureBundleContains(bundle, KEY_PLATFORM_ID);
            ensureBundleContains(bundle, KEY_NOTIFICATION);
            ensureBundleContains(bundle, KEY_CHANNEL_NAME);

            mPlatformTag = bundle.getString(KEY_PLATFORM_TAG);
            mPlatformId = bundle.getInt(KEY_PLATFORM_ID);
            mNotification = bundle.getParcelable(KEY_NOTIFICATION);
            mChannelName = bundle.getParcelable(KEY_CHANNEL_NAME);
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

    public static class CancelNotificationBundle {
        public final String mPlatformTag;
        public final int mPlatformId;


        public CancelNotificationBundle(String platformTag, int platformId) {
            mPlatformTag = platformTag;
            mPlatformId = platformId;

        }

        public CancelNotificationBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_PLATFORM_TAG);
            ensureBundleContains(bundle, KEY_PLATFORM_ID);

            mPlatformTag = bundle.getString(KEY_PLATFORM_TAG);
            mPlatformId = bundle.getInt(KEY_PLATFORM_ID);

        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putString(KEY_PLATFORM_TAG, mPlatformTag);
            args.putInt(KEY_PLATFORM_ID, mPlatformId);
            return args;
        }
    }

    public static class ResultBundle {
        public final boolean mSuccess;

        public ResultBundle(boolean success) {
            mSuccess = success;
        }

        public ResultBundle(Bundle bundle) throws IllegalArgumentException {
            ensureBundleContains(bundle, KEY_SUCCESS);
            mSuccess = bundle.getBoolean(KEY_SUCCESS);
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putBoolean(KEY_SUCCESS, mSuccess);
            return args;
        }
    }

    private static void ensureBundleContains(Bundle args, String key) {
        if (args.containsKey(key)) return;
        throw new IllegalArgumentException("Bundle must contain " + key);
    }
}
