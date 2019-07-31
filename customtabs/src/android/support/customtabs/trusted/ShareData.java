// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.support.customtabs.trusted;

import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains data to be delivered to a Web Share Target via a Trusted Web Activity.
 */
public final class ShareData {
    public static final String KEY_TITLE = "androidx.browser.trusted.SHARE_TITLE";
    public static final String KEY_TEXT = "androidx.browser.trusted.SHARE_TEXT";
    public static final String KEY_URIS = "androidx.browser.trusted.SHARE_URIS";

    /** Title of the shared message. */
    public final String title;

    /** Text of the shared message. */
    public final String text;

    /** URIs of files to be shared */
    public final List<Uri> uris;

    /**
     * Constructor.
     * @param title Title of the shared message.
     * @param text Text of the shared message.
     * @param uris URIs of files to be shared.
     */
    public ShareData(String title, String text, List<Uri> uris) {
        this.title = title;
        this.text = text;
        this.uris = uris;
    }

    /** Packs the data into a {@link Bundle} */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_TEXT, text);
        if (uris != null) {
            bundle.putParcelableArrayList(KEY_URIS, new ArrayList<>(uris));
        }
        return bundle;
    }

    /** Unpacks the data from a {@link Bundle}. */
    public static ShareData fromBundle(Bundle bundle) {
        return new ShareData(bundle.getString(KEY_TITLE),
                bundle.getString(KEY_TEXT),
                bundle.getParcelableArrayList(KEY_URIS));
    }
}