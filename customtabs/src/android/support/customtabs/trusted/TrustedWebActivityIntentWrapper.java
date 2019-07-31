// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.support.customtabs.trusted;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import java.util.List;

/**
 * Holds an {@link Intent} and other parameters necessary to start a Trusted Web Activity.
 */
public class TrustedWebActivityIntentWrapper {
    @NonNull
    private final Intent mIntent;
    @Nullable
    private final List<Uri> mSharedFileUris;

    TrustedWebActivityIntentWrapper(@NonNull Intent intent,
            @Nullable List<Uri> sharedFileUris) {
        mIntent = intent;
        mSharedFileUris = sharedFileUris;
    }

    /**
     * Launches a Trusted Web Activity.
     */
    public void launchTrustedWebActivity(Context context, String providerPackage) {
        grantUriPermissionToProvider(providerPackage, context);
        ContextCompat.startActivity(context, mIntent, null);
    }

    private void grantUriPermissionToProvider(String providerPackage, Context context) {
        if (mSharedFileUris == null) {
            return;
        }
        for (Uri uri : mSharedFileUris) {
            context.grantUriPermission(providerPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    /**
     * Returns the {@link Intent} to be launched.
     */
    public Intent getIntent() {
        return mIntent;
    }
}
