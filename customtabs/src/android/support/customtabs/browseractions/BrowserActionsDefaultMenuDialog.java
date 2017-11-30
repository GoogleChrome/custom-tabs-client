package android.support.customtabs.browseractions;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLayoutChangeListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;

/**
 * The dialog class showing the context menu and ensures proper animation is played upon calling
 * {@link #show()} and {@link #dismiss()}.
 */
public class BrowserActionsDefaultMenuDialog extends Dialog {
    private static final long ENTER_ANIMATION_DURATION_MS = 250;
    // Exit animation duration should be set to 60% of the enter animation duration.
    private static final long EXIT_ANIMATION_DURATION_MS = 150;
    private final View mContentView;

    public BrowserActionsDefaultMenuDialog(Context context, View contentView) {
        super(context);
        mContentView = contentView;
    }

    @Override
    public void show() {
        Window dialogWindow = getWindow();
        dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogWindow.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mContentView.setVisibility(View.INVISIBLE);
        mContentView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (v instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) v;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        if (group.getChildAt(i).getMeasuredHeight() == 0
                                && group.getChildAt(i).getVisibility() == View.VISIBLE) {
                            // Return early because not all the views have been measured, so
                            // animation pivots will be off.
                            return;
                        }
                    }
                }
                mContentView.setVisibility(View.VISIBLE);
                getScaleAnimation(true);
                mContentView.removeOnLayoutChangeListener(this);
            }
        });
        super.show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void dismiss() {
        Animation exitAnimation = getScaleAnimation(false);
        exitAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                BrowserActionsDefaultMenuDialog.super.dismiss();
            }
        });
        mContentView.startAnimation(exitAnimation);
    }

    private Animation getScaleAnimation(boolean isEnterAnimation) {
        float fromX = isEnterAnimation ? 0f : 1f;
        float toX = isEnterAnimation ? 1f : 0f;
        float fromY = fromX;
        float toY = toX;

        ScaleAnimation animation = new ScaleAnimation(fromX, toX, fromY, toY,
                Animation.RELATIVE_TO_PARENT, 0.5f, Animation.RELATIVE_TO_PARENT, 0.5f);

        long duration = isEnterAnimation ? ENTER_ANIMATION_DURATION_MS : EXIT_ANIMATION_DURATION_MS;

        animation.setDuration(duration);
        return animation;
    }
}
