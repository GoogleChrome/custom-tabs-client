# Chrome Custom Tabs - Example and Usage

## Summary

This presents an example application using Chrome Custom Tabs, and a possible
usage of both the intent and the background service APIs. It covers UI
customization, callback setup, pre-warming and pre-fetching, and lifecycle
management.

## Introduction

Chrome Custom Tabs provides a way for an application to customize and interact
with a Chrome `Activity` on Android, to make it a part of the application
experience, while retaining the full functionality and performance of a complete
web browser.

In particular, this covers:

* UI customization:
  * Toolbar color
  * Action button
  * Custom menu items
  * Custom in/out animations
* Navigation awareness: the browser delivers a callback to the application upon
  an external navigation.
* Performance optimizations:
  * Pre-warming of the Browser in the background, while avoiding stealing
    resources from the application
  * Providing a likely URL in advance to the browser, which may perform
    speculative work, speeding up page load time.
* Lifecycle management: the browser prevents the application from being evicted
  by the system while on top of it, by raising its importance to the
  "foreground" level.

These features can be accessed by appending extras to the `ACTION_VIEW` intent
and connecting to a bound service in Chrome. Here we present the way this is
handled in the example client application. Feel free to re-use the provided
classes in this sample, namely:

* `CustomTabUiBuilder`: Builds the intent extras used to customize the Custom Tab
  UI.
* `CustomTabActivityManager`: Handles the connection with the background service,
  the load time optimizations and the "KeepAlive" service.
* `KeepAliveService`: Empty remote service used by Chrome to keep the
  application alive.

## UI Customization

UI customization is handled by the `CustomTabUiBuilder` class. An instance of this
class has to be provided to `CustomTabActivityManager.loadUrl()` to load a URL in a
custom tab.

**Example:**
```java
CustomTabUiBuilder uiBuilder = new CustomTabUiBuilder().setToolbarColor(Color.BLUE);
// Application exit animation, Chrome enter animation.
uiBuilder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
// vice versa
uiBuilder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);

customTabManager.loadUrl(url, uiBuilder);
```

In this example, no UI customization is done, aside from the animations and the
toolbar color. The general usage is:

1. Create an instance of `CustomTabUiBuilder`
2. Build the UI using the methods of `CustomTabUiBuilder`
3. Provide this instance to `CustomTabActivityManager.loadUrl()`

The communication between the custom tab activity and the application is done
via pending intents. For each interaction leading back to the application
(through menu items), a
[`PendingIntent`](http://developer.android.com/reference/android/app/PendingIntent.html)
must be provided, and will be delivered upon activation of the UI element.

## Navigation

After a URL has been loaded inside a custom tab activity, the application can
receive a notification when the user navigates to another location. This is done
using a callback. The callback implements the interface of
`CustomTabActivityManager.NavigationCallback`, that is:

```java
void run(String url, Bundle extras);
```

This callback must be set before binding to the service, that is:

* Before calling `CustomTabActivityManager.bindService()`
* Before calling `CustomTabActivityManager.loadUrl()`

It can only be set **once**.

## Optimization

**WARNING:** The browser treats the calls described in this section only as
  advice. Actual behavior may depend on connectivity, available memory and other
  resources.

The application can communicate its intention to the browser, that is:
* Warming up the browser
* Indicating a likely navigation to a given URL

In both cases, communication with the browser is done through a bound background
service. This binding is done by `CustomTabActivityManager.bindService()`. This
**must** be called before the other methods discussed in this section.

* **Warmup:** Warms up the browser to make navigation faster. This is expected
  to create some CPU and IO activity, and to have a duration comparable to a
  normal Chrome startup. Once started, Chrome will not use additional
  resources. To warm up Chrome, use `CustomTabActivityManager.warmup()`.
* **May Launch URL:** Indicates that a given URL may be loaded in the
  future. Chrome may perform speculative work to speed up page load time. The
  application must call `CustomTabActivityManager.warmup()` first.

**Example:**
```java
// It is preferable to call this once the main activity has been shown.
// These calls are non-blocking and can be issued from the UI thread
// or any other thread. However, CustomTabActivityManager is not threadsafe.
CustomTabActivityManager customTabManager = CustomTabActivityManager.getInstance(activity);
customTabManager.bindService();
customTabManager.warmup();

// This URL is likely to be loaded. Tell the browser about it.
customTabManager.mayLaunchUrl(url, null);

// Show the custom tab.
customTabManager.loadUrl(url, uiBuilder);
```

**Tips**

* If possible, issue the warmup call in advance to reduce waiting when the
  custom tab activity is started.
* If possible, advise Chrome about the likely target URL in advance, as the
  loading optimization can take time (requiring network traffic, for instance).

## Lifecycle

Chrome Custom Tabs were designed with two constraints regarding lifecycle
management:

1. When Chrome is in the background, it should not steal CPU and/or memory from
   the application.
2. When a custom tab is in the foreground, the application should not be evicted
   by the Android framework.

The first concern is addressed by giving the background service a lower CPU
priority and eviction importance (using the `BIND_WAVE_PRIORITY` flag when
binding to the background service); the second by having Chrome bind to a
`Service` in the application to keep it alive. Such a dummy "KeepAlive" service
is provided in the example application. This requires a modification to the
`AndroidManifest.xml` file:

```xml
<service android:name="org.chromium.customtabsclient.KeepAliveService"
    android:exported="true" />
```
