// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.support.customtabs.trusted.sharing;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains data to be delivered to a Web Share Target via a Trusted Web Activity.
 */
public final class ShareData {
    /** Bundle key for {@link #title} */
    public static final String KEY_TITLE = "androidx.browser.trusted.SHARE_TITLE";

    /** Bundle key for {@link #text} */
    public static final String KEY_TEXT = "androidx.browser.trusted.SHARE_TEXT";

    /** Bundle key for {@link #uris} */
    public static final String KEY_URIS = "androidx.browser.trusted.SHARE_URIS";

    /** Title of the shared message. */
    @Nullable
    public final String title;

    /** Text of the shared message. */
    @Nullable
    public final String text;

    /** URIs of files to be shared. */
    @Nullable
    public final List<Uri> uris;

    /** Constructor. */
    public ShareData(@Nullable String title, @Nullable String text, @Nullable List<Uri> uris) {
        this.title = title;
        this.text = text;
        this.uris = uris;
    }

    /** Packs the object into a {@link Bundle} */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_TEXT, text);
        if (uris != null) {
            bundle.putParcelableArrayList(KEY_URIS, new ArrayList<>(uris));
        }
        return bundle;
    }

    /** Unpacks the object from a {@link Bundle}. */
    @NonNull
    public static ShareData fromBundle(@NonNull Bundle bundle) {
        return new ShareData(bundle.getString(KEY_TITLE),
                bundle.getString(KEY_TEXT),
                bundle.getParcelableArrayList(KEY_URIS));
    }
}