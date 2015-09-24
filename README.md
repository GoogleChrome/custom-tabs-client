# Custom Tabs - Example and Usage

## Summary

This presents an example application using Custom Tabs, and a possible usage of
both the intent and the background service APIs. It covers UI customization,
callback setup, pre-warming and pre-fetching, and lifecycle management. Here we
assume that Chrome's implementation of Custom Tabs is used. Note that this
feature is in no way specific to Chrome, but slight differences may exist with
other implementations.

## Introduction

Chrome Custom Tabs provides a way for an application to customize and interact
with a Chrome `Activity` on Android, to make it a part of the application
experience, while retaining the full functionality and performance of a complete
web browser.

### Overview

In particular, this covers:

* UI customization:
  * Toolbar color
  * Action button
  * Custom menu items
  * Custom in/out animations
* Navigation awareness: the browser delivers callbacks to the application for
  navigations in the Custom Tab.
* Performance optimizations:
  * Pre-warming of the Browser in the background, without stealing resources
    from the application
  * Providing a likely URL in advance to the browser, which may perform
    speculative work, speeding up page load time.

These features are enabled through two mechanisms:

* Adding extras to the `ACTION_VIEW` intent sent to the browser.
* Connecting to a bound service in the target browser.

### Code Organization

The code in this repository is organised in four parts:

* `shared/`: Shared code between the `Application` and `demos` modules. Feel free to
  re-use the classes within this directory, which are only provided as a convenience.
  In particular,`CustomTabsHelper` can be re-used. This code is not required to use Custom Tabs.
* `demos/`: This module contains sample implementations for Chrome Custom Tabs using the Android
  Support Library. Feel free to re-use the classes withing this module.
* `customtabs/`: Code within this directory is in the package
  `android.support.customtabs`. This contains code analog to the Android Support library, but with
   the latest version of Chrome Custom Tabs, enabling features that may still not be available on
   the Android Support Library. API is subject to changes and this code should only be used if you
   want to test the latest features. It is recommended to copy the code as-is to your project and
   remove the Android Support Library for Chrome Custom Tabs from the `build.gradle` file.
* `Application/`: Example application code, in the package
  `org.chromium.customtabsclient`. This code uses the latest version of the Chrome Custom Tabs,
   contained in the module `customtabs`.

## UI Customization

UI customization is done through the methods exposed by
`CustomTabsIntent.Builder`.

**Example:**
```java
CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
builder.setSession(session);
builder.setToolbarColor(Color.BLUE);
// Application exit animation, Chrome enter animation.
builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
// vice versa
builder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);

CustomTabsIntent customTabsIntent = builder.build();
customTabsIntent.launchUrl(this, packageName, url);
```

In this example, no UI customization is done, aside from the animations and the
toolbar color. The general usage is:

1. Create an instance of `CustomTabsIntent.Builder`
2. Build the UI using the methods of `CustomTabsIntent.Builder`
3. Call `CustomTabsIntent.Builder.build()`
4. Call `CustomTabsIntent.launchUrl()`

The communication between the custom tab activity and the application is done
via pending intents. For each interaction leading back to the application (menu
items and action button), a
[`PendingIntent`](http://developer.android.com/reference/android/app/PendingIntent.html)
must be provided, and will be delivered upon activation of the corresponding UI
element.

## Navigation

The hosting application can elect to get notifications about navigations in a
Custom Tab. This is done using a callback extending
`android.support.customtabs.CustomTabsCallback`, that is:

```java
void onNavigationEvent(int navigationEvent, Bundle extras);
```

This callback is set when a `CustomTabsSession` object is created, through
`CustomTabsSession.newSession()`. It thus has to be set:

* After binding to the background service
* Before launching a URL in a custom tab

The two events are analogous to `WebViewClient.onPageStarted()` and
`WebViewClient.onPageFinished()`, respectively (see
[WebViewClient](http://developer.android.com/reference/android/webkit/WebViewClient.html)).

## Optimization

**WARNING:** The browser treats the calls described in this section only as
  advice. Actual behavior may depend on connectivity, available memory and other
  resources.

The application can communicate its intention to the browser, that is:
* Warming up the browser
* Indicating a likely navigation to a given URL

In both cases, communication with the browser is done through a bound background
service. This binding is done by
`CustomTabClient.bindCustomTabsService()`. After the service is connected, the
client has access to a `CustomTabsClient` object, valid until the service gets
disconnected. This client can be used in these two cases:

* **Warmup**: Warms up the browser to make navigation faster. This is expected
  to create some CPU and IO activity, and to have a duration comparable to a
  normal Chrome startup. Once started, Chrome will not use additional
  resources. This is triggered by `CustomTabsClient.warmup()`.
* **Hint about a likely future navigation:** Indicates that a given URL may be
  loaded in the future. Chrome may perform speculative work to speed up page
  load time. The application must call `CustomTabsClient.warmup()` first. This
  is triggered by `CustomTabsSession.mayLaunchUrl()`.

**Example:**
```java
// Binds to the service.
CustomTabsClient.bindCustomTabsService(context, packageName, new CustomTabsServiceConnection() {
    @Override
    public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
        // mClient is now valid.
        mClient = client;
    }

    @Override
    public void onServiceDisconnected(ComponentName name, CustomTabsClient client) {
        // mClient is no longer valid. This also invalidates sessions.
        mClient = null;
    }
});

// With a valid mClient.
mClient.warmup(0);

// With a valid mClient.
CustomTabsSession session = mClient.newSession(new CustomTabsCallback());
session.mayLaunchUrl("https://www.google.com", null, null);

// Shows the Custom Tab
builder.build().launchUrl(context, packageName, Uri.parse("https://www.google.com"));
```

**Tips**

* If possible, issue the warmup call in advance to reduce waiting when the
  custom tab activity is started.
* If possible, advise Chrome about the likely target URL in advance, as the
  loading optimization can take time (requiring network traffic, for instance).

**Bugs**

Issues and bugs related to these examples and the Chrome implementation of
Custom Tabs are tracked in the Chromium Issue Tracker.
[This template](https://code.google.com/p/chromium/issues/entry?summary=Issue%20Summary&comment=Application%20Version%20(from%20%22Chrome%20Settings%20%3E%20About%20Chrome%22):%20%0DAndroid%20Build%20Number%20(from%20%22Android%20Settings%20%3E%20About%20Phone/Tablet%22):%20%0DDevice:%20%0D%0DSteps%20to%20reproduce:%20%0D%0DObserved%20behavior:%20%0D%0DExpected%20behavior:%20%0D%0DFrequency:%20%0D%3Cnumber%20of%20times%20you%20were%20able%20to%20reproduce%3E%20%0D%0DAdditional%20comments:%20%0D&labels=OS-Android,Cr-UI-Browser-Mobile-CustomTabs)
will notify relevant people faster than issues on GitHub, this is the preferred
way to report.
