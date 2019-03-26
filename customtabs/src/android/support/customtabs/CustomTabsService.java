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
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Abstract service class for implementing Custom Tabs related functionality. The service should
 * be responding to the action ACTION_CUSTOM_TABS_CONNECTION. This class should be used by
 * implementers that want to provide Custom Tabs functionality, not by clients that want to launch
 * Custom Tabs.
 */
public abstract class CustomTabsService extends Service {
    /**
     * The Intent action that a CustomTabsService must respond to.
     */
    public static final String ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService";

    /**
     * An Intent filter category to signify that the Custom Tabs provider supports Trusted Web
     * Activities.
     */
    public final static String TRUSTED_WEB_ACTIVITY_CATEGORY =
            "androidx.browser.trusted.category.TrustedWebActivities";

    /**
     * For {@link CustomTabsService#mayLaunchUrl} calls that wants to specify more than one url,
     * this key can be used with {@link Bundle#putParcelable(String, android.os.Parcelable)}
     * to insert a new url to each bundle inside list of bundles.
     */
    public static final String KEY_URL =
            "android.support.customtabs.otherurls.URL";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESULT_SUCCESS, RESULT_FAILURE_DISALLOWED,
            RESULT_FAILURE_REMOTE_ERROR, RESULT_FAILURE_MESSAGING_ERROR})
    public @interface Result {
    }

    /**
     * Indicates that the postMessage request was accepted.
     */
    public static final int RESULT_SUCCESS = 0;
    /**
     * Indicates that the postMessage request was not allowed due to a bad argument or requesting
     * at a disallowed time like when in background.
     */
    public static final int RESULT_FAILURE_DISALLOWED = -1;
    /**
     * Indicates that the postMessage request has failed due to a {@link RemoteException} .
     */
    public static final int RESULT_FAILURE_REMOTE_ERROR = -2;
    /**
     * Indicates that the postMessage request has failed due to an internal error on the browser
     * message channel.
     */
    public static final int RESULT_FAILURE_MESSAGING_ERROR = -3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RELATION_USE_AS_ORIGIN, RELATION_HANDLE_ALL_URLS})
    public @interface Relation {
    }

    /**
     * Used for {@link CustomTabsSession#validateRelationship(int, Uri, Bundle)}. For
     * App -> Web transitions, requests the app to use the declared origin to be used as origin for
     * the client app in the web APIs context.
     */
    public static final int RELATION_USE_AS_ORIGIN = 1;
    /**
     * Used for {@link CustomTabsSession#validateRelationship(int, Uri, Bundle)}. Requests the
     * ability to handle all URLs from a given origin.
     */
    public static final int RELATION_HANDLE_ALL_URLS = 2;


    /**
     * Enumerates the possible purposes of files received in {@link #receiveFile}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FILE_PURPOSE_TWA_SPLASH_IMAGE})
    public @interface FilePurpose {
    }

    /**
     * File is a splash image to be shown on top of a Trusted Web Activity while the web contents
     * are loading.
     */
    public static final int FILE_PURPOSE_TWA_SPLASH_IMAGE = 1;

    private final Map<IBinder, DeathRecipient> mDeathRecipientMap = new ArrayMap<>();

    private ICustomTabsService.Stub mBinder = new ICustomTabsService.Stub() {

        @Override
        public boolean warmup(long flags) {
            return CustomTabsService.this.warmup(flags);
        }

        @Override
        public boolean newSession(ICustomTabsCallback callback) {
            return newSessionInternal(callback, null);
        }

        @Override
        public boolean newSessionWithExtras(ICustomTabsCallback callback, Bundle extras) {
            return newSessionInternal(callback, getSessionIdFromBundle(extras));
        }

        private boolean newSessionInternal(ICustomTabsCallback callback, PendingIntent sessionId) {
            final CustomTabsSessionToken sessionToken =
                    new CustomTabsSessionToken(callback, sessionId);
            try {
                DeathRecipient deathRecipient = () -> cleanUpSession(sessionToken);
                synchronized (mDeathRecipientMap) {
                    callback.asBinder().linkToDeath(deathRecipient, 0);
                    mDeathRecipientMap.put(callback.asBinder(), deathRecipient);
                }
                return CustomTabsService.this.newSession(sessionToken);
            } catch (RemoteException e) {
                return false;
            }
        }

        @Override
        public boolean mayLaunchUrl(ICustomTabsCallback callback, Uri url,
                                    Bundle extras, List<Bundle> otherLikelyBundles) {
            return CustomTabsService.this.mayLaunchUrl(
                    new CustomTabsSessionToken(callback, getSessionIdFromBundle(extras)),
                    url, extras, otherLikelyBundles);
        }

        @Override
        public Bundle extraCommand(String commandName, Bundle args) {
            return CustomTabsService.this.extraCommand(commandName, args);
        }

        @Override
        public boolean updateVisuals(ICustomTabsCallback callback, Bundle bundle) {
            return CustomTabsService.this.updateVisuals(
                    new CustomTabsSessionToken(callback, getSessionIdFromBundle(bundle)), bundle);
        }

        @Override
        public boolean requestPostMessageChannel(ICustomTabsCallback callback,
                                                 Uri postMessageOrigin) {
            return CustomTabsService.this.requestPostMessageChannel(
                    new CustomTabsSessionToken(callback, null), postMessageOrigin);
        }

        @Override
        public boolean requestPostMessageChannelWithExtras(ICustomTabsCallback callback,
                                                 Uri postMessageOrigin, Bundle extras) {
            return CustomTabsService.this.requestPostMessageChannel(
                    new CustomTabsSessionToken(callback, getSessionIdFromBundle(extras)),
                    postMessageOrigin);
        }

        @Override
        public int postMessage(ICustomTabsCallback callback, String message, Bundle extras) {
            return CustomTabsService.this.postMessage(
                    new CustomTabsSessionToken(callback, getSessionIdFromBundle(extras)),
                    message, extras);
        }

        @Override
        public boolean validateRelationship(
                ICustomTabsCallback callback, @Relation int relation, Uri origin, Bundle extras) {
            return CustomTabsService.this.validateRelationship(
                    new CustomTabsSessionToken(callback, getSessionIdFromBundle(extras)),
                    relation, origin, extras);
        }

        @Override
        public boolean receiveFile(ICustomTabsCallback callback, @NonNull Uri uri,
                @FilePurpose int purpose, @Nullable Bundle extras) {
            return CustomTabsService.this.receiveFile(
                    new CustomTabsSessionToken(callback, getSessionIdFromBundle(extras)),
                    uri, purpose, extras);
        }

        private @Nullable PendingIntent getSessionIdFromBundle(@Nullable Bundle bundle) {
            if (bundle == null) return null;

            PendingIntent sessionId = bundle.getParcelable(CustomTabsIntent.EXTRA_SESSION_ID);
            bundle.remove(CustomTabsIntent.EXTRA_SESSION_ID);
            return sessionId;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Called when the client side {@link IBinder} for this {@link CustomTabsSessionToken} is dead.
     * Can also be used to clean up {@link DeathRecipient} instances allocated for the given token.
     *
     * @param sessionToken The session token for which the {@link DeathRecipient} call has been
     *                     received.
     * @return Whether the clean up was successful. Multiple calls with two tokens holdings the
     * same binder will return false.
     */
    protected boolean cleanUpSession(CustomTabsSessionToken sessionToken) {
        try {
            synchronized (mDeathRecipientMap) {
                IBinder binder = sessionToken.getCallbackBinder();
                DeathRecipient deathRecipient =
                        mDeathRecipientMap.get(binder);
                binder.unlinkToDeath(deathRecipient, 0);
                mDeathRecipientMap.remove(binder);
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    /**
     * Warms up the browser process asynchronously.
     *
     * @param flags Reserved for future use.
     * @return Whether warmup was/had been completed successfully. Multiple successful
     * calls will return true.
     */
    protected abstract boolean warmup(long flags);

    /**
     * Creates a new session through an ICustomTabsService with the optional callback. This session
     * can be used to associate any related communication through the service with an intent and
     * then later with a Custom Tab. The client can then send later service calls or intents to
     * through same session-intent-Custom Tab association.
     *
     * @param sessionToken Session token to be used as a unique identifier. This also has access
     *                     to the {@link CustomTabsCallback} passed from the client side through
     *                     {@link CustomTabsSessionToken#getCallback()}.
     * @return Whether a new session was successfully created.
     */
    protected abstract boolean newSession(CustomTabsSessionToken sessionToken);

    /**
     * Tells the browser of a likely future navigation to a URL.
     * <p>
     * The method {@link CustomTabsService#warmup(long)} has to be called beforehand.
     * The most likely URL has to be specified explicitly. Optionally, a list of
     * other likely URLs can be provided. They are treated as less likely than
     * the first one, and have to be sorted in decreasing priority order. These
     * additional URLs may be ignored.
     * All previous calls to this method will be deprioritized.
     *
     * @param sessionToken       The unique identifier for the session. Can not be null.
     * @param url                Most likely URL.
     * @param extras             Reserved for future use.
     * @param otherLikelyBundles Other likely destinations, sorted in decreasing
     *                           likelihood order. Each Bundle has to provide a url.
     * @return Whether the call was successful.
     */
    protected abstract boolean mayLaunchUrl(CustomTabsSessionToken sessionToken, Uri url,
                                            Bundle extras, List<Bundle> otherLikelyBundles);

    /**
     * Unsupported commands that may be provided by the implementation.
     * <p>
     * <p>
     * <strong>Note:</strong>Clients should <strong>never</strong> rely on this method to have a
     * defined behavior, as it is entirely implementation-defined and not supported.
     * <p>
     * <p> This call can be used by implementations to add extra commands, for testing or
     * experimental purposes.
     *
     * @param commandName Name of the extra command to execute.
     * @param args        Arguments for the command
     * @return The result {@link Bundle}, or null.
     */
    protected abstract Bundle extraCommand(String commandName, Bundle args);

    /**
     * Updates the visuals of custom tabs for the given session. Will only succeed if the given
     * session matches the currently active one.
     *
     * @param sessionToken The currently active session that the custom tab belongs to.
     * @param bundle       The action button configuration bundle. This bundle should be constructed
     *                     with the same structure in {@link CustomTabsIntent.Builder}.
     * @return Whether the operation was successful.
     */
    protected abstract boolean updateVisuals(CustomTabsSessionToken sessionToken,
                                             Bundle bundle);

    /**
     * Sends a request to create a two way postMessage channel between the client and the browser
     * linked with the given {@link CustomTabsSession}.
     *
     * @param sessionToken      The unique identifier for the session. Can not be null.
     * @param postMessageOrigin A origin that the client is requesting to be identified as
     *                          during the postMessage communication.
     * @return Whether the implementation accepted the request. Note that returning true
     * here doesn't mean an origin has already been assigned as the validation is
     * asynchronous.
     */
    protected abstract boolean requestPostMessageChannel(CustomTabsSessionToken sessionToken,
                                                         Uri postMessageOrigin);

    /**
     * Sends a postMessage request using the origin communicated via
     * {@link CustomTabsService#requestPostMessageChannel(
     *CustomTabsSessionToken, Uri)}. Fails when called before
     * {@link PostMessageServiceConnection#notifyMessageChannelReady(Bundle)} is received on the
     * client side.
     *
     * @param sessionToken The unique identifier for the session. Can not be null.
     * @param message      The message that is being sent.
     * @param extras       Reserved for future use.
     * @return An integer constant about the postMessage request result. Will return
     * {@link CustomTabsService#RESULT_SUCCESS} if successful.
     */
    @Result
    protected abstract int postMessage(
            CustomTabsSessionToken sessionToken, String message, Bundle extras);

    /**
     * Request to validate a relationship between the application and an origin.
     *
     * If this method returns true, the validation result will be provided through
     * {@link CustomTabsCallback#onRelationshipValidationResult(int, Uri, boolean, Bundle)}.
     * Otherwise the request didn't succeed. The client must call
     * {@link CustomTabsClient#warmup(long)} before this.
     *
     * @param sessionToken The unique identifier for the session. Can not be null.
     * @param relation Relation to check, must be one of the {@code CustomTabsService#RELATION_* }
     *                 constants.
     * @param origin Origin for the relation query.
     * @param extras Reserved for future use.
     * @return true if the request has been submitted successfully.
     */
    protected abstract boolean validateRelationship(
            CustomTabsSessionToken sessionToken, @Relation int relation, Uri origin,
            Bundle extras);


    /**
     * Receive a file from client by given Uri, e.g. in order to display a large bitmap in a Custom
     * Tab.
     *
     * Prior to calling this method, the client grants a read permission to the target
     * Custom Tabs provider via {@link android.content.Context#grantUriPermission}.
     *
     * The file is read and processed (where applicable) synchronously.
     *
     * @param sessionToken The unique identifier for the session.
     * @param uri {@link Uri} of the file.
     * @param purpose Purpose of transferring this file, one of the constants enumerated in
     *                {@code CustomTabsService#FilePurpose}.
     * @param extras Reserved for future use.
     * @return {@code true} if the file was received successfully.
     */
    protected abstract boolean receiveFile(@NonNull CustomTabsSessionToken sessionToken,
            @NonNull Uri uri, @FilePurpose int purpose, @Nullable Bundle extras);
}
