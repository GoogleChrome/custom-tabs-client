package android.support.customtabs.browseractions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * The adapter to display the icon and title of custom Browser Actions item.
 */
public class BrowserActionsFallbackMenuAdapter extends BaseAdapter {
    private final List<BrowserActionItem> mMenuItems;
    private final Context mContext;

    public BrowserActionsFallbackMenuAdapter(List<BrowserActionItem> menuItems, Context context) {
        mMenuItems = menuItems;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mMenuItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mMenuItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BrowserActionItem menuItem = mMenuItems.get(position);
        ViewHolderItem viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.browser_actions_context_menu_row, null);
            viewHolder = new ViewHolderItem();
            viewHolder.mIcon =
                    (ImageView) convertView.findViewById(R.id.browser_actions_menu_item_icon);
            viewHolder.mText =
                    (TextView) convertView.findViewById(R.id.browser_actions_menu_item_text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolderItem) convertView.getTag();
        }

        viewHolder.mText.setText(menuItem.getTitle());
        if (menuItem.getIconId() != 0) {
            Drawable drawable = ResourcesCompat.getDrawable(
                    mContext.getResources(), menuItem.getIconId(), null);
            viewHolder.mIcon.setImageDrawable(drawable);
        } else {
            viewHolder.mIcon.setImageDrawable(null);
        }
        return convertView;
    }

    private static class ViewHolderItem {
        ImageView mIcon;
        TextView mText;
    }
}
