package android.support.customtabs.browseractions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import java.util.concurrent.TimeUnit;

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
    private static Object sFileCleanupLock = new Object();

    private static final String LAST_CLEANUP_TIME_KEY = "last_cleanup_time";

    private static class FileCleanupTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;

        public FileCleanupTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences pref = mContext.getSharedPreferences(mContext.getPackageName() + AUTHORITY_SUFFIX, Context.MODE_PRIVATE);
            if (shouldCleanupFile(pref)) {
                synchronized (sFileCleanupLock) {
                    File path = new File(mContext.getFilesDir(), FILE_SUB_DIR);
                    if (!path.exists()) return null;
                    File[] files = path.listFiles();
                    long retentionDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
                    for(File file : files) {
                        String extension = getExtension(file);
                        if (extension == null || !extension.equals(FILE_EXTENSION)) continue;
                        long lastModified = file.lastModified();
                        if (lastModified < retentionDate)
                            file.delete();
                    }
                    Editor editor = pref.edit();
                    editor.putLong(LAST_CLEANUP_TIME_KEY, System.currentTimeMillis());
                    editor.apply();
                }
            }
            return null;
        }

        private String getExtension(File file) {
            String filename = file.getName();
            if (!filename.contains(".")) return null;
            return filename.substring(filename.lastIndexOf('.'));
        }

        private boolean shouldCleanupFile(SharedPreferences pref) {
            long lastCleanup = pref.getLong(LAST_CLEANUP_TIME_KEY, 0l);
            long oneWeekMillis = TimeUnit.DAYS.toMillis(7);
            return System.currentTimeMillis() > lastCleanup + oneWeekMillis;
        }
    }

    private static class FileSaveTask extends AsyncTask<String, Void, Uri> {
        private final Context mContext;
        private final String mFilename;
        private final Bitmap mBitmap;
        private final Uri mOriginalUri;

        public FileSaveTask(Context context, String filename, Bitmap bitmap, Uri originalUri) {
            super();
            mContext = context;
            mFilename = filename;
            mBitmap = bitmap;
            mOriginalUri = originalUri;
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
                URI_MAP.remove(mOriginalUri);
            } else {
                for (ResolveInfo resolveInfo : RESOLVE_INFO_MAP.get(mOriginalUri)) {
                    mContext.grantUriPermission(resolveInfo.activityInfo.packageName, fileUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                URI_MAP.put(mOriginalUri, fileUri);
            }
            RESOLVE_INFO_MAP.remove(mOriginalUri);

            // File is pending for access, notify to read the fileUri.
            if (URI_LOCK_MAP.containsKey(mOriginalUri)) {
                Object lock = URI_LOCK_MAP.get(mOriginalUri);
                synchronized (lock) {
                    lock.notify();
                }
            }
            new FileCleanupTask(mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            File img;
            synchronized (sFileCleanupLock) {
                if (!path.exists()) path.mkdir();
                img = new File(path, mFilename + FILE_EXTENSION);
                if (!img.exists() && !saveFile(img)) return null;
                img.setLastModified(System.currentTimeMillis());
            }
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
        if (fileUri == null) return null;
        return super.openFile(fileUri, mode);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Uri fileUri = getFileUri(uri);
        if (fileUri == null) return null;
        return super.query(fileUri, projection, selection, selectionArgs, sortOrder);
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
