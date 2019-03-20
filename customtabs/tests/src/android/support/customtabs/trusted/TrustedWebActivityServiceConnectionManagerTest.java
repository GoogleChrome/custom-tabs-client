/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.support.customtabs.trusted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import android.support.customtabs.PollingCheck;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

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

        TrustedWebActivityServiceConnectionManager
                .registerClient(mContext, ORIGIN, mContext.getPackageName());

        TrustedWebActivityService.setVerifiedProvider(mContext, mContext.getPackageName());
    }

    @After
    public void tearDown() {
        mManager.unbindAllConnections();

        TrustedWebActivityService.setVerifiedProvider(mContext, null);
    }

    @Test
    public void testConnection() {
        boolean delegated = mManager.execute(GOOD_SCOPE, ORIGIN,
                service -> {
                    assertEquals(TestTrustedWebActivityService.SMALL_ICON_ID,
                            service.getSmallIconId());
                    assertEquals(TestTrustedWebActivityService.NOTIFICATIONS,
                            service.getActiveNotifications());
                    mConnected = true;
                });
        assertTrue(delegated);

        PollingCheck.waitFor(() -> mConnected);
    }

    @Test
    public void testNoService() {
        boolean delegated = mManager.execute(BAD_SCOPE, ORIGIN, service -> {});
        assertFalse(delegated);
    }
}
