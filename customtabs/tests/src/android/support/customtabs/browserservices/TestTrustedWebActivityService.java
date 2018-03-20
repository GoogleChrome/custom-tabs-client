package android.support.customtabs.browserservices;

import android.app.Notification;

public class TestTrustedWebActivityService extends TrustedWebActivityService {
    public static final int SMALL_ICON_ID = 666;

    @Override
    protected boolean notifyNotificationWithChannel(String platformTag, int platformId,
            Notification notification, String channelName) {
        return true;
    }

    @Override
    protected void cancelNotification(String platformTag, int platformId) {
    }

    @Override
    protected int getSmallIconId() {
        return SMALL_ICON_ID;
    }
}
