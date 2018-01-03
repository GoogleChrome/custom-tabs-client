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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class to pass images asynchronously between different applications.
 */
public class BrowserServiceFileProvider extends FileProvider {
    private static final String TAG = "BrowserServiceFileProvider";
    private static final String AUTHORITY_SUFFIX = ".fileprovider";
    private static final String CONTENT_SCHEME = "content";
    private static final String FILE_SUB_DIR = "images";
    private static final String FILE_SUB_DIR_NAME = "myimages/";
    private static final String FILE_EXTENSION = ".png";
    private static final Set<Uri> FILES_IN_SERIALIZATION = new HashSet<>();
    private static final Map<Uri, Object> URI_LOCK_MAP = new HashMap<>();

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
            if (URI_LOCK_MAP.containsKey(mFileUri)) {
                Object lock = URI_LOCK_MAP.get(mFileUri);
                synchronized (lock) {
                    lock.notify();
                }
            }
            FILES_IN_SERIALIZATION.remove(mFileUri);
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

        private File saveFileIfNeeded() {
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
        String filename = name + "_" + Integer.toString(version);
        Uri uri = generateUri(context, filename);
        for (ResolveInfo resolveInfo : resolveInfos) {
            context.grantUriPermission(resolveInfo.activityInfo.packageName, uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        FILES_IN_SERIALIZATION.add(uri);
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
        Uri fileUri = getFileUri(uri);
        return super.openFile(fileUri, mode);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Uri fileUri = getFileUri(uri);
        if (fileUri == null) return null;
        return super.query(fileUri, projection, selection, selectionArgs, sortOrder);
    }

    private Uri getFileUri(Uri fileUri) {
        if (FILES_IN_SERIALIZATION.contains(fileUri)) {
            Object lock = new Object();
            URI_LOCK_MAP.put(fileUri, lock);
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupt waiting for file: :" + fileUri.toString());
                }
            }
            URI_LOCK_MAP.remove(fileUri);
        }
        return fileUri;
    }
}
