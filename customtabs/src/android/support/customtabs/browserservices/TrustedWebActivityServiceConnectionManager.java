package android.support.customtabs.browserservices;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.NotifyNotificationBundle;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.CancelNotificationBundle;
import android.support.customtabs.browserservices.TrustedWebActivityServiceWrapper.ResultBundle;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO(peconn): Finish up javadoc.
public class TrustedWebActivityServiceConnectionManager {
    private static final String TAG = "TWAConnectionManager";

    private interface Callback {
        void onConnected(@Nullable ITrustedWebActivityService service);
    }

    private interface ExceptionCallback {
        void onConnected(@Nullable ITrustedWebActivityService service) throws RemoteException;
    }

    public interface CompletionCallback {
        void onCompletion(boolean success);
    }

    /** Holds a connection to a TrustedWebActivityService. */
    private class Connection implements ServiceConnection {
        private ITrustedWebActivityService mService;
        private List<Callback> mCallbacks = new LinkedList<>();
        private final Uri mScope;

        public Connection(Uri scope) {
            mScope = scope;
        }

        public ITrustedWebActivityService getService() {
            return mService;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ITrustedWebActivityService.Stub.asInterface(iBinder);
            for (Callback callback : mCallbacks) {
                callback.onConnected(mService);
            }
            mCallbacks.clear();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mConnections.remove(mScope);
        }

        public void addCallback(Callback callback) {
            if (mService == null) {
                mCallbacks.add(callback);
            } else {
                callback.onConnected(mService);
            }
        }
    };

    /** Map from ServiceWorker scope to Connection. */
    private Map<Uri, Connection> mConnections = new HashMap<>();

    /** Map from Origin to set of packages verified for that origin. */
    private Map<String, Set<String>> mVerifiedPackages = new HashMap<>();

    /**
     * Attempts to delegate displaying a notification to a Trusted Web Activity.
     * @param appContext The application Context (to communicate with Services).
     * @param scope The scope of the application, used to identify the appropriate TWA.
     * @param origin The origin that the TWA must be verified for.
     * @param platformTag A Tag for the notification.
     * @param platformId An ID for the notification.
     * @param builder A {@link Notification.Builder} to create the notification.
     * @param channelName The Android notification channel to display the notification.
     * @param completionCallback Will be called once the Client has either shown the notification or
     *        failed to do so. It may fail if it doesn't have notification permissions.
     * @return True if the notification could be delegated. It will be false if the user has no
     *         applicable TWA client app on their device.
     */
    public boolean notifyNotification(Context appContext, Uri scope, String origin,
            final String platformTag, final int platformId, final Notification.Builder builder,
            final String channelName, final CompletionCallback completionCallback) {
        return execute(appContext, scope, origin, wrapCallback(new ExceptionCallback() {
            @Override
            public void onConnected(@Nullable ITrustedWebActivityService service)
                    throws RemoteException {
                if (service == null) return;
                builder.setSmallIcon(service.getSmallIconId());

                Bundle args = new NotifyNotificationBundle(platformTag, platformId,
                        builder.build(), channelName).toBundle();
                ResultBundle result = new ResultBundle(service.notifyNotificationWithChannel(args));

                completionCallback.onCompletion(result.mSuccess);
            }
        }));
    }

    /**
     * Attempts to cancel a notification that has been delegated to a Trusted Web Activity. The
     * parameters and return type are the same as {@link #notifyNotification}.
     */
    public boolean cancelNotification(Context appContext, Uri scope, String origin,
            final String platformTag, final int platformId,
            final CompletionCallback completionCallback) {
        return execute(appContext, scope, origin, wrapCallback(new ExceptionCallback() {
            @Override
            public void onConnected(@Nullable ITrustedWebActivityService service)
                    throws RemoteException {
                if (service == null) return;

                Bundle args = new CancelNotificationBundle(platformTag, platformId).toBundle();
                service.cancelNotification(args);
                completionCallback.onCompletion(true);
            }
        }));
    }

    private Callback wrapCallback(final ExceptionCallback callback) {
        return new Callback() {
            @Override
            public void onConnected(@Nullable ITrustedWebActivityService service) {
                try {
                    callback.onConnected(service);
                } catch (RemoteException e) {
                    Log.w(TAG, "Exception while trying to use TrustedWebActivityService.", e);
                }
            }
        };
    }

    @SuppressLint("StaticFieldLeak")
    private boolean execute(final Context appContext, final Uri scope, String origin,
            final Callback callback) {
        // If we have an existing connection, use it.
        Connection connection = mConnections.get(scope);
        if (connection != null) {
            connection.addCallback(callback);
            return true;
        }

        // Check that this is a notification we want to handle.
        final Intent bindServiceIntent = createServiceIntent(appContext, scope, origin);
        if (bindServiceIntent == null) return false;

        // Create a new connection.
        new AsyncTask<Void, Void, Connection>() {
            @Override
            protected Connection doInBackground(Void... voids) {
                Connection connection = new Connection(scope);
                connection.addCallback(callback);

                try {
                    if (appContext.bindService(bindServiceIntent, connection,
                            Context.BIND_AUTO_CREATE)) {
                        return connection;
                    }

                    appContext.unbindService(connection);
                    return null;
                } catch (SecurityException e) {
                    Log.w(TAG, "SecurityException while binding.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Connection connection) {
                if (connection == null) {
                    callback.onConnected(null);
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
        Set possiblePackages = mVerifiedPackages.get(origin);
        if (possiblePackages == null || possiblePackages.size() == 0) return null;

        // Get a list of installed packages that would match the scope.
        Intent scopeResolutionIntent = new Intent();
        scopeResolutionIntent.setData(scope);
        // TODO(peconn): Ensure we want MATCH_ALL here.
        List<ResolveInfo> candidateActivities = appContext.getPackageManager()
                .queryIntentActivities(scopeResolutionIntent, PackageManager.MATCH_ALL);

        // Choose the first of the installed packages that is verified.
        String resolvedPackage = null;
        for (ResolveInfo info : candidateActivities) {
            String packageName = info.resolvePackageName;
            if (possiblePackages.contains(packageName)) {
                resolvedPackage = info.resolvePackageName;
            }
        }

        if (resolvedPackage == null) return null;

        // Find the TrustedWebActivityService within that package.
        Intent serviceResolutionIntent = new Intent();
        serviceResolutionIntent.setPackage(resolvedPackage);
        serviceResolutionIntent.setAction(TrustedWebActivityService.INTENT_ACTION);
        ResolveInfo info = appContext.getPackageManager().resolveService(serviceResolutionIntent,
                PackageManager.MATCH_ALL);

        if (info == null) return null;

        Intent finalIntent = new Intent();
        // TODO(peconn): Check this actually creates a valid component!
        finalIntent.setComponent(new ComponentName(resolvedPackage, info.serviceInfo.name));
        return finalIntent;
    }

    /** Registers (and persists) a package to be used for an origin. */
    public void registerClient(String origin, String clientPackage) {
        if (!mVerifiedPackages.containsKey(origin)) {
            mVerifiedPackages.put(origin, new HashSet<String>());
        }

        mVerifiedPackages.get(origin).add(clientPackage);
        // TODO(peconn): Persist.
    }
}
