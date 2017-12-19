package android.support.customtabs.browseractions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
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
import java.util.List;
import java.util.Map;

/**
 * The class to pass images asynchronously between different applications.
 */
public class BrowserServiceFileProvider extends FileProvider {
    private static final String TAG = "BrowserServiceFileProvider";
    private static final String AUTHORITY_SUFFIX = ".fileprovider";
    private static final String CONTENT_SCHEME = "content";
    private static final String FILE_SUB_DIR = "images";
    private static final String URI_PREFIX = "BrowserService_";
    private static final String FILE_EXTENSION = ".png";
    private static final Map<Uri, Uri> URI_MAP = new HashMap<>();
    private static final Map<Uri, List<ResolveInfo>> RESOLVE_INFO_MAP = new HashMap<>();
    private static final Map<Uri, Object> URI_LOCK_MAP = new HashMap<>();

    private static class FileSaveTask extends AsyncTask<String, Void, Uri> {
        private final Context mContext;
        private final String mFilename;
        private final Bitmap mBitmap;
        private final Uri mOrigialUri;

        public FileSaveTask(Context context, String filename, Bitmap bitmap, Uri originalUri) {
            super();
            mContext = context;
            mFilename = filename;
            mBitmap = bitmap;
            mOrigialUri = originalUri;
        }

        @Override
        protected Uri doInBackground(String... params) {
            File savedFile = getFile();
            if (savedFile == null) return null;
            return FileProvider.getUriForFile(
                    mContext, mContext.getPackageName() + AUTHORITY_SUFFIX, savedFile);
        }

        @Override
        protected void onPostExecute(Uri fileUri) {
            // Fail to save the file, remove the uri mapping to indicate cannot get the file.
            if (fileUri == null) {
                URI_MAP.remove(mOrigialUri);
            } else {
                for (ResolveInfo resolveInfo : RESOLVE_INFO_MAP.get(mOrigialUri)) {
                    mContext.grantUriPermission(resolveInfo.activityInfo.packageName, fileUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                URI_MAP.put(mOrigialUri, fileUri);
            }
            RESOLVE_INFO_MAP.remove(mOrigialUri);

            // File is pending for access, notify to read the fileUri.
            if (URI_LOCK_MAP.containsKey(mOrigialUri)) {
                Object lock = URI_LOCK_MAP.get(mOrigialUri);
                synchronized (lock) {
                    lock.notify();
                }
            }
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

        private File getFile() {
            File path = new File(mContext.getFilesDir(), FILE_SUB_DIR);
            if (!path.exists()) path.mkdir();
            File img = new File(path, mFilename + FILE_EXTENSION);
            if (!img.exists() && !saveFile(img)) return null;
            return img;
        }
    }

    /**
     * Request a {@link Uri} used to access the bitmap through the file provider.
     * @param context The {@link Context} used to generate the uri, save the bitmap and grant the
     * read permission.
     * @param bitmap The {@link Bitmap} to be saved and access through the file provider.
     * @param name The name of the bitmap.
     * @param version The version number of the bitmap. Note: This plus the name decides the
     * filename of the bitmap. If it matches with existing file, bitmap will skip saving.
     * @param resolveInfos List of {@link ResolveInfo} granted the read permission of the bitmap.
     * @return The uri to access the bitmap.
     */
    public static Uri generateUri(Context context, Bitmap bitmap, String name, int version,
            List<ResolveInfo> resolveInfos) {
        Uri uri = generateUri(context);
        for (ResolveInfo resolveInfo : resolveInfos) {
            context.grantUriPermission(resolveInfo.activityInfo.packageName, uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        URI_MAP.put(uri, null);
        RESOLVE_INFO_MAP.put(uri, resolveInfos);
        String filename = name + "_" + Integer.toString(version);
        FileSaveTask fileSaveTask = new FileSaveTask(context, filename, bitmap, uri);
        fileSaveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return uri;
    }

    private static Uri generateUri(Context context) {
        String fileName = URI_PREFIX + String.valueOf(System.nanoTime());
        return new Uri.Builder()
                .scheme(CONTENT_SCHEME)
                .authority(context.getPackageName() + AUTHORITY_SUFFIX)
                .path(fileName)
                .build();
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Uri fileUri = getFileUri(uri);
        return fileUri != null ? super.openFile(fileUri, mode) : null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Uri fileUri = getFileUri(uri);
        return fileUri != null
                ? super.query(fileUri, projection, selection, selectionArgs, sortOrder)
                : null;
    }

    private Uri getFileUri(Uri originalUri) {
        // Fail saving the file.
        if (!URI_MAP.containsKey(originalUri)) return null;

        Uri fileUri = URI_MAP.get(originalUri);
        if (fileUri == null) {
            Object lock = new Object();
            URI_LOCK_MAP.put(originalUri, lock);
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupt waiting for file: :" + originalUri.toString());
                }
            }
            URI_LOCK_MAP.remove(originalUri);

            // File might be fail to save so the mapping entity is removed.
            if (URI_MAP.containsKey(originalUri)) {
                fileUri = URI_MAP.get(originalUri);
            }
            URI_MAP.remove(originalUri);
        }
        return fileUri;
    }
}
