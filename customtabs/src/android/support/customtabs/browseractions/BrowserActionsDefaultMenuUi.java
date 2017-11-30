package android.support.customtabs.browseractions;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * The class to show backup menu for Browser Actions if no provider is available.
 */
public class BrowserActionsDefaultMenuUi implements AdapterView.OnItemClickListener {
    private static final String TAG = "BrowserActionsDefaultMenuUi";

    private final Context mContext;
    private final Uri mUri;
    private final List<BrowserActionItem> mMenuItems;

    private BrowserActionsDefaultMenuDialog mBrowserActionsDialog;

    public BrowserActionsDefaultMenuUi(
            Context context, Uri uri, List<BrowserActionItem> menuItems) {
        mContext = context;
        mUri = uri;
        mMenuItems = menuItems;
    }

    /**
     * Shows the default menu.
     */
    public void displayMenu() {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.browser_actions_context_menu_page, null);
        mBrowserActionsDialog = new BrowserActionsDefaultMenuDialog(mContext, initMenuView(view));
        mBrowserActionsDialog.setContentView(view);
        mBrowserActionsDialog.show();
    }

    private BrowserActionsDefaultMenuView initMenuView(View view) {
        BrowserActionsDefaultMenuView menuView =
                (BrowserActionsDefaultMenuView) view.findViewById(R.id.browser_actions_menu_view);

        TextView urlTextView = (TextView) view.findViewById(R.id.browser_actions_header_text);
        urlTextView.setText(mUri.toString());
        urlTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (urlTextView.getMaxLines() == Integer.MAX_VALUE) {
                    urlTextView.setMaxLines(1);
                    urlTextView.setEllipsize(TextUtils.TruncateAt.END);
                } else {
                    urlTextView.setMaxLines(Integer.MAX_VALUE);
                    urlTextView.setEllipsize(null);
                }
            }
        });

        ListView menuListView = (ListView) view.findViewById(R.id.browser_actions_menu_items);
        BrowserActionsDefaultMenuAdapter adapter =
                new BrowserActionsDefaultMenuAdapter(mMenuItems, mContext);
        menuListView.setAdapter(adapter);
        menuListView.setOnItemClickListener(this);

        return menuView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PendingIntent action = mMenuItems.get(position).getAction();
        try {
            action.send();
            mBrowserActionsDialog.dismiss();
        } catch (CanceledException e) {
            Log.e(TAG, "Fail to send custom item action", e);
        }
    }
}
