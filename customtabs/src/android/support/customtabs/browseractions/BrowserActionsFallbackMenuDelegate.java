// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package android.support.customtabs.browseractions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * A delegate responsible for taking actions for Browser Actions predefined fallback context menu
 * item.
 */
public class BrowserActionsFallbackMenuDelegate {
    private final Context mContext;
    private final Uri mUri;

    /**
     * Builds a {@link BrowserActionsFallbackMenuDelegate} instance.
     * @param context The context displays the context menu.
     * @param uri The {@link Uri} of the link displayed on the context menu.
     */
    public BrowserActionsFallbackMenuDelegate(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    /**
     * Called when users choose to copy the link url.
     */
    public void onSaveToClipboard() {
        ClipboardManager clipboardManager =
                (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("url", mUri.toString());
        clipboardManager.setPrimaryClip(data);
        String toastMsg = mContext.getString(R.string.copy_toast_msg);
        Toast.makeText(mContext, toastMsg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Called when users choose to open the link in a browser.
     */
    public void onOpenInBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW, mUri);
        ContextCompat.startActivity(mContext, intent, null);
    }

    /**
     * Called when users choose to share the link.
     */
    public void onLinkShared() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, mUri.toString());
        intent.setType("text/plain");
        ContextCompat.startActivity(mContext, intent, null);
    }
}
