package android.support.customtabs.trusted;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Saves the splash image to a file and transfers it to Custom Tabs provider.
 */
public class SplashImageTransferTask {

    private static final String FOLDER_NAME = "twa_splash";

    private static final String FILE_NAME = "splash_image.png";

    private final Context mContext;
    private final int mResourceId;
    private final String mAuthority;
    private final CustomTabsSession mSession;
    private final String mProviderPackage;

    @Nullable
    private Callback mCallback;

    /**
     * @param context {@link Context} to use.
     * @param resourceId Id of the Drawable to save into file.
     * @param authority {@link FileProvider} authority.
     * @param session {@link CustomTabsSession} to use for transferring the file.
     * @param providerPackage Package name of the Custom Tabs provider.
     */
    public SplashImageTransferTask(Context context, @DrawableRes int resourceId, String authority,
            CustomTabsSession session, String providerPackage) {
        mContext = context.getApplicationContext();
        mResourceId = resourceId;
        mAuthority = authority;
        mSession = session;
        mProviderPackage = providerPackage;
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
    private final AsyncTask<Void, Void, Boolean> mAsyncTask = new AsyncTask<Void, Void, Boolean>() {

        @Override
        protected Boolean doInBackground(Void... args) {
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
                // Already saved
                return transferToCustomTabsProvider(file);
            }
            try(OutputStream os = new FileOutputStream(file)) {
                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), mResourceId);

                if (isCancelled()) return false;
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();

                if (isCancelled()) return false;
                Uri uri = FileProvider.getUriForFile(mContext, mAuthority, file);
                mContext.grantUriPermission(mProviderPackage, uri, FLAG_GRANT_READ_URI_PERMISSION);
                mSession.receiveFile(uri, CustomTabsService.FILE_PURPOSE_TWA_SPLASH_IMAGE, null);
                return transferToCustomTabsProvider(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private boolean transferToCustomTabsProvider(File file) {
            return TrustedWebUtils.transferSplashImage(mContext, file, mAuthority, mProviderPackage,
                    mSession);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (mCallback != null && !isCancelled()) {
                mCallback.onFinished(success);
            }
        }
    };

    /** Callback to be called when the file is saved and transferred to Custom Tabs provider. */
    public interface Callback {
        void onFinished(boolean successfully);
    }
}
