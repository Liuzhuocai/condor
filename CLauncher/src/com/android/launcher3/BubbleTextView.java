/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.widget.TextView;

import com.android.launcher3.IconCache.IconLoadRequest;
import com.android.launcher3.IconCache.ItemInfoUpdateReceiver;
import com.android.launcher3.Launcher.OnResumeCallback;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.badge.BadgeRenderer;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.graphics.IconPalette;
import com.android.launcher3.graphics.PreloadIconDrawable;
import com.android.launcher3.model.PackageItemInfo;
import com.condor.launcher.CustomTools;
import com.condor.launcher.liveicon.IconUpdateListener;
import com.condor.launcher.liveicon.LiveIconsManager;
import com.condor.launcher.unreadnotifier.UnreadInfoManager;
import com.condor.launcher.unreadnotifier.UnreadUtils;

import java.text.NumberFormat;

import androidx.core.graphics.ColorUtils;

/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends TextView implements ItemInfoUpdateReceiver, OnResumeCallback, IconUpdateListener {

    private static final int DISPLAY_WORKSPACE = 0;
    private static final int DISPLAY_ALL_APPS = 1;
    private static final int DISPLAY_FOLDER = 2;
    private static final String TAG = "BubbleTextView";

    private static final int[] STATE_PRESSED = new int[] {android.R.attr.state_pressed};

    //liuzuo:add for addIcon:begin
    private boolean isChecked;

    private Drawable mCheckDrawable;
    //liuzuo:add for addIcon:end
    private static final Property<BubbleTextView, Float> BADGE_SCALE_PROPERTY
            = new Property<BubbleTextView, Float>(Float.TYPE, "badgeScale") {
        @Override
        public Float get(BubbleTextView bubbleTextView) {
            return bubbleTextView.mBadgeScale;
        }

        @Override
        public void set(BubbleTextView bubbleTextView, Float value) {
            bubbleTextView.mBadgeScale = value;
            bubbleTextView.invalidate();
        }
    };

    public static final Property<BubbleTextView, Float> TEXT_ALPHA_PROPERTY
            = new Property<BubbleTextView, Float>(Float.class, "textAlpha") {
        @Override
        public Float get(BubbleTextView bubbleTextView) {
            return bubbleTextView.mTextAlpha;
        }

        @Override
        public void set(BubbleTextView bubbleTextView, Float alpha) {
            bubbleTextView.setTextAlpha(alpha);
        }
    };

    private final BaseDraggingActivity mActivity;
    private Drawable mIcon;
    private final boolean mCenterVertically;

    private final CheckLongPressHelper mLongPressHelper;
    private final StylusEventHelper mStylusEventHelper;
    private final float mSlop;

    private final boolean mLayoutHorizontal;
    private final int mIconSize;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mIsIconVisible = true;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mTextColor;
    @ViewDebug.ExportedProperty(category = "launcher")
    private float mTextAlpha = 1;

    private BadgeInfo mBadgeInfo;
    private BadgeRenderer mBadgeRenderer;
    private int mBadgeColor;
    private float mBadgeScale;
    private boolean mForceHideBadge;
    private Point mTempSpaceForBadgeOffset = new Point();
    private Rect mTempIconBounds = new Rect();

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mStayPressed;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mIgnorePressedStateChange;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mDisableRelayout = false;

    private IconLoadRequest mIconLoadRequest;

    public BubbleTextView(Context context) {
        this(context, null, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mActivity = BaseDraggingActivity.fromContext(context);
        DeviceProfile grid = mActivity.getDeviceProfile();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.BubbleTextView, defStyle, 0);
        mLayoutHorizontal = a.getBoolean(R.styleable.BubbleTextView_layoutHorizontal, false);

        int display = a.getInteger(R.styleable.BubbleTextView_iconDisplay, DISPLAY_WORKSPACE);
        int defaultIconSize = grid.iconSizePx;
        if (display == DISPLAY_WORKSPACE) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, grid.iconTextSizePx);
            setCompoundDrawablePadding(grid.iconDrawablePaddingPx);
        } else if (display == DISPLAY_ALL_APPS) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, grid.allAppsIconTextSizePx);
            setCompoundDrawablePadding(grid.allAppsIconDrawablePaddingPx);
            defaultIconSize = grid.allAppsIconSizePx;
        } else if (display == DISPLAY_FOLDER) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, grid.folderChildTextSizePx);
            setCompoundDrawablePadding(grid.folderChildDrawablePaddingPx);
            defaultIconSize = grid.folderChildIconSizePx;
        }
        mCenterVertically = a.getBoolean(R.styleable.BubbleTextView_centerVertically, false);

        mIconSize = a.getDimensionPixelSize(R.styleable.BubbleTextView_iconSizeOverride,
                defaultIconSize);
        a.recycle();

        mLongPressHelper = new CheckLongPressHelper(this);
        mStylusEventHelper = new StylusEventHelper(new SimpleOnStylusPressListener(this), this);
        //liuzuo:remove ellipsize:start
        //setEllipsize(TruncateAt.END);
        //liuzuo:remove ellipsize:end
        setAccessibilityDelegate(mActivity.getAccessibilityDelegate());
        setTextAlpha(1f);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // Disable marques when not focused to that, so that updating text does not cause relayout.
        //liuzuo:remove ellipsize:start
        //setEllipsize(focused ? TruncateAt.MARQUEE : TruncateAt.END);
        //liuzuo:remove ellipsize:end
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    /**
     * Resets the view so it can be recycled.
     */
    public void reset() {
        mBadgeInfo = null;
        mBadgeColor = Color.TRANSPARENT;
        mBadgeScale = 0f;
        mForceHideBadge = false;
    }

    public void applyFromShortcutInfo(ShortcutInfo info) {
        applyFromShortcutInfo(info, false);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, boolean promiseStateChanged) {
        applyIconAndLabel(info);
        //Log.d("liuzuo87","getTargetComponent"+info.getTargetComponent().toString());
        setTag(info);
        //liuzuo:fix usingLowResIcon bug:begin
        verifyHighRes();
        //liuzuo:fix usingLowResIcon bug:end
        if (promiseStateChanged || (info.hasPromiseIconUi())) {
            applyPromiseState(promiseStateChanged);
        }

        applyBadgeState(info, false /* animate */);
    }

    // Perry: Add highlighter text for search results: start
    public void applyFromApplicationInfo(AppInfo info) {
        applyFromApplicationInfo(info, null);
    }

    public void applyFromApplicationInfo(AppInfo info, Spannable highlightedText) {
        applyIconAndLabel(info, highlightedText);

        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();

        if (info instanceof PromiseAppInfo) {
            PromiseAppInfo promiseAppInfo = (PromiseAppInfo) info;
            applyProgressLevel(promiseAppInfo.level);
        }
        applyBadgeState(info, false /* animate */);
    }

    public void applyFromPackageItemInfo(PackageItemInfo info) {
        applyIconAndLabel(info);
        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();
    }
    // Perry: Add highlighter text for search results: end

    // Perry: Add highlighter text for search results: start
    private void applyIconAndLabel(ItemInfoWithIcon info) {
        applyIconAndLabel(info, null);
    }

    private void applyIconAndLabel(ItemInfoWithIcon info, Spannable highlightedText) {
        // Perry: Add live icon: start
        LiveIconsManager.obtain().addHostView(info, this);
        // Perry: Add live icon: end
        FastBitmapDrawable iconDrawable = DrawableFactory.get(getContext()).newIcon(info);
        mBadgeColor = IconPalette.getMutedColor(info.iconColor, 0.54f);

        setIcon(iconDrawable);
        if (highlightedText != null) {
            setText(highlightedText, TextView.BufferType.SPANNABLE);
        } else {
            setText(info.title);
        }
        if (info.contentDescription != null) {
            setContentDescription(info.isDisabled()
                    ? getContext().getString(R.string.disabled_app_label, info.contentDescription)
                    : info.contentDescription);
        }
    }
    // Perry: Add highlighter text for search results: end

    /**
     * Overrides the default long press timeout.
     */
    public void setLongPressTimeout(int longPressTimeout) {
        mLongPressHelper.setLongPressTimeout(longPressTimeout);
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    public void refreshDrawableState() {
        if (!mIgnorePressedStateChange) {
            super.refreshDrawableState();
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mStayPressed) {
            mergeDrawableStates(drawableState, STATE_PRESSED);
        }
        return drawableState;
    }

    /** Returns the icon for this view. */
    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        // Check for a stylus button press, if it occurs cancel any long press checks.
        if (mStylusEventHelper.onMotionEvent(event)) {
            mLongPressHelper.cancelLongPress();
            result = true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // If we're in a stylus button press, don't check for long press.
                if (!mStylusEventHelper.inStylusButtonPressed()) {
                    mLongPressHelper.postCheckForLongPress();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!Utilities.pointInView(this, event.getX(), event.getY(), mSlop)) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }
        return result;
    }

    void setStayPressed(boolean stayPressed) {
        mStayPressed = stayPressed;
        refreshDrawableState();
    }

    @Override
    public void onLauncherResume() {
        // Reset the pressed state of icon that was locked in the press state while activity
        // was launching
        setStayPressed(false);
    }

    void clearPressedBackground() {
        setPressed(false);
        setStayPressed(false);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Unlike touch events, keypress event propagate pressed state change immediately,
        // without waiting for onClickHandler to execute. Disable pressed state changes here
        // to avoid flickering.
        mIgnorePressedStateChange = true;
        boolean result = super.onKeyUp(keyCode, event);
        mIgnorePressedStateChange = false;
        refreshDrawableState();
        return result;
    }

    @SuppressWarnings("wrongcall")
    protected void drawWithoutBadge(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //liuzuo:add for addIcon:begin
        drawCheckDrawable(getCheckDrawable(),canvas);
        //liuzuo:add for addIcon:end
        if (mUnreadCount >= 1 && supportDrawUnread()) {
            UnreadUtils.drawUnreadEventIfNeed(canvas, this, mUnreadCount);
        } else {
            drawBadgeIfNecessary(canvas);
        }
    }

    /**
     * Draws the icon badge in the top right corner of the icon bounds.
     * @param canvas The canvas to draw to.
     */
    protected void drawBadgeIfNecessary(Canvas canvas) {
        if (!mForceHideBadge && (hasBadge() || mBadgeScale > 0)
            //liuzuo:add for addIcon:begin
            &&!isChecked) {
            //liuzuo:add for addIcon:end

            getIconBounds(mTempIconBounds);
            mTempSpaceForBadgeOffset.set((getWidth() - mIconSize) / 2, getPaddingTop());
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.translate(scrollX, scrollY);
            mBadgeRenderer.draw(canvas, mBadgeColor,mBadgeInfo, mTempIconBounds, mBadgeScale,
                    mTempSpaceForBadgeOffset);
            canvas.translate(-scrollX, -scrollY);
        }
    }

    public void forceHideBadge(boolean forceHideBadge) {
        if (mForceHideBadge == forceHideBadge) {
            return;
        }
        mForceHideBadge = forceHideBadge;

        if (forceHideBadge) {
            invalidate();
        } else if (hasBadge()) {
            ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, 0, 1).start();
        }
    }

    private boolean hasBadge() {
        return mBadgeInfo != null;
    }

    public void getIconBounds(Rect outBounds) {
        int top = getPaddingTop();
        int left = (getWidth() - mIconSize) / 2;
        int right = left + mIconSize;
        int bottom = top + mIconSize;
        outBounds.set(left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCenterVertically) {
            Paint.FontMetrics fm = getPaint().getFontMetrics();
            int cellHeightPx = mIconSize + getCompoundDrawablePadding() +
                    (int) Math.ceil(fm.bottom - fm.top) * 2;
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setPadding(getPaddingLeft(), Math.max(0, (height - cellHeightPx) / 2), getPaddingRight(),
                    getPaddingBottom());
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setTextColor(int color) {
        mTextColor = color;
        super.setTextColor(getModifiedColor());
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        mTextColor = colors.getDefaultColor();
        if (Float.compare(mTextAlpha, 1) == 0) {
            super.setTextColor(colors);
        } else {
            super.setTextColor(getModifiedColor());
        }
    }

    public boolean shouldTextBeVisible() {
        // Text should be visible everywhere but the hotseat.
        Object tag = getParent() instanceof FolderIcon ? ((View) getParent()).getTag() : getTag();
        ItemInfo info = tag instanceof ItemInfo ? (ItemInfo) tag : null;
        return info == null || info.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT;
    }

    public void setTextVisibility(boolean visible) {
        setTextAlpha(visible ? 1 : 0);
    }

    public void setTextAlpha(float alpha) {
        mTextAlpha = alpha;
        super.setTextColor(getModifiedColor());
    }

    private int getModifiedColor() {
        if (mTextAlpha == 0) {
            // Special case to prevent text shadows in high contrast mode
            return Color.TRANSPARENT;
        }
        return ColorUtils.setAlphaComponent(
                mTextColor, Math.round(Color.alpha(mTextColor) * mTextAlpha));
    }

    /**
     * Creates an animator to fade the text in or out.
     * @param fadeIn Whether the text should fade in or fade out.
     */
    public ObjectAnimator createTextAlphaAnimator(boolean fadeIn) {
        float toAlpha = shouldTextBeVisible() && fadeIn ? 1 : 0;
        return ObjectAnimator.ofFloat(this, TEXT_ALPHA_PROPERTY, toAlpha);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    public void applyPromiseState(boolean promiseStateChanged) {
        if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();
            final boolean isPromise = info.hasPromiseIconUi();
            final int progressLevel = isPromise ?
                    ((info.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE) ?
                            info.getInstallProgress() : 0)) : 100;

            PreloadIconDrawable preloadDrawable = applyProgressLevel(progressLevel);
            if (preloadDrawable != null && promiseStateChanged) {
                preloadDrawable.maybePerformFinishedAnimation();
            }
        }
    }

    public PreloadIconDrawable applyProgressLevel(int progressLevel) {
        if (getTag() instanceof ItemInfoWithIcon) {
            ItemInfoWithIcon info = (ItemInfoWithIcon) getTag();
            if (progressLevel >= 100) {
                setContentDescription(info.contentDescription != null
                        ? info.contentDescription : "");
            } else if (progressLevel > 0) {
                setContentDescription(getContext()
                        .getString(R.string.app_downloading_title, info.title,
                                NumberFormat.getPercentInstance().format(progressLevel * 0.01)));
            } else {
                setContentDescription(getContext()
                        .getString(R.string.app_waiting_download_title, info.title));
            }
            if (mIcon != null) {
                final PreloadIconDrawable preloadDrawable;
                if (mIcon instanceof PreloadIconDrawable) {
                    preloadDrawable = (PreloadIconDrawable) mIcon;
                    preloadDrawable.setLevel(progressLevel);
                } else {
                    preloadDrawable = DrawableFactory.get(getContext())
                            .newPendingIcon(info, getContext());
                    preloadDrawable.setLevel(progressLevel);
                    setIcon(preloadDrawable);
                }
                return preloadDrawable;
            }
        }
        return null;
    }

    public void applyBadgeState(ItemInfo itemInfo, boolean animate) {
        if (mIcon instanceof FastBitmapDrawable) {
            boolean wasBadged = mBadgeInfo != null;
            mBadgeInfo = mActivity.getBadgeInfoForItem(itemInfo);
            boolean isBadged = mBadgeInfo != null;
            float newBadgeScale = isBadged ? 1f : 0;
            mBadgeRenderer = mActivity.getDeviceProfile().mBadgeRenderer;
            if (wasBadged || isBadged) {
                // Animate when a badge is first added or when it is removed.
                if (animate && (wasBadged ^ isBadged) && isShown()) {
                    ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, newBadgeScale).start();
                } else {
                    mBadgeScale = newBadgeScale;
                    invalidate();
                }
            }
            if (itemInfo.contentDescription != null) {
                if (hasBadge()) {
                    int count = mBadgeInfo.getNotificationCount();
                    setContentDescription(getContext().getResources().getQuantityString(
                            R.plurals.badged_app_label, count, itemInfo.contentDescription, count));
                } else {
                    setContentDescription(itemInfo.contentDescription);
                }
            }
        }
    }

    /**
     * Sets the icon for this view based on the layout direction.
     */
    public void setIcon(Drawable icon) {
        if (mIsIconVisible) {
            applyCompoundDrawables(icon);
        }
        mIcon = icon;
    }

    public void setIconVisible(boolean visible) {
        mIsIconVisible = visible;
        Drawable icon = visible ? mIcon : new ColorDrawable(Color.TRANSPARENT);
        applyCompoundDrawables(icon);
    }

    protected void applyCompoundDrawables(Drawable icon) {
        // If we had already set an icon before, disable relayout as the icon size is the
        // same as before.
        mDisableRelayout = mIcon != null;

        icon.setBounds(0, 0, mIconSize, mIconSize);
        if (mLayoutHorizontal) {
            setCompoundDrawablesRelative(icon, null, null, null);
        } else {
            setCompoundDrawables(null, icon, null, null);
        }
        mDisableRelayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mDisableRelayout) {
            super.requestLayout();
        }
    }

    /**
     * Applies the item info if it is same as what the view is pointing to currently.
     */
    @Override
    public void reapplyItemInfo(ItemInfoWithIcon info) {
        if (getTag() == info) {
            mIconLoadRequest = null;
            mDisableRelayout = true;

            // Optimization: Starting in N, pre-uploads the bitmap to RenderThread.
            info.iconBitmap.prepareToDraw();

            if (info instanceof AppInfo) {
                applyFromApplicationInfo((AppInfo) info);
            } else if (info instanceof ShortcutInfo) {
                applyFromShortcutInfo((ShortcutInfo) info);
                mActivity.invalidateParent(info);
            } else if (info instanceof PackageItemInfo) {
                applyFromPackageItemInfo((PackageItemInfo) info);
            }

            mDisableRelayout = false;
        }
    }

    /**
     * Verifies that the current icon is high-res otherwise posts a request to load the icon.
     */
    public void verifyHighRes() {
        if (mIconLoadRequest != null) {
            mIconLoadRequest.cancel();
            mIconLoadRequest = null;
        }
        if (getTag() instanceof ItemInfoWithIcon) {
            ItemInfoWithIcon info = (ItemInfoWithIcon) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance(getContext()).getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        }
    }

    public int getIconSize() {
        return mIconSize;
    }

    //liuzuo:add for addIcon:begin
    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        Log.d(TAG,"setChecked");
        isChecked = checked;
        invalidate();
    }
    private Drawable getCheckDrawable() {
        if (isChecked) {
            if(mCheckDrawable ==null) {
                final Resources res = getContext().getResources();
                int width = mIconSize;
                int widthOfmCheckDrawable;
                mCheckDrawable = res.getDrawable(R.drawable.zzz_ic_check);
                float scale = 95/ 100f;
                widthOfmCheckDrawable = mCheckDrawable.getIntrinsicWidth();
                int left = (int) (((getWidth() - width) / 2 + width)*scale) - widthOfmCheckDrawable / 2 - 1;
                /* int top = getPaddingTop() + (widthOfmCheckDrawable/2 - width) / 2 - widthOfmCheckDrawable / 2 + 1;*/
                //int top = (int) ((getHeight()-textSize-width)/4.5*scale);
                int top = getPaddingTop()-widthOfmCheckDrawable/2;

                top = top <= 0 ? 0 : top;
                int right = left + widthOfmCheckDrawable;
                int buttom = top + widthOfmCheckDrawable;
                if (right > getWidth()) {
                    left = getWidth() - widthOfmCheckDrawable;
                    right = getWidth();
                } else {

                }

                mCheckDrawable.setBounds(left, top, right, buttom);
            }
            return mCheckDrawable;
        }else {

            return null;
        }
    }

    private void drawCheckDrawable(Drawable checkDrawable, Canvas canvas) {
        if(checkDrawable!=null){

            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            if ((scrollX | scrollY) == 0) {
                checkDrawable.draw(canvas);
            } else {
                canvas.save();
                canvas.translate(scrollX, scrollY);
                checkDrawable.draw(canvas);
                canvas.restore();
            }
        }
    }

    // Perry: Add live icon: start
    @Override
    public void onIconUpdated() {
        post(() -> {
            if (getTag() instanceof ItemInfoWithIcon) {
                applyIconAndLabel((ItemInfoWithIcon) getTag());
            }
        });
    }
    // Perry: Add live icon: end
    //M:liuzuo:add the folderImportMode:begin
  /*  public void updateFolderIcon() {
        if(getTag() instanceof ShortcutInfo){
            ShortcutInfo shortcutInfo = (ShortcutInfo) getTag();
            Workspace workspace = mLauncher.getWorkspace();
            if(shortcutInfo!=null&&shortcutInfo.rank<FolderIcon.NUM_ITEMS_IN_PREVIEW&&shortcutInfo.container>0&&workspace!=null){
                final ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                        workspace.getAllShortcutAndWidgetContainers();
                int childCount ;
                View view ;
                Object tag ;
                for (ShortcutAndWidgetContainer layout : childrenLayouts) {
                    childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        view = layout.getChildAt(j);
                        tag = view.getTag();
                        if (tag instanceof FolderInfo) {
                            FolderInfo folderInfo= (FolderInfo) tag;
                            if(folderInfo.contents.contains(shortcutInfo)) {
                                Log.d(TAG,"((FolderIcon) view).invalidate()");
                                FolderIcon folderIcon = (FolderIcon) view;
                                folderIcon.getPreviewItemManager().updateItemDrawingParams(false);
                                folderIcon.invalidate();
                            }
                        }
                    }
                }
            }
        }
    }*/
    //liuzuo:add for addIcon:end
    //Bruce : add for unread message : start
    public Rect getIconRect() {
        int iconSize = mIconSize;
        Point center = new Point(getScrollX() + (getWidth() >> 1),
                getScrollY() + getPaddingTop() + (iconSize >> 1));
        Rect iconRect = new Rect();

        iconRect.left   = center.x - (iconSize >> 1);
        iconRect.top    = center.y - (iconSize >> 1);
        iconRect.right  = iconRect.left + iconSize;
        iconRect.bottom = iconRect.top + iconSize;
        return iconRect;
    }

    private int mUnreadCount = -1;
    private int mUnreadType = -1;
    private boolean supportDrawUnread() {
        switch (mUnreadType) {
            case UnreadInfoManager.TYPE_CALL_LOG:
                return CustomTools.mSwitchUnreadPhone;
            case UnreadInfoManager.TYPE_SMS:
                return CustomTools.mSwitchUnreadSms;
            default:
                break;
        }
        return false;

    }

    public void drawUnreadIcon(int unreadCount, int type) {
        mUnreadCount = unreadCount;
        mUnreadType = type;
        //Log.d("liuzuo98","drawUnreadIcon  mUnreadCount="+mUnreadCount+"  ,mUnreadType="+mUnreadType);
        invalidate();
    }
    //Bruce : add for unread message : end

}
