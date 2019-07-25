// Copyright 2019 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package android.support.customtabs.trusted;

import static android.support.customtabs.TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY;
import static android.support.customtabs.testutil.TestUtil.getBrowserActivityWhenLaunched;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.CustomTabsSessionToken;
import android.support.customtabs.EnableComponentsTestRule;
import android.support.customtabs.TestActivity;
import android.support.customtabs.TestCustomTabsServiceSupportsTwas;
import android.support.customtabs.TrustedWebUtils;
import android.support.customtabs.TrustedWebUtils.SplashScreenParamKey;
import android.support.customtabs.testutil.CustomTabConnectionRule;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link TrustedWebActivityIntentBuilder}.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class TrustedWebActivityIntentBuilderTest {

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class
    );

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    @Rule
    public final CustomTabConnectionRule mConnectionRule = new CustomTabConnectionRule();

    private TestActivity mActivity;
    private CustomTabsSession mSession;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mSession = mConnectionRule.establishSessionBlocking(mActivity);
    }

    @Test
    public void intentIsConstructedCorrectly() {
        Uri url = Uri.parse("https://test.com/page");
        int toolbarColor = 0xffaabbcc;
        List<String> additionalTrustedOrigins =
                Arrays.asList("https://m.test.com", "https://test.org");

        Bundle splashScreenParams = new Bundle();
        int splashBgColor = 0x112233;
        splashScreenParams.putInt(SplashScreenParamKey.BACKGROUND_COLOR, splashBgColor);

        Intent intent =
                new TrustedWebActivityIntentBuilder(url)
                        .setToolbarColor(toolbarColor)
                        .setAdditionalTrustedOrigins(additionalTrustedOrigins)
                        .setSplashScreenParams(splashScreenParams).build(mSession);

        assertTrue(intent.getBooleanExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, false));
        assertTrue(CustomTabsSessionToken.getSessionTokenFromIntent(intent)
                .isAssociatedWith(mSession));
        assertEquals(url, intent.getData());
        assertEquals(toolbarColor, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
        assertEquals(additionalTrustedOrigins,
                intent.getStringArrayListExtra(TrustedWebUtils.EXTRA_ADDITIONAL_TRUSTED_ORIGINS));

        Bundle splashScreenParamsReceived =
                intent.getBundleExtra(TrustedWebUtils.EXTRA_SPLASH_SCREEN_PARAMS);

        // No need to test every splash screen param: they are sent in as-is in provided Bundle.
        assertEquals(splashBgColor,
                splashScreenParamsReceived.getInt(SplashScreenParamKey.BACKGROUND_COLOR));
    }
}
