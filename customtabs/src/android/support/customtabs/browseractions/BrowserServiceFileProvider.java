/*
 * Copyright 2018 The Android Open Source Project
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
package android.support.customtabs.browseractions;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class to pass images asynchronously between different applications.
 * Call {@link generateUri(Context, Bitmap, String, int, List<String>)} to save the image
 * and generate a uri to access the image.
 * To access the image, pass the uri to {@link BrowserServiceImageReadTask}.
 */
public class BrowserServiceFileProvider extends FileProvider {
    private static final String TAG = "BrowserServiceFileProvider";
    private static final String AUTHORITY_SUFFIX = ".image_provider";
    private static final String CONTENT_SCHEME = "content";
    private static final String FILE_SUB_DIR = "image_provider";
    private static final String FILE_SUB_DIR_NAME = "image_provider_images/";
    private static final String FILE_EXTENSION = ".png";

    private static Set<Uri> sFileInSerialization = new HashSet<>();
    private static Map<Uri, Object> sUriLockMap = new HashMap<>();

    private static class FileSaveTask extends AsyncTask<String, Void, Void> {
        private final Context mContext;
        private final String mFilename;
        private final Bitmap mBitmap;
        private final Uri mFileUri;

        public FileSaveTask(Context context, String filename, Bitmap bitmap, Uri fileUri) {
            super();
            mContext = context;
            mFilename = filename;
            mBitmap = bitmap;
            mFileUri = fileUri;
        }

        @Override
        protected Void doInBackground(String... params) {
            saveFileIfNeeded();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // If file is pending for access, notify to read the fileUri.
            if (sUriLockMap.containsKey(mFileUri)) {
                Object lock = sUriLockMap.get(mFileUri);
                synchronized (lock) {
                    lock.notify();
                }
            }
            sFileInSerialization.remove(mFileUri);
        }

        private boolean saveFile(File savedFile) {
            try {
                FileOutputStream fOut = new FileOutputStream(savedFile);
                boolean result = mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.close();
                return result;
            } catch (IOException e) {
                Log.e(TAG, "Fail to save file", e);
                return false;
            }
        }

        private void saveFileIfNeeded() {
            File path = new File(mContext.getFilesDir(), FILE_SUB_DIR);
            if (!path.exists()) path.mkdir();
            File img = new File(path, mFilename + FILE_EXTENSION);
            if (!img.exists())
                saveFile(img);
        }
    }

    /**
     * Request a {@link Uri} used to access the bitmap through the file provider.
     * @param context The {@link Context} used to generate the uri, save the bitmap and grant the
     *                read permission.
     * @param bitmap The {@link Bitmap} to be saved and access through the file provider.
     * @param name The name of the bitmap.
     * @param version The version number of the bitmap. Note: This plus the name decides the
     *                 filename of the bitmap. If it matches with existing file, bitmap will skip
     *                 saving.
     * @param packageNames List of package granted the read permission of the bitmap.
     * @return The uri to access the bitmap.
     */
    public static Uri generateUri(Context context, Bitmap bitmap, String name, int version,
            List<String> packageNames) {
        String filename = name + "_" + Integer.toString(version);
        Uri uri = generateUri(context, filename);
        for(String packageName : packageNames)
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sFileInSerialization.add(uri);
        FileSaveTask fileSaveTask = new FileSaveTask(context, filename, bitmap, uri);
        fileSaveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return uri;
    }

    private static Uri generateUri(Context context, String filename) {
        String fileName = FILE_SUB_DIR_NAME + filename + FILE_EXTENSION;
        return new Uri.Builder()
                .scheme(CONTENT_SCHEME)
                .authority(context.getPackageName() + AUTHORITY_SUFFIX)
                .path(fileName)
                .build();
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        blockUntilFileReady(uri);
        return super.openFile(uri, mode);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        blockUntilFileReady(uri);
        return super.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    private void blockUntilFileReady(Uri fileUri) {
        if (sFileInSerialization.contains(fileUri)) {
            Object lock = new Object();
            sUriLockMap.put(fileUri, lock);
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupt waiting for file: :" + fileUri.toString());
                } finally {
                    sUriLockMap.remove(fileUri);
                }
            }
        }
    }
}
