/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.customtabs;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsService.Relation;
import android.support.customtabs.trusted.TrustedWebActivityService;

/**
 * A callback class for custom tabs client to get messages regarding events in their custom tabs. In
 * the implementation, all callbacks are sent to the UI thread for the client.
 */
public class CustomTabsCallback {
    /**
     * Sent when the tab has started loading a page.
     */
    public static final int NAVIGATION_STARTED = 1;

    /**
     * Sent when the tab has finished loading a page.
     */
    public static final int NAVIGATION_FINISHED = 2;

    /**
     * Sent when the tab couldn't finish loading due to a failure.
     */
    public static final int NAVIGATION_FAILED = 3;

    /**
     * Sent when loading was aborted by a user action before it finishes like clicking on a link
     * or refreshing the page.
     */
    public static final int NAVIGATION_ABORTED = 4;

    /**
     * Sent when the tab becomes visible.
     */
    public static final int TAB_SHOWN = 5;

    /**
     * Sent when the tab becomes hidden.
     */
    public static final int TAB_HIDDEN = 6;

    /**
     * Key for the extra included in {@link #onRelationshipValidationResult} {@code extras}
     * containing whether the verification was performed while the device was online. This may be
     * missing in cases verification was short cut.
     */
    public static final String ONLINE_EXTRAS_KEY = "online";

    /**
     * To be called when a navigation event happens.
     *
     * @param navigationEvent The code corresponding to the navigation event.
     * @param extras Reserved for future use.
     */
    public void onNavigationEvent(int navigationEvent, Bundle extras) {}

    /**
     * Unsupported callbacks that may be provided by the implementation.
     *
     * <p>
     * <strong>Note:</strong>Clients should <strong>never</strong> rely on this callback to be
     * called and/or to have a defined behavior, as it is entirely implementation-defined and not
     * supported.
     *
     * <p> This can be used by implementations to add extra callbacks, for testing or experimental
     * purposes.
     *
     * @param callbackName Name of the extra callback.
     * @param args Arguments for the callback
     */
    public void extraCallback(String callbackName, Bundle args) {}

    /**
     * Called when {@link CustomTabsSession} has requested a postMessage channel through
     * {@link CustomTabsService#requestPostMessageChannel(
     * CustomTabsSessionToken, android.net.Uri)} and the channel
     * is ready for sending and receiving messages on both ends.
     *
     * @param extras Reserved for future use.
     */
    public void onMessageChannelReady(Bundle extras) {}

    /**
     * Called when a tab controlled by this {@link CustomTabsSession} has sent a postMessage.
     * If postMessage() is called from a single thread, then the messages will be posted in the
     * same order. When received on the client side, it is the client's responsibility to preserve
     * the ordering further.
     *
     * @param message The message sent.
     * @param extras Reserved for future use.
     */
    public void onPostMessage(String message, Bundle extras) {}

    /**
     * Called when a relationship validation result is available.
     *
     * @param relation Relation for which the result is available. Value previously passed to
     *                 {@link CustomTabsSession#validateRelationship(int, Uri, Bundle)}. Must be one
     *                 of the {@code CustomTabsService#RELATION_* } constants.
     * @param requestedOrigin Origin requested. Value previously passed to
     *                        {@link CustomTabsSession#validateRelationship(int, Uri, Bundle)}.
     * @param result Whether the relation was validated.
     * @param extras Reserved for future use.
     */
    public void onRelationshipValidationResult(@Relation int relation, Uri requestedOrigin,
                                               boolean result, Bundle extras) {}

    /* package */ static class Wrapper extends ICustomTabsCallback.Stub {
        private final CustomTabsCallback mCallback;
        private Context mApplicationContext;
        private ComponentName mServiceComponentName;

        private final Handler mHandler = new Handler(Looper.getMainLooper());

        Wrapper(@Nullable CustomTabsCallback callback, Context context,
                ComponentName componentName) {
            mCallback = callback;
            mApplicationContext = context;
            mServiceComponentName = componentName;
        }

        Wrapper(@Nullable CustomTabsCallback callback) {
            this(callback, null, null);
        }

        /* package */ void attachToService(Context context, ComponentName componentName) {
            mApplicationContext = context;
            mServiceComponentName = componentName;
        }

        @Override
        public void onNavigationEvent(final int navigationEvent, final Bundle extras)
                throws RemoteException {
            if (mCallback == null) return;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onNavigationEvent(navigationEvent, extras);
                }
            });
        }

        @Override
        public void extraCallback(final String callbackName, final Bundle args)
                throws RemoteException {
            if (mCallback == null) return;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.extraCallback(callbackName, args);
                }
            });
        }

        @Override
        public void onMessageChannelReady(final Bundle extras) throws RemoteException {
            if (mCallback == null) return;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onMessageChannelReady(extras);
                }
            });
        }

        @Override
        public void onPostMessage(final String message, final Bundle extras)
                throws RemoteException {
            if (mCallback == null) return;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPostMessage(message, extras);
                }
            });
        }

        @Override
        public void onRelationshipValidationResult(final int relation, final Uri requestedOrigin,
                                                   final boolean result, final Bundle extras)
                throws RemoteException {
            if (mServiceComponentName != null && mApplicationContext != null && result) {
                TrustedWebActivityService.setVerifiedProvider(mApplicationContext,
                        mServiceComponentName.getPackageName());
            }

            if (mCallback == null) return;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onRelationshipValidationResult(
                            relation, requestedOrigin, result, extras);
                }
            });
        }
    }
}
