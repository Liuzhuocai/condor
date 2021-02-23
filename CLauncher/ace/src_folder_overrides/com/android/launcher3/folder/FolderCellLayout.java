/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.folder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.touch.ItemClickHandler;

import java.util.ArrayList;

public class FolderCellLayout extends CellLayout {

    private static final String TAG = "FolderPagedView";

    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;

    private static final int[] sTmpArray = new int[2];

    public final boolean mIsRtl;

    private final LayoutInflater mInflater;
    private final ViewGroupFocusHelper mFocusIndicatorHelper;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxCountX;
    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxCountY;

    private int mAllocatedContentSize;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountX;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountY;

    private Folder mFolder;
    private int mHoverCount;

    public FolderCellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        InvariantDeviceProfile profile = LauncherAppState.getIDP(context);
        mMaxCountX = profile.numFolderColumns;
        mMaxCountY = profile.numFolderRows;

        DeviceProfile grid = Launcher.getLauncher(getContext()).getDeviceProfile();
        setCellDimensions(grid.folderCellWidthPx, grid.folderCellHeightPx);
        getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        setInvertIfRtl(true);

        mInflater = LayoutInflater.from(context);

        mIsRtl = Utilities.isRtl(getResources());
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        mFocusIndicatorHelper = new ViewGroupFocusHelper(this);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    /**
     * Calculates the grid size such that {@param count} items can fit in the grid.
     * The grid size is calculated such that countY <= countX and countX = ceil(sqrt(count)) while
     * maintaining the restrictions of {@link #mMaxCountX} &amp; {@link #mMaxCountY}.
     */
    public static void calculateGridSize(int count, int countX, int countY, int maxCountX,
            int maxCountY, int maxItemsPerPage, int[] out) {
        boolean done;
        int gridCountX = countX;
        int gridCountY = countY;

        if (count >= maxItemsPerPage) {
            gridCountX = maxCountX;
            gridCountY = maxCountY;
            done = true;
        } else {
            done = false;
        }

        while (!done) {
            int oldCountX = gridCountX;
            int oldCountY = gridCountY;
            if (gridCountX * gridCountY < count) {
                // Current grid is too small, expand it
                if ((gridCountX <= gridCountY || gridCountY == maxCountY)
                        && gridCountX < maxCountX) {
                    gridCountX++;
                } else if (gridCountY < maxCountY) {
                    gridCountY++;
                }
                if (gridCountY == 0) gridCountY++;
            } else if ((gridCountY - 1) * gridCountX >= count && gridCountY >= gridCountX) {
                gridCountY = Math.max(0, gridCountY - 1);
            } else if ((gridCountX - 1) * gridCountY >= count) {
                gridCountX = Math.max(0, gridCountX - 1);
            }
            done = gridCountX == oldCountX && gridCountY == oldCountY;
        }

        out[0] = gridCountX;
        out[1] = gridCountY;
    }

    /**
     * Sets up the grid size such that {@param count} items can fit in the grid.
     */
    public void setupContentDimensions(int count) {
        mAllocatedContentSize = count;
        mGridCountX = mMaxCountX;
        mGridCountY = (int)Math.ceil(count / (float)mMaxCountX);

        mHoverCount = Math.min(mMaxCountY / 2, mGridCountY);

        // Update grid size
        setGridSize(mGridCountX, mGridCountY);

        mFolder.updateBackgroundBounds();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mFocusIndicatorHelper.draw(canvas);
        super.dispatchDraw(canvas);
    }

   /* @Override
    public void regionToScaleRect(int cellX, int cellY, int spanX, int spanY, float scale, Rect result) {
        super.regionToScaleRect(Math.min(cellX, mMaxCountX-1),
                Math.min(cellY, getMaxShowCountY()-1), spanX, spanY, scale, result);
    }*/

    public int getMaxShowCountY() {
        if (mFolder.mLauncher.isInEditMode()) {
            return 3;
        }

        return mMaxCountY;
    }

    /**
     * Binds items to the layout.
     */
    public void bindItems(ArrayList<ShortcutInfo> items) {
        ArrayList<View> icons = new ArrayList<>();
        for (ShortcutInfo item : items) {
            icons.add(createNewView(item));
        }
        arrangeChildren(icons, icons.size(), false);
    }

    public void allocateSpaceForRank(int rank) {
        ArrayList<View> views = new ArrayList<>(mFolder.getItemsInReadingOrder());
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
    }

    /**
     * Create space for a new item at the end, and returns the rank for that item.
     * Also sets the current page to the last page.
     */
    public int allocateRankForNewItem() {
        int rank = getItemCount();
        allocateSpaceForRank(rank);
        return rank;
    }

    public View createAndAddViewForRank(ShortcutInfo item, int rank) {
        View icon = createNewView(item);
        allocateSpaceForRank(rank);
        addViewForRank(icon, item, rank);
        return icon;
    }

    /**
     * Adds the {@param view} to the layout based on {@param rank} and updated the position
     * related attributes. It assumes that {@param item} is already attached to the view.
     */
    public void addViewForRank(View view, ShortcutInfo item, int rank) {
        item.rank = rank;
        item.cellX = rank % mGridCountX;
        item.cellY = rank / mGridCountX;

        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = item.cellX;
        lp.cellY = item.cellY;
        addViewToCellLayout(
                view, -1, mFolder.mLauncher.getViewIdForItem(item), lp, true);
    }

    public void findRankCoordinates(int rank, int[] coordinates) {
        coordinates[0] = rank % mGridCountX;
        coordinates[1] = rank / mGridCountX;
    }

    @SuppressLint("InflateParams")
    public View createNewView(ShortcutInfo item) {
        final BubbleTextView textView = (BubbleTextView) mInflater.inflate(
                R.layout.folder_application, null, false);
        textView.applyFromShortcutInfo(item);
        textView.setHapticFeedbackEnabled(false);
        textView.setOnClickListener(ItemClickHandler.INSTANCE);
        textView.setOnLongClickListener(mFolder);
        textView.setOnFocusChangeListener(mFocusIndicatorHelper);

        textView.setLayoutParams(new CellLayout.LayoutParams(
                item.cellX, item.cellY, item.spanX, item.spanY));
        return textView;
    }

    public CellLayout getCurrentCellLayout() {
        return this;
    }

    /**
     * Updates position and rank of all the children in the view.
     * It essentially removes all views from all the pages and then adds them again in appropriate
     * page.
     *
     * @param list the ordered list of children.
     * @param itemCount if greater than the total children count, empty spaces are left
     * at the end, otherwise it is ignored.
     *
     */
    public void arrangeChildren(ArrayList<View> list, int itemCount) {
        arrangeChildren(list, itemCount, true);
    }

    @SuppressLint("RtlHardcoded")
    private void arrangeChildren(ArrayList<View> list, int itemCount, boolean saveChanges) {
        removeAllViews();
        setupContentDimensions(itemCount);

        int position = 0;
        int newX, newY, rank;

        rank = 0;
        for (int i = 0; i < itemCount; i++) {
            View v = list.size() > i ? list.get(i) : null;
            if (v != null) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                newX = position % mGridCountX;
                newY = position / mGridCountX;
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.cellX != newX || info.cellY != newY || info.rank != rank) {
                    info.cellX = newX;
                    info.cellY = newY;
                    info.rank = rank;
                    if (saveChanges) {
                        mFolder.mLauncher.getModelWriter().addOrMoveItemInDatabase(info,
                                mFolder.mInfo.id, 0, info.cellX, info.cellY);
                    }
                }
                lp.cellX = info.cellX;
                lp.cellY = info.cellY;
                addViewToCellLayout(
                        v, -1, mFolder.mLauncher.getViewIdForItem(info), lp, true);

                if (v instanceof BubbleTextView) {
                    ((BubbleTextView) v).verifyHighRes();
                }
            }

            rank ++;
            position++;
        }
    }

    public int getItemCount() {
        return getShortcutsAndWidgets().getChildCount();
    }

    /**
     * @return the rank of the cell nearest to the provided pixel position.
     */
    public int findNearestArea(int pixelX, int pixelY) {
        findNearestArea(pixelX, pixelY, 1, 1, sTmpArray);
        if (mFolder.isLayoutRtl()) {
            sTmpArray[0] = getCountX() - sTmpArray[0] - 1;
        }
        return Math.min(mAllocatedContentSize - 1,
                sTmpArray[1] * mGridCountX + sTmpArray[0]);
    }

    public View getFirstItem() {
        ShortcutAndWidgetContainer currContainer = getShortcutsAndWidgets();
        if (mGridCountX > 0) {
            return currContainer.getChildAt(0, 0);
        } else {
            return currContainer.getChildAt(0);
        }
    }

    public View getLastItem() {
        ShortcutAndWidgetContainer currContainer = getShortcutsAndWidgets();
        int lastRank = currContainer.getChildCount() - 1;
        if (mGridCountX > 0) {
            return currContainer.getChildAt(lastRank % mGridCountX, lastRank / mGridCountX);
        } else {
            return currContainer.getChildAt(lastRank);
        }
    }

    /**
     * Iterates over all its items in a reading order.
     * @return the view for which the operator returned true.
     */
    public View iterateOverItems(Workspace.ItemOperator op) {
        for (int j = 0; j < getCountY(); j++) {
            for (int i = 0; i < getCountX(); i++) {
                View v = getChildAt(i, j);
                if ((v != null) && op.evaluate((ItemInfo) v.getTag(), v)) {
                    return v;
                }
            }
        }
        return null;
    }

    public String getAccessibilityDescription() {
        return getContext().getString(R.string.folder_opened, mGridCountX, mGridCountY);
    }

    /**
     * Sets the focus on the first visible child.
     */
    public void setFocusOnFirstChild() {
        View firstChild = getCurrentCellLayout().getChildAt(0, 0);
        if (firstChild != null) {
            firstChild.requestFocus();
        }
    }

    /**
     * Ensures that all the icons on the given page are of high-res
     */
    public void verifyVisibleHighResIcons() {
        ShortcutAndWidgetContainer parent = getShortcutsAndWidgets();
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            BubbleTextView icon = ((BubbleTextView) parent.getChildAt(i));
            icon.verifyHighRes();
            // Set the callback back to the actual icon, in case
            // it was captured by the FolderIcon
            Drawable d = icon.getCompoundDrawables()[1];
            if (d != null) {
                d.setCallback(icon);
            }
        }
    }

    public int getAllocatedContentSize() {
        return mAllocatedContentSize;
    }

    /**
     * Reorders the items such that the {@param empty} spot moves to {@param target}
     */
    public void realTimeReorder(int empty, int target) {
        int delay = 0;
        float delayAmount = START_VIEW_REORDER_DELAY;

        if (target == empty) {
            return;
        }

        int startPos = empty, endPos = target;
        int direction = target > empty ? 1 : -1;

        if ((endPos - startPos) * direction <= 0) {
            return;
        }

        for (int i = startPos; i != endPos; i += direction) {
            int nextPos = i + direction;
            View v = getChildAt(nextPos % mGridCountX, nextPos / mGridCountX);
            if (v != null) {
                ((ItemInfo) v.getTag()).rank -= direction;
            }
            if (animateChildToPosition(v, i % mGridCountX, i / mGridCountX,
                    REORDER_ANIMATION_DURATION, delay, true, true)) {
                delay += delayAmount;
                delayAmount *= VIEW_REORDER_DELAY_FACTOR;
            }
        }
    }

    public int getMaxCountHeight() {
        return getPaddingTop() + getPaddingBottom() + getMaxShowCountY() * getCellHeight();
    }

    public int getHoverTopPadding() {
        return -(((mMaxCountY - mHoverCount) * getCellHeight()));
    }

    public int getDisplayHeight() {
        if (mFolder.isHovering()) {
            return getPaddingTop() + getPaddingBottom() + mHoverCount * getCellHeight();
        }

        return Math.min(getDesiredHeight(), getMaxCountHeight());
    }

    public int getPositionOfChild(BubbleTextView child) {
        if (child == null) {
            return 0;
        }

        ItemInfo info = (ItemInfo) child.getTag();
        if (info != null) {
            int maxShowCountY = getMaxShowCountY();
            if (info.cellY < maxShowCountY) {
                return 0;
            }
            return getPaddingTop() + (info.cellY - maxShowCountY + 1) * getCellHeight();
        }
        return 0;
    }

    public boolean isFull() {
        int count = getItemCount();
        return count / mMaxCountX >= getMaxShowCountY()
                && count % mMaxCountX == 0;
    }
}
