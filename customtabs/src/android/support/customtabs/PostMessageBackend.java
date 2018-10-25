package android.support.customtabs;

import android.content.Context;
import android.os.Bundle;

/**
 * Abstracts a receiver of postMessage events. For example, this could be a service connection like
 * {@link PostMessageServiceConnection} or it could be a local client.
 *
 * <p>This will always be backed by a class on the provider side rather than the client side.
 * However, in the case of {@link PostMessageServiceConnection}, it will defer to the client by
 * making remote calls.
 */
public interface PostMessageBackend {

    /**
     * Posts a message to the client.
     * @param message The String message to post.
     * @param extras Unused.
     * @return Whether the postMessage was sent successfully.
     */
    boolean onPostMessage(String message, Bundle extras);

    /**
     * Notifies the client that the postMessage channel is ready to be used.
     * @param extras Unused.
     * @return Whether the notification was sent successfully.
     */
    boolean onNotifyMessageChannelReady(Bundle extras);

    /**
     * Notifies the client that the channel has been disconnected.
     * @param appContext The application context.
     */
    void onDisconnectChannel(Context appContext);
}
