package android.support.customtabs.browserservices;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;

public class TrustedWebActivityHelper {
    private final Uri mOrigin;
    private final Context mContext;

    private CustomTabsClient mClient;
    private String mPackageName;
    private Callback mCallback;

    public TrustedWebActivityHelper(Context context, Uri origin) {
        mContext = context;
        mOrigin = origin;
    }

    public interface Callback {
        void onServiceConnected();
        void onServiceDisconnected();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void bindService() {
        // TODO(peconn): If already bound, call callback?

        // TODO(peconn): Allow specifying packages?
        mPackageName = CustomTabsClient.getPackageName(mContext, null);
        CustomTabsClient.bindCustomTabsService(mContext, mPackageName, mServiceConnection);
    }

    private CustomTabsServiceConnection mServiceConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            mClient = client;

            CustomTabsSession session = client.newSession(mVerificationCallback);
            session.validateRelationship(
                    CustomTabsService.RELATION_HANDLE_ALL_URLS, mOrigin, null);

            if (mCallback != null) mCallback.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mClient = null;

            if (mCallback != null) mCallback.onServiceDisconnected();
        }
    };

    private CustomTabsCallback mVerificationCallback = new CustomTabsCallback() {
        @Override
        public void onRelationshipValidationResult(int relation, Uri requestedOrigin,
                boolean result,
                Bundle extras) {
            if (!result) return;

            TrustedWebActivityService.setVerifiedProvider(mContext, mPackageName);
        }
    };

    public void launch(Uri uri, @Nullable CustomTabsCallback callback) {
        if (mClient == null) {
            throw new IllegalStateException("Service must be bound when launch is called.");
        }

        CustomTabsSession session = mClient.newSession(callback);

        // TODO(peconn): Find a way to allow users to add stuff to the builder.
        CustomTabsIntent intent = new CustomTabsIntent.Builder(session).build();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }

        intent.intent.setPackage(mPackageName);

        TrustedWebUtils.launchAsTrustedWebActivity(mContext, intent, uri);
    }

    public void unBindService() {
        mContext.unbindService(mServiceConnection);
    }
}
