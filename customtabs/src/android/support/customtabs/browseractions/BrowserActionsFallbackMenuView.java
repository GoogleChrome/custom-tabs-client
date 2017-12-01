package android.support.customtabs.browseractions;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * The class responsible for deciding the size of Browser Actions context menu.
 */
public class BrowserActionsFallbackMenuView extends LinearLayout {
    private final int mBrowserActionsMenuMinPaddingPx;
    private final int mBrowserActionsMenuMaxWidthPx;

    public BrowserActionsFallbackMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBrowserActionsMenuMinPaddingPx = getResources().getDimensionPixelOffset(
                R.dimen.browser_actions_context_menu_min_padding);
        mBrowserActionsMenuMaxWidthPx = getResources().getDimensionPixelOffset(
                R.dimen.browser_actions_context_menu_max_width);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int appWindowWidthPx = getResources().getDisplayMetrics().widthPixels;
        int contextMenuWidth = Math.min(appWindowWidthPx - 2 * mBrowserActionsMenuMinPaddingPx,
                mBrowserActionsMenuMaxWidthPx);
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(contextMenuWidth, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
