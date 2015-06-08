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

import org.chromium.chrome.browser.customtabs.ICustomTabsConnectionCallback;

import android.os.Bundle;

/**
 * Interface for communicating between a browser background service and another application.
 */
interface ICustomTabsConnectionService {
    /**
     * Sets the callback triggered on an external navigation.
     *
     * Must be called right after the service connection, and must be called
     * again if the service gets disconnected. Only one call to this method is
     * allowed, following ones will return an error.
     * Must be called before the VIEW intent is sent to the browser.
     *
     * @param callback Callback to be called, null if no callback is wanted.
     * @return 0 for success.
     */
    long finishSetup(ICustomTabsConnectionCallback callback);

    /**
     * Warms up the browser process.
     *
     * Warmup is asynchronous, the return value indicates that the request has been accepted.
     *
     * @param flags Reserved for future use.
     * @return 0 for success.
     */
    long warmup(long flags);

    /**
     * @return A negative number to signal an error, or a positive new session ID.
     */
    long newSession();

    /**
     * Tells the browser of a likely future navigation to a URL.
     *
     * The method {@link warmup} has to be called first.
     * The most likely URL has to be specified first. Optionally, a list of
     * other likely URLs can be provided. They are treated as less likely than
     * the first one, and have to be sorted in decreasing priority order. These
     * additional URLs may be ignored.
     * All previous calls to this method will be deprioritized.
     *
     * @param sessionId As returned by {@link newSession}.
     * @param url Most likely URL.
     * @param extras Reserved for future use.
     * @param otherLikelyBundles Other likely destinations, sorted in decreasing
     *     likelihood order. Each Bundle has to provide a "url" String value.
     * @return sessionId if it is known by the service, a number <0 to signal an error.
     */
    long mayLaunchUrl(
            long sessionId, String url, in Bundle extras, in List<Bundle> otherLikelyBundles);
}
