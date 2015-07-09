# Copyright 2015 Google Inc. All rights reserved.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

{
  'targets': [
    {
      'target_name': 'custom_tabs_client_example_apk',
      'type': 'none',
      'variables': {
        'java_in_dir': 'Application/src/main/',
        'java_in_dir_suffix': 'java/',
        'resource_dir': '<(java_in_dir)/res',
        'apk_name': 'CustomTabsClientExample',
        'run_findbugs': 0,
      },
      'dependencies': [
        'custom_tabs_support_lib',
      ],
      'includes': [ '../../../build/java_apk.gypi' ],
    },
    {
      'target_name': 'custom_tabs_support_lib',
      'type': 'none',
      'variables': {
        'java_in_dir': 'customtabs/android/support/customtabs/',
        'java_in_dir_suffix': '',
      },
      'dependencies': [
        'chrome_custom_tabs_service_aidl',
      ],
      'includes': [ '../../../build/java.gypi' ],
    },
    {
      'target_name': "chrome_custom_tabs_service_aidl",
      'type': 'none',
      'variables': {
        'java_in_dir': 'customtabs/android/support/customtabs/',
        'java_in_dir_suffix': '',
        'aidl_interface_file': '<(java_in_dir)/common.aidl',
      },
      'sources': [
        '<(java_in_dir)/ICustomTabsCallback.aidl',
        '<(java_in_dir)/ICustomTabsService.aidl',
      ],
      'includes': [ '../../../build/java_aidl.gypi' ],
    },
  ],
}
