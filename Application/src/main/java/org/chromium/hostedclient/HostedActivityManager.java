// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.chromium.hostedclient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import org.chromium.chrome.browser.hosted.IBrowserConnectionCallback;
import org.chromium.chrome.browser.hosted.IBrowserConnectionService;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the connection with the warmup service.
 *
 * The class instance must be accessed from one thread at a time.
 */
public class HostedActivityManager {
    /**
     * Called when the user navigates away from the first URL.
     */
    public interface NavigationCallback {
        /**
         * Called when the user navigates away from the first URL.
         *
         * May be called on any thread.
         *
         * @param url URL the user is navigating to.
         * @param extras Reserved for future use.
         */
        void run(String url, Bundle extras);
    }

    private class HostedServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IBrowserConnectionCallback.Stub cb = null;
            if (mNavigationCallback != null) {
                cb = new IBrowserConnectionCallback.Stub() {
                        @Override
                        public void onUserNavigation(long sessionId, String url, Bundle extras)
                                throws RemoteException {
                            mNavigationCallback.run(url, extras);
                        }
                    };
            }
            synchronized (mLock) {
                mConnectionService = IBrowserConnectionService.Stub.asInterface(service);
                try {
                    if (cb != null) mConnectionService.finishSetup(cb);
                    mSessionId = mConnectionService.newSession();
                } catch (RemoteException e) {
                    mSessionId = -1;
                    mConnectionService = null;
                    return;
                }
                mServiceConnected = true;
                for (Runnable runnable : mServiceRunnables) {
                    mMainLooperHandler.post(runnable);
                }
                mServiceRunnables.clear();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (mLock) {
                mServiceConnected = false;
            }
            if (mShouldRebind) bindService();
        }
    }

    private static final String TAG = "BrowserConnection";

    static final String CHROME_PACKAGE = "com.google.android.apps.chrome";
    private static final String CHROME_SERVICE_CLASS_NAME =
            "org.chromium.chrome.browser.hosted.ChromeConnectionService";
    private static final String EXTRA_HOSTED_SESSION_ID = "hosted:session_id";
    private static final String EXTRA_HOSTED_KEEP_ALIVE = "hosted:keep_alive";

    private static final Object sConstructionLock = new Object();
    private static HostedActivityManager sInstance;

    private Object mLock; // Protects the variables in this paragraph.
    private List<Runnable> mServiceRunnables;
    private IBrowserConnectionService mConnectionService;
    private boolean mServiceConnected;

    private boolean mShouldRebind;
    private ServiceConnection mConnection;
    private Handler mMainLooperHandler; // Used for tasks needing the service.
    private final Context mContext;
    private boolean mBindHasBeenCalled;
    private NavigationCallback mNavigationCallback;
    private long mSessionId;

    private HostedActivityManager(Context context) {
        mLock = new Object();
        mServiceRunnables = new ArrayList<Runnable>();
        mContext = context;
        mSessionId = -1;
        mConnection = new HostedServiceConnection();
        mMainLooperHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Get the instance.
     *
     * @param context An activity.
     * @return the instance of HostedActivityManager.
     */
    public static HostedActivityManager getInstance(Activity context) {
        synchronized (sConstructionLock) {
            if (sInstance == null) {
                sInstance = new HostedActivityManager(context);
            }
            return sInstance;
        }
    }

    /**
     * Sets the navigation callback. Can only be set once, and must be set
     * before binding to the service.
     */
    public void setNavigationCallback(NavigationCallback navigationCallback) {
        if (!mBindHasBeenCalled && mNavigationCallback == null) {
            mNavigationCallback = navigationCallback;
        }
    }

    /**
     * Binds to the service.
     *
     * @return true for success.
     */
    public boolean bindService() {
        mBindHasBeenCalled = true;
        mShouldRebind = true;
        Intent intent = new Intent();
        intent.setClassName(CHROME_PACKAGE, CHROME_SERVICE_CLASS_NAME);
        boolean available;
        try {
            available = mContext.bindService(
                    intent, mConnection, Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
        } catch (SecurityException e) {
            return false;
        }
        return available;
    }

    /**
     * Unbinds from the service.
     *
     * No callback will be delivered after this call, and the shared state
     * between the application and the remote service is forgotten.
     */
    public void unbindService() {
        mShouldRebind = false;
        mContext.unbindService(mConnection);
    }

    /**
     * Warms up the browser.
     *
     * {@link bindService} must be called before this method.
     *
     * @return true for success.
     */
    public boolean warmup() {
        enqueueServiceRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnectionService.warmup(0);
                } catch (RemoteException e) {
                    // Nothing
                }
            }
        });
        return true;
    }

    /**
     * Signals to the browser that a collection of URL may be launched.
     *
     * {@link bindService} must be called before this method.
     *
     * @param url Most likely url.
     * @param otherLikelyUrls Other likely urls, sorted in decreasing likelyhood order. May be null.
     * @return true for success.
     */
    public boolean mayLaunchUrl(final String url, List<String> otherLikelyUrls) {
        final List<Bundle> otherLikelyBundles = new ArrayList<Bundle>();
        if (otherLikelyUrls != null) {
            for (String otherUrl : otherLikelyUrls) {
                Bundle bundle = new Bundle();
                bundle.putString("url", otherUrl);
                otherLikelyBundles.add(bundle);
            }
        }
        enqueueServiceRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnectionService.mayLaunchUrl(mSessionId, url, null, otherLikelyBundles);
                } catch (RemoteException e) {
                    // Nothing
                }
            }
        });
        return true;
    }

    /**
     * Loads a URL inside the hosted activity.
     *
     * @param url URL to navigate to.
     * @param intent Intent, built with {@link IntentBuilder}.
     */
    public void loadUrl(String url, HostedUiBuilder uiBuilder) {
        final Intent intent = uiBuilder.getIntent();
        final Bundle startBundle = uiBuilder.getStartBundle();
        intent.setData(Uri.parse(url));
        intent.putExtra(EXTRA_HOSTED_SESSION_ID, mSessionId);
        Intent keepAliveIntent = new Intent().setClassName(
                mContext.getPackageName(), KeepAliveService.class.getCanonicalName());
        intent.putExtra(EXTRA_HOSTED_KEEP_ALIVE, keepAliveIntent);
        // The service needs to be reachable to get a sessionID, which is
        // required to connect to the KeepAlive service. We don't want users to
        // have to bind to the service manually, so do it for them here.
        if (!mBindHasBeenCalled) {
            bindService();
        }
        enqueueServiceRunnable(new Runnable() {
                @Override
                public void run() {
                    // If bindService() has not been called before, the
                    // sessionId is unknown up to this point.
                    intent.putExtra(EXTRA_HOSTED_SESSION_ID, mSessionId);
                    mContext.startActivity(intent, startBundle);
                }
            });
    }

    /**
     * Posts a runnable to the main thread when the service is connected.
     *
     * When the service is connected, don't run the runnable right away to
     * preserve the ordering. If the service is not connected, then the tasks
     * will be posted once the service is reconnected, in onServiceConnected().
     */
    private void enqueueServiceRunnable(Runnable runnable) {
        synchronized (mLock) {
            if (mServiceConnected) {
                mMainLooperHandler.post(runnable);
            } else {
                mServiceRunnables.add(runnable);
            }
        }
    }
}
