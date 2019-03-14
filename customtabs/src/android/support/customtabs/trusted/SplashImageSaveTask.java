package android.support.customtabs.trusted;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Saves the splash image to a file available via the FileProvider.
 */
public class SplashImageSaveTask {

    private static final String FOLDER_NAME = "twa_splash";

    private static final String FILE_NAME = "splash_image.png";

    private final Context mContext;
    private final int mResourceId;
    private final String mAuthority;

    @Nullable
    private Callback mCallback;

    /**
     * @param context Context to use.
     * @param resourceId Id of the Drawable to save into file
     * @param authority FileProvider authority.
     */
    public SplashImageSaveTask(Context context, @DrawableRes int resourceId, String authority) {
        mContext = context.getApplicationContext();
        mResourceId = resourceId;
        mAuthority = authority;
    }

    /**
     * Executes the task. Should be called only once.
     * @param callback {@link Callback} to be called when done.
     */
    public void execute(Callback callback) {
        assert mAsyncTask.getStatus() == AsyncTask.Status.PENDING;
        mCallback = callback;
        mAsyncTask.execute();
    }

    /**
     * Cancels the execution. The callback passed into {@link #execute} won't be called, and
     * the references to it will be released.
     */
    public void cancel() {
        mAsyncTask.cancel(true);
        mCallback = null;
    }

    @SuppressLint("StaticFieldLeak") // No leaking should happen
    private final AsyncTask<Void, Void, Uri> mAsyncTask = new AsyncTask<Void, Void, Uri>() {

        @Override
        protected Uri doInBackground(Void... args) {
            if (isCancelled()) return null;
            File dir = new File(mContext.getFilesDir(), FOLDER_NAME);
            if (!dir.exists()) {
                boolean mkDirSuccessful = dir.mkdir();
                if (!mkDirSuccessful) {
                    throw new RuntimeException("Failed to make a directory for splash screens");
                }
            }
            File file = new File(dir, FILE_NAME);
            if (file.exists()) {
                return FileProvider.getUriForFile(mContext, mAuthority, file); // Already saved
            }
            try(OutputStream os = new FileOutputStream(file)) {
                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), mResourceId);
                if (isCancelled()) return null;
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                return FileProvider.getUriForFile(mContext, mAuthority, file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(@Nullable Uri uri) {
            if (mCallback != null && !isCancelled()) {
                assert uri != null;
                mCallback.onSaved(uri);
            }
        }
    };

    /** Callback to be called when the file is saved. */
    public interface Callback {
        void onSaved(Uri uri);
    }
}
