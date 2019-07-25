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

import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.trusted.TrustedWebActivityIntentBuilder;

/**
 * Defines behavior of the splash screen shown when launching a TWA.
 */
public interface SplashScreenStrategy {

    /**
     * Called immediately in the beginning of TWA launching process (before establishing
     * connection with CustomTabsService). Can be used to display splash screen on the client app's
     * side before the browser is launched.
     * @param providerPackage Package name of the browser being launched. Implementations should
     * check whether this browser supports splash screens.
     * @param builder {@link TrustedWebActivityIntentBuilder} with user-specified parameters, such as
     * status bar color.
     */
    void onTwaLaunchInitiated(String providerPackage, TrustedWebActivityIntentBuilder builder);

    /**
     * Called when TWA is ready to be launched.
     * @param builder {@link TrustedWebActivityIntentBuilder} to be supplied with splash screen related
     * parameters.
     * @param session {@link CustomTabsSession} with which the TWA will launch.
     * @param onReadyCallback Callback to be triggered when splash screen preparation is finished.
     * TWA is launched immediately upon triggering this callback.
     */
    void configureTwaBuilder(TrustedWebActivityIntentBuilder builder, CustomTabsSession session,
            Runnable onReadyCallback);

}
