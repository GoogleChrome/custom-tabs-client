/*
 * Copyright 2019 The Android Open Source Project
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

import static android.support.customtabs.TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.PollingCheck;
import android.support.customtabs.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link LauncherActivity}.
 *
 * TODO(pshmakov): Add tests for session resumption.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class LauncherActivityTest {
    // The default URL specified in the test AndroidManifest.xml under LauncherActivity.
    private static final Uri DEFAULT_URL = Uri.parse("https://www.test.com/default_url/");
    // The resource id of the color specified as the status bar color.
    // TODO(peconn): Create a specific test color in a test-only colors.xml
    private static final int STATUS_BAR_COLOR_ID = R.color.browser_actions_bg_grey;
    private static final int POLL_TIMEOUT = 3000;

    private Context mContext = InstrumentationRegistry.getContext();

    @Rule
    public final ActivityTestRule<LauncherActivity> mActivityTestRule =
            new ActivityTestRule<>(LauncherActivity.class, false, false);

    @Test
    public void launchesTwa() {
        launchAsTwa();
    }

    @Test
    public void readsUrlFromManifest() {
        launchAsTwa();

        assertEquals(DEFAULT_URL, TestBrowser.getLaunchIntent().getData());
    }

    @Test
    public void readsStatusBarColorFromManifest() {
        launchAsTwa();

        int requestedColor = TestBrowser.getLaunchIntent()
                .getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0);
        int expectedColor = InstrumentationRegistry.getTargetContext().getResources()
                .getColor(STATUS_BAR_COLOR_ID);

        assertEquals(expectedColor, requestedColor);
    }

    @Test
    public void fallsBackToCustomTab() {
        launchAs(TwaProviderPicker.LaunchMode.CUSTOM_TAB);

        assertFalse(TestBrowser.getLaunchIntent().hasExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY));
    }

    private void launchAsTwa() {
        launchAs(TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY);
    }

    private void launchAs(@TwaProviderPicker.LaunchMode int launchMode) {
        TwaProviderPicker.setProviderForTesting(
                new TwaProviderPicker.Action(launchMode, mContext.getPackageName()));
        mActivityTestRule.launchActivity(null);
        PollingCheck.waitFor(POLL_TIMEOUT, TestBrowser::hasLaunched);
    }
}
