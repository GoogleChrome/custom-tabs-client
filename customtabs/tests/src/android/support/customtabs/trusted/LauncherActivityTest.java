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

import android.app.Instrumentation;
import android.content.Context;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.EnableComponentsTestRule;
import android.support.customtabs.R;
import android.support.customtabs.TestCustomTabsService;
import android.support.customtabs.TestCustomTabsServiceSupportsTwas;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
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

    private Context mContext = InstrumentationRegistry.getContext();

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            LauncherActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class,
            TestCustomTabsService.class
    );
    @Rule
    public final ActivityTestRule<LauncherActivity> mActivityTestRule =
            new ActivityTestRule<>(LauncherActivity.class, false, false);

    @Before
    public void setUp() {
        TwaProviderPicker.restrictToPackageForTesting(mContext.getPackageName());
    }

    @After
    public void tearDown() {
        TwaProviderPicker.restrictToPackageForTesting(null);
    }

    @Test
    public void launchesTwa() {
        launch();
    }

    @Test
    public void readsUrlFromManifest() {
        TestBrowser browser = launch();

        assertEquals(DEFAULT_URL, browser.getLaunchIntent().getData());
    }

    @Test
    public void readsStatusBarColorFromManifest() {
        TestBrowser browser = launch();
        checkColor(browser);
    }

    @Test
    public void fallsBackToCustomTab() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TestBrowser browser = launch();

        assertFalse(browser.getLaunchIntent().hasExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY));
    }

    @Test
    public void customTabHasStatusBarColor() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TestBrowser browser = launch();

        checkColor(browser);
    }

    private void checkColor(TestBrowser browser) {
        int requestedColor = browser.getLaunchIntent()
                .getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0);
        int expectedColor = InstrumentationRegistry.getTargetContext().getResources()
                .getColor(STATUS_BAR_COLOR_ID);

        assertEquals(expectedColor, requestedColor);
    }

    private TestBrowser launch() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor
                = instrumentation.addMonitor(TestBrowser.class.getName(), null, false);

        mActivityTestRule.launchActivity(null);

        TestBrowser browserActivity = (TestBrowser) instrumentation.waitForMonitor(monitor);
        instrumentation.removeMonitor(monitor);
        return browserActivity;
    }
}
