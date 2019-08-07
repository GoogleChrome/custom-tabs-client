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
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.EnableComponentsTestRule;
import android.support.customtabs.TestCustomTabsService;
import android.support.customtabs.TestCustomTabsServiceSupportsTwas;
import android.support.customtabs.test.R;
import android.support.customtabs.testutil.TestUtil;
import android.support.customtabs.trusted.sharing.ShareData;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link LauncherActivity}.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class LauncherActivityTest {
    // The default URL specified in the test AndroidManifest.xml under LauncherActivity.
    private static final Uri DEFAULT_URL = Uri.parse("https://www.test.com/default_url/");
    // The resource id of the color specified as the status bar color.
    private static final int STATUS_BAR_COLOR_ID = R.color.status_bar_color;

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

        assertEquals(DEFAULT_URL, browser.getIntent().getData());
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

        assertFalse(browser.getIntent().hasExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY));
    }

    @Test
    public void customTabHasStatusBarColor() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TestBrowser browser = launch();

        checkColor(browser);
    }

    @Test
    public void sendsShareParamsFromSendIntent() {
        String subject = "foo";
        String text = "bar";
        Uri uri = Uri.parse("file://baz");

        Intent intent = new Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, text)
            .putExtra(Intent.EXTRA_STREAM, uri);

        TestBrowser browser = launch(intent);
        assertShareParamsInIntent(browser.getIntent(),
                new ShareData(subject, text, Collections.singletonList(uri)));
    }

    @Test
    public void sendsShareParamsFromSendMultipleIntent() {
        String subject = "foo";
        String text = "bar";
        List<Uri> uris = Arrays.asList(Uri.parse("file://baz"), Uri.parse("file://qux"));

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));

        TestBrowser browser = launch(intent);

        assertShareParamsInIntent(browser.getIntent(), new ShareData(subject, text, uris));
    }

    private void assertShareParamsInIntent(Intent intent, ShareData shareData) {
        ShareData data = ShareData.fromBundle(intent.getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA));
        assertEquals(shareData.title, data.title);
        assertEquals(shareData.text, data.text);
        assertEquals(shareData.uris, data.uris);
        String shareTarget =
                intent.getStringExtra(TrustedWebActivityIntentBuilder.EXTRA_SHARE_TARGET);
        assertEquals(mActivityTestRule.getActivity().getString(R.string.share_target), shareTarget);
    }

    private void checkColor(TestBrowser browser) {
        int requestedColor = browser.getIntent()
                .getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0);
        int expectedColor = InstrumentationRegistry.getTargetContext().getResources()
                .getColor(STATUS_BAR_COLOR_ID);

        assertEquals(expectedColor, requestedColor);
    }

    private TestBrowser launch() {
        return launch(null);
    }

    private TestBrowser launch(Intent intent) {
        return TestUtil.getBrowserActivityWhenLaunched(() ->
                mActivityTestRule.launchActivity(intent));
    }
}
