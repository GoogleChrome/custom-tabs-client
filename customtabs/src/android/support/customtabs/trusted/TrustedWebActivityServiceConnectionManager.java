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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A TrustedWebActivityServiceConnectionManager will be used by a Trusted Web Activity provider and
 * takes care of connecting to and communicating with {@link TrustedWebActivityService}s.
 * <p>
 * Trusted Web Activity client apps are registered with the {@link #registerClient}, associating a
 * package with an origin. There may be multiple packages associated with a single origin.
 * Note, the origins are essentially keys to a map of origin to package name - while they
 * semantically are web origins, they aren't used that way.
 * <p>
 * To interact with a {@link TrustedWebActivityService}, call {@link #execute}.
 */
public class TrustedWebActivityServiceConnectionManager {
    private static final String TAG = "TWAConnectionManager";
    private static final String PREFS_FILE = "TrustedWebActivityVerifiedPackages";

    /**
     * A callback to be executed once a connection to a {@link TrustedWebActivityService} is open.
     */
    public interface ExecutionCallback {
        /**
         * Is run when a connection is open.
         * @param service A {@link TrustedWebActivityServiceWrapper} wrapping the connected
         *                {@link TrustedWebActivityService}.
         *                It may be null if the connection failed.
         * @throws RemoteException May be thrown by {@link TrustedWebActivityServiceWrapper}'s
         *                         methods.
         *                         If the user does not want to catch them, they will be caught
         *                         gracefully by {@link #execute}.
         */
        void onConnected(@Nullable TrustedWebActivityServiceWrapper service) throws RemoteException;
    }

    private interface WrappedCallback {
        void onConnected(@Nullable TrustedWebActivityServiceWrapper service);
    }

    /** Holds a connection to a TrustedWebActivityService. */
    private class Connection implements ServiceConnection {
        private TrustedWebActivityServiceWrapper mService;
        private List<WrappedCallback> mCallbacks = new LinkedList<>();
        private final Uri mScope;

        public Connection(Uri scope) {
            mScope = scope;
        }

        public TrustedWebActivityServiceWrapper getService() {
            return mService;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = new TrustedWebActivityServiceWrapper(
                    ITrustedWebActivityService.Stub.asInterface(iBinder), componentName);
            for (WrappedCallback callback : mCallbacks) {
                callback.onConnected(mService);
            }
            mCallbacks.clear();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mConnections.remove(mScope);
        }

        public void addCallback(WrappedCallback callback) {
            if (mService == null) {
                mCallbacks.add(callback);
            } else {
                callback.onConnected(mService);
            }
        }
    };

    private final Context mContext;
    /** Map from ServiceWorker scope to Connection. */
    private Map<Uri, Connection> mConnections = new HashMap<>();

    private static volatile SharedPreferences sSharedPreferences;
    /**
     * We use this Object as a lock for setting {@link #sSharedPreferences} in a thread safe way
     * using double-check locking [1]. Usually to make static, thread-safe singletons in Java we'd
     * use the initialization-on-demand holder idiom [2] which it a bit nicer. However, to create
     * |sSharedPreferences| we need a Context, and the initialization-on-demand holder idiom doesn't
     * allow for parameters.
     *
     * [1] - https://en.wikipedia.org/wiki/Double-checked_locking
     * [2] - https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
     */
    private static final Object sSharedPreferencesLock = new Object();

    /**
     * Gets the verified packages for the given origin. |origin| may be null, in which case this
     * method call will just trigger caching the Preferences.
     */
    private Set<String> getVerifiedPackages(Context context, String origin) {
        // Loading preferences is on the critical path for this class - we need to synchronously
        // inform the client whether or not an notification can be handled by a TWA.
        // I considered loading the preferences into a cache on a background thread when this class
        // was created, but ultimately if that load hadn't completed by the time {@link #execute} or
        // {@link #registerClient} were called, we'd still need to block for it to complete.
        // Therefore we attempt to asynchronously load the preferences in the constructor, but if
        // they aren't loaded by the time they are needed, we disable StrictMode and read them on
        // the main thread.
        StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();

        if (sSharedPreferences == null) {
            synchronized (sSharedPreferencesLock) {
                if (sSharedPreferences == null) {
                    sSharedPreferences =
                            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
                }
            }
        }

        Set<String> packages = origin == null ? null :
                sSharedPreferences.getStringSet(origin, new HashSet<String>());

        StrictMode.setThreadPolicy(policy);

        return packages;
    }

    /**
     * Creates a TrustedWebActivityServiceConnectionManager.
     * @param context A Context used for accessing SharedPreferences.
     */
    public TrustedWebActivityServiceConnectionManager(Context context) {
        mContext = context;

        // Asynchronously try to load (and therefore cache) the preferences.
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                getVerifiedPackages(mContext, null);
            }
        });
    }

    private WrappedCallback wrapCallback(final ExecutionCallback callback) {
        return new WrappedCallback() {
            @Override
            public void onConnected(@Nullable final TrustedWebActivityServiceWrapper service) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onConnected(service);
                        } catch (RemoteException | RuntimeException e) {
                            Log.w(TAG,
                                    "Exception while trying to use TrustedWebActivityService.", e);
                        }
                    }
                });
            }
        };
    }

    /**
     * Connects to the appropriate {@link TrustedWebActivityService} or uses an existing connection
     * if available and runs code once connected.
     * <pre>
     * To find a Service to connect to, this method attempts to resolve an
     * {@link Intent#ACTION_VIEW} Intent with the {@code scope} as data. The first of the resolved
     * packages that registered (through {@link #registerClient}) to {@code origin} will be chosen.
     * Finally, an Intent with the action {@link TrustedWebActivityService#INTENT_ACTION} will be
     * used to find the Service.
     *
     * @param scope The scope used in an Intent to find packages that may have a
     *              {@link TrustedWebActivityService}.
     * @param origin An origin that the {@link TrustedWebActivityService} package must be registered
     *               to.
     * @param callback A {@link ExecutionCallback} that will be run with a connection.
     *                 It will be run on a background thread from the ThreadPool as most methods
     *                 from {@link TrustedWebActivityServiceWrapper} require this.
     *                 Any {@link RemoteException} or {@link RuntimeException} exceptions thrown by
     *                 the callback will be swallowed.
     *                 This is to allow users to deal with exceptions thrown by
     *                 {@link TrustedWebActivityServiceWrapper} if they wish, but to fail
     *                 gracefully if they don't.
     * @return Whether a {@link TrustedWebActivityService} was found.
     */
    @SuppressLint("StaticFieldLeak")
    public boolean execute(final Uri scope, String origin, final ExecutionCallback callback) {
        final WrappedCallback wrappedCallback = wrapCallback(callback);

        // If we have an existing connection, use it.
        Connection connection = mConnections.get(scope);
        if (connection != null) {
            connection.addCallback(wrappedCallback);
            return true;
        }

        // Check that this is a notification we want to handle.
        final Intent bindServiceIntent = createServiceIntent(mContext, scope, origin);
        if (bindServiceIntent == null) return false;

        // Create a new connection.
        new AsyncTask<Void, Void, Connection>() {
            @Override
            protected Connection doInBackground(Void... voids) {
                Connection connection = new Connection(scope);
                connection.addCallback(wrappedCallback);

                try {
                    if (mContext.bindService(bindServiceIntent, connection,
                            Context.BIND_AUTO_CREATE)) {
                        return connection;
                    }

                    mContext.unbindService(connection);
                    return null;
                } catch (SecurityException e) {
                    Log.w(TAG, "SecurityException while binding.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Connection connection) {
                if (connection == null) {
                    wrappedCallback.onConnected(null);
                } else {
                    mConnections.put(scope, connection);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return true;
    }

    /**
     * Creates an Intent to launch the Service for the given scope and verified origin. Will
     * return null if there is no applicable Service.
     */
    private @Nullable Intent createServiceIntent(Context appContext, Uri scope, String origin) {
        Set<String> possiblePackages = getVerifiedPackages(appContext, origin);

        if (possiblePackages == null || possiblePackages.size() == 0) {
            return null;
        }

        // Get a list of installed packages that would match the scope.
        Intent scopeResolutionIntent = new Intent();
        scopeResolutionIntent.setData(scope);
        scopeResolutionIntent.setAction(Intent.ACTION_VIEW);
        // TODO(peconn): Do we want MATCH_ALL here.
        // TODO(peconn): Do we need a category here?
        List<ResolveInfo> candidateActivities = appContext.getPackageManager()
                .queryIntentActivities(scopeResolutionIntent, PackageManager.MATCH_DEFAULT_ONLY);

        // Choose the first of the installed packages that is verified.
        String resolvedPackage = null;
        for (ResolveInfo info : candidateActivities) {
            String packageName = info.activityInfo.packageName;

            if (possiblePackages.contains(packageName)) {
                resolvedPackage = packageName;
                break;
            }
        }

        if (resolvedPackage == null) {
            Log.w(TAG, "No TWA candidates for " + origin + " have been registered.");
            return null;
        }

        // Find the TrustedWebActivityService within that package.
        Intent serviceResolutionIntent = new Intent();
        serviceResolutionIntent.setPackage(resolvedPackage);
        serviceResolutionIntent.setAction(TrustedWebActivityService.INTENT_ACTION);
        ResolveInfo info = appContext.getPackageManager().resolveService(serviceResolutionIntent,
                PackageManager.MATCH_ALL);

        if (info == null) {
            Log.w(TAG, "Could not find TWAService for " + resolvedPackage);
            return null;
        }

        Intent finalIntent = new Intent();
        finalIntent.setComponent(new ComponentName(resolvedPackage, info.serviceInfo.name));
        return finalIntent;
    }

    /**
     * Registers (and persists) a package to be used for an origin. This information is persisted
     * in SharedPreferences.
     * @param origin The origin for which the package is relevant.
     * @param clientPackage The packages to register.
     */
    public void registerClient(final String origin, final String clientPackage) {
        Set<String> possiblePackages = getVerifiedPackages(mContext, origin);
        possiblePackages.add(clientPackage);

        // sSharedPreferences won't be null after a call to getVerifiedPackages.
        SharedPreferences.Editor editor = sSharedPreferences.edit();
        editor.putStringSet(origin, possiblePackages);
        editor.apply();
    }

    // TODO(peconn): Do we want to be able to unregister a client? To wipe all clients?
}
