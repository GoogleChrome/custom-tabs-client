// Copyright 2015 Google Inc. All Rights Reserved.
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

package org.chromium.chrome.browser.customtabs;

import android.os.Bundle;

/**
 * Interface for the client-provided callback on user navigation.
 */
interface ICustomTabsConnectionCallback {
    /**
     * To be called when a page navigation starts.
     *
     * @param sessionId As returned by {@link ICustomTabsConnectionService#newSession}.
     * @param url URL the user has navigated to.
     * @param extras Reserved for future use.
     */
    oneway void onUserNavigationStarted(long sessionId, String url, in Bundle extras);

    /**
     * To be called when a page navigation finishes.
     *
     * @param sessionId As returned by {@link ICustomTabsConnectionService#newSession}.
     * @param url URL the user has navigated to.
     * @param extras Reserved for future use.
     */
    oneway void onUserNavigationFinished(long sessionId, String url, in Bundle extras);
}
