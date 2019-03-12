package android.support.customtabs.trusted;

import static android.support.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.customtabs.CustomTabsService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

/**
 * Tests for {@link TwaProviderPicker}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class TwaProviderPickerTest {
    private PackageManager mPackageManager;
    private ShadowPackageManager mShadowPackageManager;

    private static final String BROWSER1 = "com.browser.one";
    private static final String BROWSER2 = "com.browser.two";
    private static final String CUSTOM_TABS_PROVIDER1 = "com.customtabs.one";
    private static final String CUSTOM_TABS_PROVIDER2 = "com.customtabs.two";
    private static final String TWA_PROVIDER1 = "com.trustedweb.one";
    private static final String TWA_PROVIDER2 = "com.trustedweb.two";

    // TODO(peconn): Deduplicate these with members in TrustedWebUtils (should probably move
    // TrustedWebUtils into this package, which requires removing TrustedWebUtils' dependencies on
    // package methods in CustomTabsSession (in #launchBrowserSessions)).
    private static final String CHROME = "com.android.chrome";
    private static final int CHROME_72_VERSION = 362600000;

    @Before
    public void setUp() {
        mPackageManager = RuntimeEnvironment.application.getPackageManager();
        mShadowPackageManager = shadowOf(mPackageManager);
    }

    /**
     * Tests that we attempt don't do anything stupid if the user (somehow) does not have any
     * browsers on their device.
     */
    @Test
    public void noBrowsers() {
        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(TwaProviderPicker.BROWSER, action.launchMode);
        assertNull(action.provider);
    }

    /**
     * Tests that in lack of any Custom Tabs or Trusted Web Activity providers, we choose Android's
     * preferred browser.
     */
    @Test
    public void noCustomTabsProviders() {
        installBrowser(BROWSER1);
        installBrowser(BROWSER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(TwaProviderPicker.BROWSER, action.launchMode);
        assertEquals(BROWSER1, action.provider);
    }

    /**
     * Tests that a Custom Tabs provider is chosen over non-Custom Tabs browsers.
     */
    @Test
    public void customTabsProvider() {
        installBrowser(BROWSER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(TwaProviderPicker.CUSTOM_TAB, action.launchMode);
        assertEquals(CUSTOM_TABS_PROVIDER1, action.provider);
    }

    /**
     * Tests that a Trusted Web Activity provider takes preference over all.
     */
    @Test
    public void trustedWebActivityProvider() {
        installBrowser(BROWSER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER1);
        installTrustedWebActivityProvider(TWA_PROVIDER1);
        installTrustedWebActivityProvider(TWA_PROVIDER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(TwaProviderPicker.TRUSTED_WEB_ACTIVITY, action.launchMode);
        assertEquals(TWA_PROVIDER1, action.provider);
    }

    /**
     * Tests that we recognise Chrome 72 as supporting Trusted Web Activities even though it doesn't
     * have the TRUSTED_WEB_ACTIVITY_CATEGORY.
     */
    @Test
    public void choosesChrome72() {
        installBrowser(BROWSER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER1);
        installChrome72();
        installTrustedWebActivityProvider(TWA_PROVIDER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(TwaProviderPicker.TRUSTED_WEB_ACTIVITY, action.launchMode);
        assertEquals(CHROME, action.provider);
    }

    /**
     * Tests that if the user has a non-Chrome TWA provider as their default, we choose that.
     */
    @Test
    public void choosesDefaultOverChrome() {
        installTrustedWebActivityProvider(TWA_PROVIDER1);
        installChrome72();

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(TwaProviderPicker.TRUSTED_WEB_ACTIVITY, action.launchMode);
        assertEquals(TWA_PROVIDER1, action.provider);
    }

    private void installBrowser(String packageName) {
        Intent intent = new Intent()
                .setData(Uri.parse("http://"))
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.resolvePackageName = packageName;

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installCustomTabsProvider(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(packageName);

        mShadowPackageManager.addResolveInfoForIntent(intent, new ResolveInfo());
    }

    private void installTrustedWebActivityProvider(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(packageName);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.filter = Mockito.mock(IntentFilter.class);
        when(resolveInfo.filter.hasCategory(eq(TRUSTED_WEB_ACTIVITY_CATEGORY))).thenReturn(true);

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installChrome72() {
        installTrustedWebActivityProvider(CHROME);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = CHROME_72_VERSION;
        packageInfo.packageName = CHROME;

        mShadowPackageManager.addPackage(packageInfo);
    }
}