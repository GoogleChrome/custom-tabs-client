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

package org.chromium.customtabsclient;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.chromium.chrome.browser.customtabs.ICustomTabsConnectionCallback;
import org.chromium.chrome.browser.customtabs.ICustomTabsConnectionService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles the connection with the warmup service.
 *
 * The class instance must be accessed from one thread at a time.
 */
public class CustomTabActivityManager {
    public static final String CATEGORY_CUSTOM_TABS = "android.intent.category.CUSTOM_TABS";

    /**
     * Called when the user navigates away from the first URL.
     */
    public interface NavigationCallback {
        /**
         * Called when a page navigation has started in the custom tab.
         *
         * May be called on any thread.
         *
         * @param url URL the user is navigating to.
         * @param extras Reserved for future use.
         */
        void onUserNavigationStarted(String url, Bundle extras);

        /**
         * Called when a page navigation has finished in the custom tab.
         *
         * May be called on any thread.
         *
         * @param url URL the user has navigated to.
         * @param extras Reserved for future use.
         */
        void onUserNavigationFinished(String url, Bundle extras);
    }

    private class CustomTabServiceConnection implements ServiceConnection {
        private void postAndClearPendingTasks() {
            for (Runnable runnable : mServiceRunnables) {
                mMainLooperHandler.post(runnable);
            }
            mServiceRunnables.clear();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ICustomTabsConnectionCallback.Stub cb = null;
            if (mNavigationCallback != null) {
                cb = new ICustomTabsConnectionCallback.Stub() {
                    @Override
                    public void onUserNavigationStarted(long sessionId, String url, Bundle extras)
                            throws RemoteException {
                        mNavigationCallback.onUserNavigationStarted(url, extras);
                    }

                    @Override
                    public void onUserNavigationFinished(long sessionId, String url, Bundle extras)
                            throws RemoteException {
                        mNavigationCallback.onUserNavigationFinished(url, extras);
                    }
                };
            }
            synchronized (mLock) {
                mConnectionService = ICustomTabsConnectionService.Stub.asInterface(service);
                try {
                    if (cb != null) mConnectionService.finishSetup(cb);
                    mSessionId = mConnectionService.newSession();
                } catch (RemoteException e) {
                    mSessionId = -1;
                    mConnectionService = null;
                    return;
                }
                mServiceConnected = true;
                postAndClearPendingTasks();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (mLock) {
                mServiceConnected = false;
                mConnectionService = null;
            }
            if (mShouldRebind) {
                boolean available = bindService();
                if (!available) {
                    // The remote service is not available. In this case, post
                    // all the tasks anyway, since one of the tasks can be
                    // sending the VIEW intent. Sending the VIEW intent should
                    // not be blocked by an unavailable service (for instance,
                    // the target version of Chrome doesn't have the background
                    // service).
                    Log.w(TAG, "The remote service is not available, purging the queue.");
                    synchronized (mLock) {
                        postAndClearPendingTasks();
                    }
                }
            }
        }
    }

    private static final String TAG = "CustomTabsConnection";
    static final String STABLE_PACKAGE = "com.android.chrome";
    static final String BETA_PACKAGE = "com.chrome.beta";
    static final String DEV_PACKAGE = "com.chrome.dev";
    static final String LOCAL_PACKAGE = "com.google.android.apps.chrome";

    private static final String EXTRA_CUSTOM_TABS_SESSION_ID =
            "android.support.CUSTOM_TABS:session_id";
    private static final String EXTRA_CUSTOM_TABS_KEEP_ALIVE =
            "android.support.CUSTOM_TABS:keep_alive";

    private static final Object sConstructionLock = new Object();
    private static CustomTabActivityManager sInstance;

    private Object mLock; // Protects the variables in this paragraph.
    private List<Runnable> mServiceRunnables;
    private ICustomTabsConnectionService mConnectionService;
    private boolean mServiceConnected;

    private boolean mShouldRebind;
    private ServiceConnection mConnection;
    private Handler mMainLooperHandler; // Used for tasks needing the service.
    private final Context mContext;
    private boolean mBindHasBeenCalled;
    private NavigationCallback mNavigationCallback;
    private long mSessionId;
    private String mPackageNameToUse;
    private boolean mBindServiceFailed;

    private CustomTabActivityManager(Context context) {
        mLock = new Object();
        mServiceRunnables = new ArrayList<Runnable>();
        mContext = context;
        mSessionId = -1;
        mConnection = new CustomTabServiceConnection();
        mMainLooperHandler = new Handler(Looper.getMainLooper());
        mPackageNameToUse = getPackageNameToUse();
    }

    /**
     * Get the instance.
     *
     * @param context An activity.
     * @return the instance of CustomTabActivityManager.
     */
    public static CustomTabActivityManager getInstance(Activity context) {
        synchronized (sConstructionLock) {
            if (sInstance == null) {
                sInstance = new CustomTabActivityManager(context);
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
        if (TextUtils.isEmpty(mPackageNameToUse)) {
            mPackageNameToUse = getPackageNameToUse();
            if (TextUtils.isEmpty(mPackageNameToUse)) return false;
        }
        mBindHasBeenCalled = true;
        mShouldRebind = true;
        Intent intent = new Intent();
        intent.addCategory(CATEGORY_CUSTOM_TABS);
        intent.setPackage(mPackageNameToUse);
        boolean available;
        try {
            available = mContext.bindService(
                    intent, mConnection, Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
        } catch (SecurityException e) {
            available = false;
        }
        mBindServiceFailed = !available;
        return available;
    }

    /**
     * Unbinds from the service.
     *
     * No callback will be delivered after this call, and the shared state
     * between the application and the remote service is forgotten.
     */
    public void unbindService() {
        mBindHasBeenCalled = false;
        mShouldRebind = false;
        mBindServiceFailed = false;
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
                    if (mConnectionService == null) return;
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
                    if (mConnectionService == null) return;
                    mConnectionService.mayLaunchUrl(mSessionId, url, null, otherLikelyBundles);
                } catch (RemoteException e) {
                    // Nothing
                }
            }
        });
        return true;
    }

    /**
     * Loads a URL inside a custom tab.
     *
     * @param url URL to navigate to.
     * @param intent Intent, built with {@link IntentBuilder}.
     */
    public void loadUrl(String url, CustomTabUiBuilder uiBuilder) {
        final Intent intent = uiBuilder.getIntent();
        final Bundle startBundle = uiBuilder.getStartBundle();
        intent.setData(Uri.parse(url));
        intent.putExtra(EXTRA_CUSTOM_TABS_SESSION_ID, mSessionId);
        Intent keepAliveIntent = new Intent().setClassName(
                mContext.getPackageName(), KeepAliveService.class.getCanonicalName());
        intent.putExtra(EXTRA_CUSTOM_TABS_KEEP_ALIVE, keepAliveIntent);
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
                    intent.putExtra(EXTRA_CUSTOM_TABS_SESSION_ID, mSessionId);
                    sendIntent(intent, startBundle);
                }
            });
    }

    /**
      * If there is a default handler for this intent use that. If we haven't found a viable
      * package for the service show an intent picker. Set the package name to be compatible with
      * the service connection if there are no speacialized handlers.
      * @param intent The intent to resolve against and send.
      * @param startBundle {@link ActivityOptions} bundle to use while starting the activity.
     */
    private void sendIntent(Intent intent, Bundle startBundle) {
        if (!hasDefaultActivityHandler(intent)
                && !TextUtils.isEmpty(mPackageNameToUse)
                && !hasSpecializedHandlerIntents(intent)) {
            intent.setPackage(mPackageNameToUse);
        }
        try {
            mContext.startActivity(intent, startBundle);
        } catch (ActivityNotFoundException e) {
            if (!TextUtils.isEmpty(intent.getPackage())) {
                Log.e(TAG, "No VIEW intent handlers");
                return;
            }
            // Reset the package name and try again.
            String packageNameToUse = getPackageNameToUse();
            intent.setPackage(packageNameToUse.equals(mPackageNameToUse) ? null : packageNameToUse);
            mPackageNameToUse = packageNameToUse;
            mContext.startActivity(intent, startBundle);
        }
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
            if (mServiceConnected || mBindServiceFailed) {
                mMainLooperHandler.post(runnable);
            } else {
                mServiceRunnables.add(runnable);
            }
        }
    }

    private boolean hasDefaultActivityHandler(Intent intent) {
        PackageManager pm = mContext.getPackageManager();
        ResolveInfo defaultHandlerInfo = pm.resolveActivity(intent, 0);
        return defaultHandlerInfo != null
                && defaultHandlerInfo.match != 0
                && defaultHandlerInfo.activityInfo != null;
    }

    /**
     * Goes through all apps that supports CATEGORY_CUSTOM_TABS in a service and VIEW intents. Picks
     * the one chosen by the user if there is one, otherwise makes a best effort to return a
     * valid package name.
     * @param context {@link Context} to use for accessing {@link PackageManager}.
     * @return The package name recommended to use for connecting to custom tabs related components.
     */
    private String getPackageNameToUse() {
        PackageManager pm = mContext.getPackageManager();

        // Get default VIEW intent handler.
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
        ResolveInfo defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0);
        String defaultViewHandlerPackageName = null;
        if (defaultViewHandlerInfo != null) {
            defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName;
        }

        // Get all apps that can handle VIEW intents.
        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        Set<String> resolvedActivityPackageList = new HashSet<>();
        for (ResolveInfo info : resolvedActivityList) {
            resolvedActivityPackageList.add(info.activityInfo.packageName);
        }

        // Get all apps that has services for custom tabs.
        Intent serviceIntent = new Intent(Intent.ACTION_MAIN);
        serviceIntent.addCategory(CATEGORY_CUSTOM_TABS);
        List<ResolveInfo> resolvedServiceList = pm.queryIntentServices(serviceIntent, 0);

        List<String> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedServiceList) {
            String servicePackageName = info.serviceInfo.packageName;
            if (resolvedActivityPackageList.contains(servicePackageName)) {
                packagesSupportingCustomTabs.add(servicePackageName);
            }
        }

        // Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
        // and service calls.
        String packageNameToUse;
        if (packagesSupportingCustomTabs.isEmpty()) return null;
        if (packagesSupportingCustomTabs.size() == 1) return packagesSupportingCustomTabs.get(0);
        if (!TextUtils.isEmpty(defaultViewHandlerPackageName)
                && !hasSpecializedHandlerIntents(activityIntent)
                && packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)) {
            return defaultViewHandlerPackageName;
        }
        if (packagesSupportingCustomTabs.contains(STABLE_PACKAGE)) return STABLE_PACKAGE;
        if (packagesSupportingCustomTabs.contains(BETA_PACKAGE)) return BETA_PACKAGE;
        if (packagesSupportingCustomTabs.contains(DEV_PACKAGE)) return DEV_PACKAGE;
        if (packagesSupportingCustomTabs.contains(LOCAL_PACKAGE)) return LOCAL_PACKAGE;
        return null;
    }

    /**
     * Used to check whether there is a specialized handler for a given intent.
     * @param intent The intent to check with.
     * @return Whether there is a specialized handler for the given intent.
     */
    private boolean hasSpecializedHandlerIntents(Intent intent) {
        try {
            PackageManager pm = mContext.getPackageManager();
            List<ResolveInfo> handlers = pm.queryIntentActivities(
                    intent,
                    PackageManager.GET_RESOLVED_FILTER);
            if (handlers == null || handlers.size() == 0) {
                return false;
            }
            for (ResolveInfo resolveInfo : handlers) {
                IntentFilter filter = resolveInfo.filter;
                if (filter == null) continue;
                if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue;
                if (resolveInfo.activityInfo == null) continue;
                return true;
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Runtime exception while getting specialized handlers");
        }
        return false;
    }
}
