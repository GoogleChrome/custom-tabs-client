/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.support.customtabs.browserservices;

/**
 * Interface to a TrustedWebActivityService.
 * @hide
 */
interface ITrustedWebActivityService {
   // Display a notification with a specified channel name.
   // Arguments:
   // String PLATFORM_TAG
   // int PLATFORM_ID
   // Notification NOTIFICATION
   // String CHANNEL_NAME
   // Returns:
   // boolean SUCCESS
   Bundle notifyNotificationWithChannel(in Bundle args);

   // Cancel a notification.
   // Arguments:
   // String PLATFORM_TAG
   // int PLATFORM_ID
   void cancelNotification(in Bundle args);

   // Gets the resource id to be used for the notifications small icon.
   int getSmallIconId();
}
