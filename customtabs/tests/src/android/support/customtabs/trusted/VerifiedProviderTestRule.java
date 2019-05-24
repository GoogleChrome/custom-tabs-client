package android.support.customtabs.trusted;

import android.content.Context;
import android.support.customtabs.PollingCheck;
import android.support.test.InstrumentationRegistry;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sets the instrumentation context as a verified Trusted Web Activity Provider, meaning that the
 * TrustedWebActivityService will accept calls from it.
 */
public class VerifiedProviderTestRule extends TestWatcher {
    @Override
    protected void starting(Description description) {
        set(true);
    }

    @Override
    protected void finished(Description description) {
        set(false);
    }

    /**
     * Manually disables verification, causing TrustedWebActivityService calls to throw an
     * exception.
     */
    public void manuallyDisable() {
        set(false);
    }

    private void set(boolean enabled) {
        Context context = InstrumentationRegistry.getContext();
        TrustedWebActivityService.setVerifiedProviderSynchronouslyForTesting(
                context, enabled ? context.getPackageName() : null);
    }
}
