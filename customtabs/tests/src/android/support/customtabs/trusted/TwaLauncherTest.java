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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSessionToken;
import android.support.customtabs.EnableComponentsTestRule;
import android.support.customtabs.TestActivity;
import android.support.customtabs.TestCustomTabsService;
import android.support.customtabs.TestCustomTabsServiceSupportsTwas;
import android.support.customtabs.trusted.splashscreens.SplashScreenStrategy;
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
 * Instrumentation tests for {@link TwaLauncher}
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class TwaLauncherTest {

    private static final Uri URL = Uri.parse("https://www.test.com/default_url/");

    private Context mContext = InstrumentationRegistry.getContext();

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class,
            TestCustomTabsService.class
    );
    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    private TestActivity mActivity;

    private TwaLauncher mTwaLauncher;

    @Before
    public void setUp() {
        TwaProviderPicker.restrictToPackageForTesting(mContext.getPackageName());
        mActivity = mActivityTestRule.getActivity();
        mTwaLauncher = new TwaLauncher(mActivity);
    }

    @After
    public void tearDown() {
        TwaProviderPicker.restrictToPackageForTesting(null);
        mTwaLauncher.destroy();
    }

    @Test
    public void launchesTwaWithJustUrl() {
        Runnable launchRunnable = () -> mTwaLauncher.launch(URL);
        TestBrowser browser = getBrowserActivityWhenLaunched(launchRunnable);
        assertTrue(browser.getIntent().getBooleanExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY,
                false));
        assertEquals(URL, browser.getIntent().getData());
    }

    @Test
    public void transfersTwaBuilderParams() {
        // Checking just one parameters. TrustedWebActivityBuilderTest tests the rest. Here we just
        // check that TwaLauncher doesn't ignore the passed builder.
        TrustedWebActivityIntentBuilder builder = makeBuilder().setToolbarColor(0xff0000ff);
        Runnable launchRunnable = () -> mTwaLauncher.launch(builder, null, null);
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();
        assertEquals(0xff0000ff, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void fallsBackToCustomTab() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TwaLauncher launcher = new TwaLauncher(mActivity);

        Runnable launchRunnable = () -> launcher.launch(URL);
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();

        launcher.destroy();
        assertFalse(intent.hasExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY));
    }

    @Test
    public void customTabFallbackUsesToolbarColor() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TwaLauncher launcher = new TwaLauncher(mActivity);

        TrustedWebActivityIntentBuilder builder = makeBuilder().setToolbarColor(0xff0000ff);
        Runnable launchRunnable = () -> launcher.launch(builder, null, null);
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();

        launcher.destroy();
        assertEquals(0xff0000ff, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void reusesSessionForSubsequentLaunches() {
        TwaLauncher launcher1 = new TwaLauncher(mActivity);
        CustomTabsSessionToken token1 =
                getSessionTokenFromLaunchedBrowser(() -> launcher1.launch(URL));
        launcher1.destroy();

        // New activity is created (e.g. by an external VIEW intent).
        TwaLauncher launcher2 = new TwaLauncher(mActivity);
        CustomTabsSessionToken token2 =
                getSessionTokenFromLaunchedBrowser(() -> launcher2.launch(URL));
        launcher2.destroy();

        assertEquals(token1, token2);
    }

    @Test
    public void createsDifferentSessions_IfDifferentIdsSpecified() {
        int sessionId1 = 1;
        int sessionId2 = 2;

        TwaLauncher launcher1 = new TwaLauncher(mActivity, null, sessionId1);
        CustomTabsSessionToken token1 =
                getSessionTokenFromLaunchedBrowser(() -> launcher1.launch(URL));
        launcher1.destroy();

        // New activity is created (e.g. by an external VIEW intent).
        TwaLauncher launcher2 = new TwaLauncher(mActivity, null, sessionId2);
        CustomTabsSessionToken token2 =
                getSessionTokenFromLaunchedBrowser(() -> launcher2.launch(URL));
        launcher2.destroy();

        assertNotEquals(token1, token2);
    }

    @Test
    public void completionCallbackCalled() {
        Runnable callback = mock(Runnable.class);
        Runnable launchRunnable = () -> mTwaLauncher.launch(makeBuilder(), null, callback);
        getBrowserActivityWhenLaunched(launchRunnable);
        verify(callback).run();
    }

    @Test
    public void completionCallbackCalled_WhenFallingBackToCct() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TwaLauncher twaLauncher = new TwaLauncher(mActivity);

        Runnable callback = mock(Runnable.class);
        Runnable launchRunnable = () -> twaLauncher.launch(makeBuilder(), null, callback);
        getBrowserActivityWhenLaunched(launchRunnable);
        verify(callback).run();
        twaLauncher.destroy();
    }

    @Test
    public void notifiesSplashScreenStrategyOfLaunchInitiation() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);
        TrustedWebActivityIntentBuilder builder = makeBuilder();
        mTwaLauncher.launch(builder, strategy, null);
        verify(strategy).onTwaLaunchInitiated(
                eq(InstrumentationRegistry.getContext().getPackageName()),
                eq(builder));
    }

    @Test
    public void doesntLaunch_UntilSplashScreenStrategyFinishesConfiguring() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);

        // Using spy to verify build not called to avoid testing directly that activity is
        // not launched.
        TrustedWebActivityIntentBuilder builder = spy(makeBuilder());
        mTwaLauncher.launch(builder, strategy, null);
        verify(builder, never()).build(any());
    }

    @Test
    public void launches_WhenSplashScreenStrategyFinishesConfiguring() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(strategy).configureTwaBuilder(any(), any(), any());

        Runnable launchRunnable = () -> mTwaLauncher.launch(makeBuilder(), strategy, null);
        assertNotNull(getBrowserActivityWhenLaunched(launchRunnable));
    }

    private TrustedWebActivityIntentBuilder makeBuilder() {
        return new TrustedWebActivityIntentBuilder(URL);
    }


    private CustomTabsSessionToken getSessionTokenFromLaunchedBrowser(Runnable launchRunnable) {
        return CustomTabsSessionToken.getSessionTokenFromIntent(
                getBrowserActivityWhenLaunched(launchRunnable).getIntent());
    }
}
