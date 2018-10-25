package android.support.customtabs;

import android.content.Context;
import android.os.Bundle;

public interface PostMessageBackend {

    boolean postMessage(String message, Bundle extras);

    boolean notifyMessageChannelReady(Bundle extras);

    void onDisconnectChannel(Context appContext);
}
