package android.support.customtabs.trusted;

import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A fake Browser that accepts browsable Intents.
 */
public class TestBrowser extends AppCompatActivity {

    private final CountDownLatch mResumeLatch = new CountDownLatch(1);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumeLatch.countDown();
    }

    /**
     * Waits until onResume. Returns whether has reached onResume until timeout.
     * If already resumed, returns "true" immediately.
     */
    public boolean waitForResume(int timeoutMillis) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread() : "Deadlock!";
        try {
            return mResumeLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
