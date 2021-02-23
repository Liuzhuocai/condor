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

import static android.view.MotionEvent.ACTION_DOWN;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.CellLayout.ContainerType;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

public class ShortcutAndWidgetContainer extends ViewGroup {
    static final String TAG = "ShortcutAndWidgetContainer";

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private final int[] mTmpCellXY = new int[2];

    @ContainerType private final int mContainerType;
    private final WallpaperManager mWallpaperManager;

    private int mCellWidth;
    private int mCellHeight;

    private int mCountX;

    private Launcher mLauncher;
    private boolean mInvertIfRtl = false;

    public ShortcutAndWidgetContainer(Context context, @ContainerType int containerType) {
        super(context);
        mLauncher = Launcher.getLauncher(context);
        mWallpaperManager = WallpaperManager.getInstance(context);
        mContainerType = containerType;
    }

    public void setCellDimensions(int cellWidth, int cellHeight, int countX, int countY) {
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
        mCountX = countX;
    }

    public View getChildAt(int x, int y) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

            if ((lp.cellX <= x) && (x < lp.cellX + lp.cellHSpan) &&
                    (lp.cellY <= y) && (y < lp.cellY + lp.cellVSpan)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSpecSize, heightSpecSize);

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child);
            }
        }
    }

    public void setupLp(View child) {
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        if (child instanceof LauncherAppWidgetHostView) {
            DeviceProfile profile = mLauncher.getDeviceProfile();
            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX,
                    profile.appWidgetScale.x, profile.appWidgetScale.y);
        } else {
            // Perry: Hotseat reorder function: start
            if (mContainerType == CellLayout.HOTSEAT) {
                lp.leftMargin = lp.rightMargin = Math.max(0, (mCellWidth - getMinCellSize()) / 2);
            } else {
                lp.leftMargin = lp.rightMargin = 0;
            }
            // Perry: Hotseat reorder function: end

            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX);
        }
    }

    // Set whether or not to invert the layout horizontally if the layout is in RTL mode.
    public void setInvertIfRtl(boolean invert) {
        mInvertIfRtl = invert;
    }

    public int getCellContentHeight() {
        return Math.min(getMeasuredHeight(),
                mLauncher.getDeviceProfile().getCellHeight(mContainerType));
    }

    public void measureChild(View child) {
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        final DeviceProfile profile = mLauncher.getDeviceProfile();

        if (child instanceof LauncherAppWidgetHostView) {
            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX,
                    profile.appWidgetScale.x, profile.appWidgetScale.y);
            // Widgets have their own padding
        } else {
            // Perry: Hotseat reorder function: start
            if (mContainerType == CellLayout.HOTSEAT) {
                lp.leftMargin = lp.rightMargin = Math.max(0, (mCellWidth - getMinCellSize()) / 2);
            } else {
                lp.leftMargin = lp.rightMargin = 0;
            }
            // Perry: Hotseat reorder function: end

            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX);
            // Center the icon/folder
            int cHeight = getCellContentHeight();
            int cellPaddingY = (int) Math.max(0, ((lp.height - cHeight) / 2f));
            int cellPaddingX = mContainerType == CellLayout.WORKSPACE
                    ? profile.workspaceCellPaddingXPx
                    : (int) (profile.edgeMarginPx / 2f);
            child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, 0);
        }
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        child.measure(childWidthMeasureSpec, childheightMeasureSpec);
    }

    public boolean invertLayoutHorizontally() {
        return mInvertIfRtl && Utilities.isRtl(getResources());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                if (child instanceof LauncherAppWidgetHostView) {
                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) child;

                    // Scale and center the widget to fit within its cells.
                    DeviceProfile profile = mLauncher.getDeviceProfile();
                    float scaleX = profile.appWidgetScale.x;
                    float scaleY = profile.appWidgetScale.y;

                    lahv.setScaleToFit(Math.min(scaleX, scaleY));
                    lahv.setTranslationForCentering(-(lp.width - (lp.width * scaleX)) / 2.0f,
                            -(lp.height - (lp.height * scaleY)) / 2.0f);
                }

                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                if (lp.dropped) {
                    lp.dropped = false;

                    final int[] cellXY = mTmpCellXY;
                    getLocationOnScreen(cellXY);
                    mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                            WallpaperManager.COMMAND_DROP,
                            cellXY[0] + childLeft + lp.width / 2,
                            cellXY[1] + childTop + lp.height / 2, 0, null);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == ACTION_DOWN && getAlpha() == 0) {
            // Dont let children handle touch, if we are not visible.
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    // Perry: locate application function: start
    public BubbleTextView findShortcutByAppInfo(AppInfo app) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            ItemInfo info = (ItemInfo) view.getTag();

            if (info instanceof ShortcutInfo) {
                BubbleTextView shortcut = (BubbleTextView) view;
                if (app.getTargetComponent().equals(info.getTargetComponent())
                        && app.user.equals(info.user)) {
                    return shortcut;
                }
            } else if (info instanceof FolderInfo) {
                if (view instanceof FolderIcon) {
                    Folder folder = ((FolderIcon)view).getFolder();
                    if (folder != null) {
                        BubbleTextView shortcut = folder.findShortcutByAppInfo(app);
                        if (shortcut != null) {
                            return shortcut;
                        }
                    }
                }
            }
        }

        return null;
    }

    public FolderIcon findFolderIconById(long id) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            ItemInfo info = (ItemInfo) view.getTag();
            if (view instanceof FolderIcon && info.id == id) {
                return (FolderIcon) view;
            }
        }
        return null;
    }
    // Perry: locate application function: end

    // Perry: Hotseat reorder function: start
    private int getMinCellSize() {
        if (getParent() instanceof CellLayout) {
            return ((CellLayout) getParent()).getMinCellSize();
        }

        return mLauncher.getDeviceProfile().cellWidthPx;
    }
    // Perry: Hotseat reorder function: end
}
