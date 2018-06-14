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

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsService.Relation;
import android.support.v4.app.BundleCompat;
import android.util.Log;

/**
 * Wrapper class that can be used as a unique identifier for a session. Also contains an accessor
 * for the {@link CustomTabsCallback} for the session if there was any.
 */
public class CustomTabsSessionToken {
    private static final String TAG = "CustomTabsSessionToken";
    private final ICustomTabsCallback mCallbackBinder;
    private final CustomTabsCallback mCallback;

    private final PendingIntent mSessionId;

    /* package */ static class MockCallback extends ICustomTabsCallback.Stub {
        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) {}

        @Override
        public void extraCallback(String callbackName, Bundle args) {}

        @Override
        public void onMessageChannelReady(Bundle extras) {}

        @Override
        public void onPostMessage(String message, Bundle extras) {}

        @Override
        public void onRelationshipValidationResult(@Relation int relation, Uri requestedOrigin,
                boolean result, Bundle extras) {}

        @Override
        public IBinder asBinder() {
            return this;
        }
    }

    /**
     * Obtain a {@link CustomTabsSessionToken} from an intent. See {@link CustomTabsIntent.Builder}
     * for ways to generate an intent for custom tabs.
     * @param intent The intent to generate the token from. This has to include an extra for
     *               {@link CustomTabsIntent#EXTRA_SESSION}.
     * @return The token that was generated.
     */
    public static CustomTabsSessionToken getSessionTokenFromIntent(Intent intent) {
        Bundle b = intent.getExtras();
        IBinder binder = BundleCompat.getBinder(b, CustomTabsIntent.EXTRA_SESSION);
        if (binder == null) return null;
        PendingIntent sessionId = intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID);
        return new CustomTabsSessionToken(ICustomTabsCallback.Stub.asInterface(binder), sessionId);
    }

    /**
     * Provides browsers a way to generate a mock {@link CustomTabsSessionToken} for testing
     * purposes.
     *
     * @return A mock token with no functionality.
     */
    @NonNull
    public static CustomTabsSessionToken createMockSessionTokenForTesting() {
        return new CustomTabsSessionToken(new MockCallback(), null);
    }

    CustomTabsSessionToken(ICustomTabsCallback callbackBinder, PendingIntent sessionId) {
        mCallbackBinder = callbackBinder;
        mSessionId = sessionId;
        mCallback = new CustomTabsCallback() {

            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
                try {
                    mCallbackBinder.onNavigationEvent(navigationEvent, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @Override
            public void extraCallback(String callbackName, Bundle args) {
                try {
                    mCallbackBinder.extraCallback(callbackName, args);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @Override
            public void onMessageChannelReady(Bundle extras) {
                try {
                    mCallbackBinder.onMessageChannelReady(extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @Override
            public void onPostMessage(String message, Bundle extras) {
                try {
                    mCallbackBinder.onPostMessage(message, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @Override
            public void onRelationshipValidationResult(@Relation int relation, Uri origin,
                                                       boolean result, Bundle extras) {
                try {
                    mCallbackBinder.onRelationshipValidationResult(
                            relation, origin, result, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

        };
    }

    IBinder getCallbackBinder() {
        return mCallbackBinder.asBinder();
    }

    @Override
    public int hashCode() {
        return getCallbackBinder().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomTabsSessionToken)) return false;
        CustomTabsSessionToken token = (CustomTabsSessionToken) o;
        return token.getCallbackBinder().equals(mCallbackBinder.asBinder());
    }

    /**
     * @return {@link CustomTabsCallback} corresponding to this session if there was any non-null
     *         callbacks passed by the client.
     */
    public CustomTabsCallback getCallback() {
        return mCallback;
    }

    /**
     * @return Whether this token is associated with the given session.
     */
    public boolean isAssociatedWith(CustomTabsSession session) {
        //return session.getBinder().equals(mCallbackBinder);
        return session.getId().equals(mSessionId);
    }

    /**
     * Returns the id associated with the session.
     */
    public PendingIntent getId() {
        return mSessionId;
    }
}
