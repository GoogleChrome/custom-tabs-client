package android.support.customtabs.browseractions;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

/**
 * The dialog class showing the context menu and ensures proper animation is played upon calling
 * {@link #show()} and {@link #dismiss()}.
 */
public class BrowserActionsFallbackMenuDialog extends Dialog {
    private static final long ENTER_ANIMATION_DURATION_MS = 250;
    // Exit animation duration should be set to 60% of the enter animation duration.
    private static final long EXIT_ANIMATION_DURATION_MS = 150;
    private final View mContentView;

    public BrowserActionsFallbackMenuDialog(Context context, int theme, View contentView) {
        super(context, theme);
        mContentView = contentView;
    }

    @Override
    public void show() {
        Window dialogWindow = getWindow();
        dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        startAnimation(true);
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
        startAnimation(false);
    }

    private void startAnimation(boolean isEnterAnimation) {
        float from = 0f;
        float to = 1f;
        long duration = isEnterAnimation ? ENTER_ANIMATION_DURATION_MS : EXIT_ANIMATION_DURATION_MS;
        if (!isEnterAnimation) {
            from = 1f;
            to = 0f;
        }
        mContentView.setScaleX(from);
        mContentView.setScaleY(from);

        mContentView.animate()
                .scaleX(to)
                .scaleY(to)
                .setDuration(duration)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setListener(new AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!isEnterAnimation) {
                            BrowserActionsFallbackMenuDialog.super.dismiss();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {}
                })
                .start();
    }
}
