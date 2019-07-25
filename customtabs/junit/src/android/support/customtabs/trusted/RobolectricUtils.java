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

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.support.customtabs.CustomTabsService;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.List;

/**
 * Utils for testing with Robolectric.
 */
public class RobolectricUtils {

    /**
     * Ensures intents with {@link CustomTabsService#ACTION_CUSTOM_TABS_CONNECTION} resolve to a
     * Service with given categories
     */
    public static void installCustomTabsService(String providerPackage, List<String> categories) {
        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(providerPackage);

        IntentFilter filter = new IntentFilter();
        for (String category : categories) {
            filter.addCategory(category);
        }
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.filter = filter;

        ShadowPackageManager manager = Shadows.shadowOf(RuntimeEnvironment.application
                .getPackageManager());
        manager.addResolveInfoForIntent(intent, resolveInfo);
    }
}
