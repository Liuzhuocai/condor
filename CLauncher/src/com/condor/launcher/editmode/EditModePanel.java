package com.condor.launcher.editmode;

import android.content.Context;
import android.graphics.Rect;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DropTarget;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.wallpaperpicker.WallpaperPagedView;

import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

/**
 * Created by Perry on 19-1-18
 */
public class EditModePanel extends InsettableFrameLayout implements DropTarget {
    private final Launcher mLauncher;
    private final Rect mHitRect;
    // Perry: Implement sliding effect function: start
    private final TransitionSet mTransition;
    private EditNormalPanel mNormalPanel;
    private EditEffectPanel mEffectPanel;
    // Perry: Implement sliding effect function: end
    // Perry: Optimizing wallpaper picker UI: start
    private WallpaperPagedView mWallpaperPicker;
    // Perry: Optimizing wallpaper picker UI: end
    //liuzuo:add for wallpaper:end
    // Perry: Optimizing transition animation for edit mode: start
    private FrameLayout mContainer;
    // Perry: Optimizing transition animation for edit mode: end

    public EditModePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = Launcher.getLauncher(context);
        mHitRect  = new Rect();
        // Perry: Implement sliding effect function: start
        mTransition = new TransitionSet()
                .addTransition(new Fade())
                .addTransition(new Slide())
                .setDuration(300);
        // Perry: Implement sliding effect function: end
    }

    // Perry: Implement sliding effect function: start
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Perry: Optimizing transition animation for edit mode: start
        mContainer   = findViewById(R.id.editmode_container);
        // Perry: Optimizing transition animation for edit mode: end
        mNormalPanel = findViewById(R.id.editmode_normal_panel);
        mEffectPanel = findViewById(R.id.editmode_effect_panel);
        //liuzuo:add for wallpaper:start
        mWallpaperPicker = findViewById(R.id.wallpaper_picker);
        //liuzuo:add for wallpaper:end

        mEffectPanel.initParentViews(this);
        // Perry: Optimizing wallpaper picker UI: start
        mWallpaperPicker.initParentViews(this);
        // Perry: Optimizing wallpaper picker UI: end
    }

    public void resetPanels() {
        // Perry: Optimizing transition animation for edit mode: start
        if (mNormalPanel.getVisibility() != VISIBLE) {
            mNormalPanel.setVisibility(VISIBLE);
            mEffectPanel.setVisibility(GONE);
            mWallpaperPicker.setVisibility(GONE);
        }
        // Perry: Optimizing transition animation for edit mode: end
    }

    public boolean switchToEffectPanel() {
        //liuzuo:add for wallpaper:start
        if (mNormalPanel.getVisibility() == VISIBLE) {
            // Perry: Optimizing transition animation for edit mode: start
            TransitionManager.beginDelayedTransition(mContainer, mTransition.setInterpolator(new LinearOutSlowInInterpolator()));
            // Perry: Optimizing transition animation for edit mode: end
            mNormalPanel.setVisibility(GONE);
            mEffectPanel.setVisibility(VISIBLE);
            return true;
        }
        return false;
    }

    public boolean backToNormalPanel() {
        //liuzuo:add for wallpaper:start
       if (mEffectPanel.getVisibility() == VISIBLE||mWallpaperPicker.getVisibility() == VISIBLE) {
           // Perry: Optimizing transition animation for edit mode: start
           TransitionManager.beginDelayedTransition(mContainer, mTransition.setInterpolator(new FastOutLinearInInterpolator()));
           // Perry: Optimizing transition animation for edit mode: end
            mNormalPanel.setVisibility(VISIBLE);
            mEffectPanel.setVisibility(GONE);
            mWallpaperPicker.setVisibility(GONE);
            return true;
        }

        return false;
    }
    // Perry: Implement sliding effect function: end

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Workspace ws = mLauncher.getWorkspace();
        AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(mLauncher);
        // Perry: Adjust UI: start
        if (topOpenView == null && !mLauncher.getDeviceProfile().isVerticalBarLayout()) {
            ws.getScaledHitRect(mHitRect);
            if (mHitRect.contains((int) ev.getX(), (int) ev.getY())) {
                ev.offsetLocation(0, -ws.getTranslationY());
                try {
                    return ws.dispatchTouchEvent(ev);
                } finally {
                    ev.offsetLocation(0, ws.getTranslationY());
                }
            }
        }
        // Perry: Adjust UI: end

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void setInsets(Rect insets) {
        // Perry: Optimizing transition animation for edit mode: start
        DeviceProfile grid = mLauncher.getDeviceProfile();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mContainer.getLayoutParams();
        lp.topMargin = insets.top;
        lp.leftMargin = insets.left;
        lp.rightMargin = insets.right;
        lp.bottomMargin = insets.bottom;
        lp.height = grid.getEditModeButtonBarHeight();
        mContainer.setLayoutParams(lp);
        // Perry: Optimizing transition animation for edit mode: end
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    public void onDrop(DragObject dragObject, DragOptions options) {

    }

    @Override
    public void onDragEnter(DragObject dragObject) {

    }

    @Override
    public void onDragOver(DragObject dragObject) {

    }

    @Override
    public void onDragExit(DragObject dragObject) {

    }

    @Override
    public boolean acceptDrop(DragObject d) {
        if (d.dragSource instanceof Workspace) {
            mLauncher.getDragController().cancelDrag();
        }

        return false;
    }

    @Override
    public void prepareAccessibilityDrop() {

    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(getChildAt(0), outRect);
    }

    //liuzuo:add for wallpaper:start
    public boolean switchToWallpaperPanel() {
        if (mNormalPanel.getVisibility() == VISIBLE) {
            // Perry: Optimizing transition animation for edit mode: start
            TransitionManager.beginDelayedTransition(mContainer, mTransition.setInterpolator(new LinearOutSlowInInterpolator()));
            // Perry: Optimizing transition animation for edit mode: end
            mNormalPanel.setVisibility(GONE);
            mWallpaperPicker.setVisibility(VISIBLE);
            return true;
        }
        return false;
    }
    //liuzuo:add for wallpaper:end
}
