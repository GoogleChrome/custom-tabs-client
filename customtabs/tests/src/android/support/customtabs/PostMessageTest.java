/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.customtabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;


/**
 * Tests for a complete loop between a browser side {@link CustomTabsService}
 * and a client side {@link PostMessageService}. Both services are bound to through
 * {@link ServiceTestRule}, but {@link CustomTabsCallback#extraCallback} is used to link browser
 * side actions.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PostMessageTest {
    private TestCustomTabsCallback mCallback;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private PostMessageServiceConnection mPostMessageServiceConnection;
    private CustomTabsSession mSession;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getContext();
        String packageName = context.getPackageName();

        // Bind to PostMessageService only after CustomTabsService sends the callback to do so. This
        // callback is sent after requestPostMessageChannel is called.
        mCallback = new TestCustomTabsCallback() {
            @Override
            public void extraCallback(String callbackName, Bundle args) {
                if (!TestCustomTabsService.CALLBACK_BIND_TO_POST_MESSAGE.equals(callbackName)) {
                    return;
                }

                mPostMessageServiceConnection.bindSessionToPostMessageService(context, packageName);
            }
        };

        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                mSession = client.newSession(mCallback);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mSession = null;
            }
        };

        CustomTabsSessionToken token = new CustomTabsSessionToken(mCallback.getStub(), null);
        mPostMessageServiceConnection = new PostMessageServiceConnection(token);

        Intent intent = new Intent();
        intent.setClassName(packageName, TestCustomTabsService.class.getName());
        context.bindService(intent, mCustomTabsServiceConnection, Context.BIND_AUTO_CREATE);
        PollingCheck.waitFor(() -> mSession != null);
    }

    @Test
    public void testCustomTabsConnection() {
        assertTrue(mSession.requestPostMessageChannel(Uri.EMPTY));
        assertEquals(CustomTabsService.RESULT_SUCCESS, mSession.postMessage("", null));
        PollingCheck.waitFor(() -> mPostMessageServiceConnection.isBoundToService());
    }
}
