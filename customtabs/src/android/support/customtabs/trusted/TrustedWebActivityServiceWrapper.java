/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.customtabs.trusted;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;

/**
 * TrustedWebActivityServiceWrapper is used by a Trusted Web Activity provider app to wrap calls to
 * the {@link TrustedWebActivityService} in the client app.
 * All of these calls except {@link #getComponentName()} forward over IPC
 * to corresponding calls on {@link TrustedWebActivityService}, eg {@link #getSmallIconId()}
 * forwards to {@link TrustedWebActivityService#getSmallIconId()}.
 * <p>
 * These IPC calls are synchronous, though the {@link TrustedWebActivityService} method may hit the
 * disk. Therefore it is recommended to call them on a background thread (without StrictMode).
 */
public class TrustedWebActivityServiceWrapper {
    // Inputs.
    private static final String KEY_PLATFORM_TAG =
            "android.support.customtabs.trusted.PLATFORM_TAG";
    private static final String KEY_PLATFORM_ID =
            "android.support.customtabs.trusted.PLATFORM_ID";
    private static final String KEY_NOTIFICATION =
            "android.support.customtabs.trusted.NOTIFICATION";
    private static final String KEY_CHANNEL_NAME =
            "android.support.customtabs.trusted.CHANNEL_NAME";

    // Outputs.
    private static final String KEY_NOTIFICATION_SUCCESS =
            "android.support.customtabs.trusted.NOTIFICATION_SUCCESS";

    private final ITrustedWebActivityService mService;
    private final ComponentName mComponentName;

    TrustedWebActivityServiceWrapper(ITrustedWebActivityService service,
            ComponentName componentName) {
        mService = service;
        mComponentName = componentName;
    }

    /**
     * Requests a notification be shown.
     * @param platformTag The tag to identify the notification.
     * @param platformId The id to identify the notification.
     * @param notification The notification.
     * @param channel The name of the channel in the Trusted Web Activity client app to display the
     *                notification on.
     * @return Whether notifications or the notification channel are blocked for the client app.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    public boolean notify(String platformTag, int platformId, Notification notification,
            String channel) throws RemoteException {
        Bundle args = new NotifyNotificationArgs(platformTag, platformId, notification, channel)
                .toBundle();
        return new ResultArgs(mService.notifyNotificationWithChannel(args)).mSuccess;
    }

    /**
     * Requests a notification be cancelled.
     * @param platformTag The tag to identify the notification.
     * @param platformId The id to identify the notification.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    public void cancel(String platformTag, int platformId) throws RemoteException {
        Bundle args = new CancelNotificationArgs(platformTag, platformId).toBundle();
        mService.cancelNotification(args);
    }

    /**
     * Requests an Android resource id to be used for the notification small icon.
     * @return An Android resource id for the notification small icon. -1 if non found.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    public int getSmallIconId() throws RemoteException {
        return mService.getSmallIconId();
    }

    /**
     * Gets the {@link ComponentName} of the connected Trusted Web Activity client app.
     * @return The Trusted Web Activity client app component name.
     */
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

    static class ResultArgs {
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
