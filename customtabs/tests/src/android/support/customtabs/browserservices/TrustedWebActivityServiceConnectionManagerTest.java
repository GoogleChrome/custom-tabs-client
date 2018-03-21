package android.support.customtabs.browserservices;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.customtabs.PollingCheck;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TrustedWebActivityServiceConnectionManagerTest {
    private TrustedWebActivityServiceConnectionManager mManager;
    private Context mContext;

    private boolean mConnected;

    private static final String ORIGIN = "https://localhost:3080";
    private static final Uri GOOD_SCOPE = Uri.parse("https://www.example.com/notifications");
    private static final Uri BAD_SCOPE = Uri.parse("https://www.notexample.com");

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext();
        mManager = new TrustedWebActivityServiceConnectionManager(mContext);

        mManager.registerClient(ORIGIN, mContext.getPackageName());

        TrustedWebActivityService.setVerifiedProvider(mContext, mContext.getPackageName());
    }

    @After
    public void tearDown() {
        TrustedWebActivityService.setVerifiedProvider(mContext, null);
    }

    @Test
    public void testConnection() {
        boolean delegated = mManager.execute(mContext, GOOD_SCOPE, ORIGIN,
                new TrustedWebActivityServiceConnectionManager.ExecutionCallback() {
                    @Override
                    public void onConnected(@Nullable TrustedWebActivityServiceWrapper service)
                            throws RemoteException {
                        service.getSmallIcon();
                        mConnected = true;
                    }
                });
        Assert.assertTrue(delegated);

        PollingCheck.waitFor(500, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mConnected;
            }
        });
    }

    @Test
    public void testNoService() {
        boolean delegated = mManager.execute(mContext, BAD_SCOPE, ORIGIN,
                new TrustedWebActivityServiceConnectionManager.ExecutionCallback() {
                    @Override
                    public void onConnected(@Nullable TrustedWebActivityServiceWrapper service)
                            throws RemoteException {}});
        Assert.assertFalse(delegated);
    }
}
