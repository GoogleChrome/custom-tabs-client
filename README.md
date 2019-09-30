# Chrome Custom Tabs - Examples and Documentation

Chrome Custom Tabs provides a way for an application to customize and interact
with a Chrome `Activity` on Android. This makes the web content feel like being
a part of the application, while retaining the full functionality and
performance of a complete web browser.

## AndroidX

This library is based off (and contains copies of the classes from) the Android
Support Library Custom Tabs package.
There are known bugs when using custom-tabs-client with AndroidX or with
Jetifier (the AndroidX conversion tool).
For example, run-time crashes like [this bug](https://crbug.com/983378):

```
java.lang.IllegalAccessError:
  Method 'android.os.Bundle android.support.customtabs.CustomTabColorSchemeParams.toBundle()'
  is inaccessible to class 'androidx.browser.customtabs.CustomTabsIntent$Builder'
  (declaration of 'androidx.browser.customtabs.CustomTabsIntent$Builder' appears in XYZ.apk)
```

Please use the
[Android Browser Helper](https://github.com/GoogleChrome/android-browser-helper)
library instead.
It contains the same functionality updated to work with AndroidX.

## Examples

[Using Custom
Tabs](https://chromium.googlesource.com/custom-tabs-client/+/master/Using.md)
should be easy. This repository hosts examples and in-depth documentation. The
examples are importable as projects into the Android Studio.

## Bugs, Issues and Discussion

We want to hear your feedback! Please create bugs and start discussions using
[this template](https://code.google.com/p/chromium/issues/entry?summary=Issue%20Summary&comment=Application%20Version%20(from%20%22Chrome%20Settings%20%3E%20About%20Chrome%22):%20%0DAndroid%20Build%20Number%20(from%20%22Android%20Settings%20%3E%20About%20Phone/Tablet%22):%20%0DDevice:%20%0D%0DSteps%20to%20reproduce:%20%0D%0DObserved%20behavior:%20%0D%0DExpected%20behavior:%20%0D%0DFrequency:%20%0D%3Cnumber%20of%20times%20you%20were%20able%20to%20reproduce%3E%20%0D%0DAdditional%20comments:%20%0D&labels=OS-Android,Cr-UI-Browser-Mobile-CustomTabs).
Please use the template for any issues related to the Custom Tabs APIs, their
implementation in Chrome, the examples in this repository, and related
functionality of the Android Support Library.

Note: we know that the GitHub issue tracker is great! However, since Custom Tabs
is mostly driven by Chrome developers, and we want to keep all bugs in one
place, we prefer the Chromium issue tracker at the moment.

## Contributing

We accept contributions to Custom Tabs examples and documentation. Please see
[our contributor's guide](https://chromium.googlesource.com/custom-tabs-client/+/master/CONTRIBUTING.md).
