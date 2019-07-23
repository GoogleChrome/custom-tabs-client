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

package android.support.customtabs.trusted.splashscreens;

import static android.support.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT;
import static android.support.customtabs.CustomTabsService.CATEGORY_COLOR_SCHEME_CUSTOMIZATION;
import static android.support.customtabs.CustomTabsService.CATEGORY_NAVBAR_COLOR_CUSTOMIZATION;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.customtabs.CustomTabColorSchemeParams;
import android.support.customtabs.trusted.RobolectricUtils;
import android.support.customtabs.trusted.TrustedWebActivityIntentBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;

/**
 * Tests for {@link SystemBarColorPredictor}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class SystemBarColorPredictorTest {
    private static final String PACKAGE = "org.chromium.chrome";
    private final Context mContext = RuntimeEnvironment.application;
    private final TrustedWebActivityIntentBuilder mBuilder = new TrustedWebActivityIntentBuilder(
            Uri.EMPTY);
    private final SystemBarColorPredictor mPredictor = new SystemBarColorPredictor();

    @Test
    public void predictsDefaultStatusBarColor_WithoutColorSchemeParams() {
        installService();
        int color = 0xff000001;
        mBuilder.setToolbarColor(color);
        assertStatusBarColor(color);
    }

    @Test
    public void predictsDefaultStatusBarColor_IfColorSchemeParamsNotSupported() {
        installService();
        int defaultColor = 0xff000001;
        int schemeSpecificColor = 0xff000002;
        mBuilder.setToolbarColor(defaultColor).setColorSchemeParams(COLOR_SCHEME_LIGHT,
                new CustomTabColorSchemeParams.Builder().setToolbarColor(schemeSpecificColor)
                            .build());
        assertStatusBarColor(defaultColor);
    }

    @Test
    public void predictsSchemeSpecificStatusBarColor_IfSupported() {
        installService(CATEGORY_COLOR_SCHEME_CUSTOMIZATION);
        int defaultColor = 0xff000001;
        int schemeSpecificColor = 0xff000002;
        mBuilder.setToolbarColor(defaultColor).setColorSchemeParams(COLOR_SCHEME_LIGHT,
                new CustomTabColorSchemeParams.Builder().setToolbarColor(schemeSpecificColor)
                        .build());
        assertStatusBarColor(schemeSpecificColor);
    }

    @Test
    public void predictsWhiteNavBarColor_ForOldChrome() {
        installService();
        int color = 0xff000001;
        mBuilder.setNavigationBarColor(color);
        assertNavBarColor(Color.WHITE);
    }

    @Test
    public void predictsDefaultNavBarColor_WithoutColorSchemeParams() {
        installService(CATEGORY_NAVBAR_COLOR_CUSTOMIZATION);
        int color = 0xff000001;
        mBuilder.setNavigationBarColor(color);
        assertNavBarColor(color);
    }

    @Test
    public void predictsDefaultNavBarColor_IfColorSchemeParamsNotSupported() {
        installService(CATEGORY_NAVBAR_COLOR_CUSTOMIZATION);

        int defaultColor = 0xff000001;
        int schemeSpecificColor = 0xff000002;
        mBuilder.setNavigationBarColor(defaultColor).setColorSchemeParams(COLOR_SCHEME_LIGHT,
                new CustomTabColorSchemeParams.Builder().setNavigationBarColor(schemeSpecificColor)
                        .build());
        assertNavBarColor(defaultColor);
    }

    @Test
    public void predictsSchemeSpecificNavBarColor_IfSupported() {
        installService(CATEGORY_NAVBAR_COLOR_CUSTOMIZATION, CATEGORY_COLOR_SCHEME_CUSTOMIZATION);
        int defaultColor = 0xff000001;
        int schemeSpecificColor = 0xff000002;
        mBuilder.setNavigationBarColor(defaultColor).setColorSchemeParams(COLOR_SCHEME_LIGHT,
                new CustomTabColorSchemeParams.Builder().setNavigationBarColor(schemeSpecificColor)
                        .build());
        assertNavBarColor(schemeSpecificColor);
    }

    private void assertStatusBarColor(Integer color) {
        assertEquals(color, mPredictor.getExpectedStatusBarColor(mContext, PACKAGE,
                mBuilder));
    }

    private void assertNavBarColor(Integer color) {
        assertEquals(color, mPredictor.getExpectedNavbarColor(mContext, PACKAGE,
                mBuilder));
    }

    private void installService(String... categories) {
        RobolectricUtils.installCustomTabsService(PACKAGE, Arrays.asList(categories));
    }
}
