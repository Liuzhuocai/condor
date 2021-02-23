package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class HotseatCellLayout extends CellLayout {
    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;
    private boolean mIsVerticalBarLayout = false;

    public HotseatCellLayout(Context context) {
        super(context);
    }

    public HotseatCellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HotseatCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params, boolean markCells) {
        if (mIsVerticalBarLayout) {
            if (params.cellY >= getMaxCount()) {
                return false;
            } else if (params.cellY >= getCountY()) {
                setGridSize(1, params.cellY + 1);
            }
        } else {
            if (params.cellX >= getMaxCount()) {
                return false;
            } else if (params.cellX >= getCountX()) {
                setGridSize(params.cellX + 1, 1);
            }
        }

        return super.addViewToCellLayout(child, index, childId, params, markCells);
    }

    @Override
    public void setGridSize(int x, int y) {
        if (mIsVerticalBarLayout) {
            if (y > getMaxCount()) {
                return;
            }

            if (y <= 0) {
                y = 1;
            }
        } else {
            if (x > getMaxCount()) {
                return;
            }

            if (x <= 0) {
                x = 1;
            }
        }

        super.setGridSize(x, y);
        refreshOccupiedCells();
    }

    @Override
    public void arrangeChildren() {
        setUseTempCoords(false);
        int[] emptyCell = findEmptyCell();

        if (emptyCell[0] < 0 || emptyCell[1] < 0) {
            return;
        }
        if (mIsVerticalBarLayout) {
            setGridSize(1, getItemsCount());
        } else {
            setGridSize(getItemsCount(), 1);
        }
        realTimeReorder(emptyCell);
        updateItemsInDatabase();
    }

    @Override
    public void cellToPoint(int cellX, int cellY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        final int[] cellSize = new int[2];
        calculateCellSize(cellSize, getCountX(), getCountY());
        result[0] = hStartPadding + cellX * cellSize[0];
        result[1] = vStartPadding + cellY * cellSize[1];
    }

    @Override
    protected void cellToPointWithMargin(int cellX, int cellY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        final int[] cellSize = new int[2];
        calculateCellSize(cellSize, getCountX(), getCountY());
        if (mIsVerticalBarLayout) {
            result[0] = hStartPadding + cellX * cellSize[0];
            result[1] = vStartPadding + cellY * cellSize[1] + Math.max(0, (cellSize[1] - getMinCellSize()) / 2);
        } else {
            result[0] = hStartPadding + cellX * cellSize[0] + Math.max(0, (cellSize[0] - getMinCellSize()) / 2);
            result[1] = vStartPadding + cellY * cellSize[1];
        }
    }

    @Override
    public float getDistanceFromCell(float x, float y, int[] cell) {
        cellToCenterPoint(cell[0], cell[1], 1, 1, mTmpPoint);
        return (float) Math.hypot(x - mTmpPoint[0], y - mTmpPoint[1]);
    }

    @Override
    public int getMinCellSize() {
        if (getWidth() == 0) {
            return super.getMinCellSize();
        }

        int childWidthSize = mLauncher.getDeviceProfile().availableWidthPx - getPaddingLeft() - getPaddingRight();
        return DeviceProfile.calculateCellWidth(childWidthSize, 4);
    }

    private int[] findEmptyCell() {
        int[] emptyCell = new int[] {-1, -1};
        for (int y = getCountY() - 1; y >= 0; y--) {
            for (int x = 0; x < getCountX(); x++) {
                if (getChildAt(x, y) == null) {
                    emptyCell[0] = x;
                    emptyCell[1] = y;
                    return emptyCell;
                }
            }
        }
        return emptyCell;
    }

    private void realTimeReorder(int[] empty) {
        int start;
        int delay = 0;
        float delayAmount = START_VIEW_REORDER_DELAY;

        if (mIsVerticalBarLayout) {
            for (int i = empty[1] - 1; i >= 0; i--) {
                View v = getChildAt(empty[0], i);
                if (animateChildToPosition(v, empty[0], i, REORDER_ANIMATION_DURATION, 0, true, true)) {
                    delay += delayAmount;
                    delayAmount *= VIEW_REORDER_DELAY_FACTOR;
                }
            }

            start = empty[1];
            for (int y = start + 1; y <= getCountY(); y++) {
                View v = getChildAt(empty[0], y);
                if (animateChildToPosition(v, empty[0], start, REORDER_ANIMATION_DURATION, delay, true, true)) {
                    start = y;
                    delay += delayAmount;
                    delayAmount *= VIEW_REORDER_DELAY_FACTOR;
                }
            }
        } else {
            for (int i = empty[0] - 1; i >= 0; i--) {
                View v = getChildAt(i, empty[1]);
                if (animateChildToPosition(v, i, empty[1], REORDER_ANIMATION_DURATION, 0, true, true)) {
                    delay += delayAmount;
                    delayAmount *= VIEW_REORDER_DELAY_FACTOR;
                }
            }

            start = empty[0];
            for (int x = start + 1; x <= getCountX(); x++) {
                View v = getChildAt(x, empty[1]);
                if (animateChildToPosition(v, start, empty[1], REORDER_ANIMATION_DURATION, delay, true, true)) {
                    start = x;
                    delay += delayAmount;
                    delayAmount *= VIEW_REORDER_DELAY_FACTOR;
                }
            }
        }
    }

    private void updateItemsInDatabase() {
        if (mIsVerticalBarLayout) {
            for (int i = 0; i < getCountY(); i++) {
                View v = getChildAt(0, i);
                if (v != null) {
                    ItemInfo info = (ItemInfo) v.getTag();
                    mLauncher.getModelWriter().modifyItemInDatabase(
                            info, LauncherSettings.Favorites.CONTAINER_HOTSEAT, -1, info.cellX,
                            info.cellY, info.spanX, info.spanY);
                }
            }
        } else {
            for (int i = 0; i < getCountX(); i++) {
                View v = getChildAt(i, 0);
                if (v != null) {
                    ItemInfo info = (ItemInfo) v.getTag();
                    mLauncher.getModelWriter().modifyItemInDatabase(
                            info, LauncherSettings.Favorites.CONTAINER_HOTSEAT, -1, info.cellX,
                            info.cellY, info.spanX, info.spanY);
                }
            }
        }
    }

    private void cellToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        if (getItemsCount() <= 0) {
            super.regionToCenterPoint(cellX, cellY, spanX, spanY, result);
            return;
        }

        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        final int[] cellSize = new int[2];
        if (mIsVerticalBarLayout) {
            calculateCellSize(cellSize, 1, getItemsCount());
        } else {
            calculateCellSize(cellSize, getItemsCount(), 1);
        }
        result[0] = hStartPadding + cellX * cellSize[0] + (spanX * cellSize[0]) / 2;
        result[1] = vStartPadding + cellY * cellSize[1] + (spanY * cellSize[1]) / 2;
    }

    public int getMaxCount() {
        return mLauncher.getDeviceProfile().inv.numHotseatIcons;
    }

    public long getReorderTimeout() {
        if (getItemsCount() <= 2) {
            return Workspace.REORDER_TIMEOUT;
        }

        return Workspace.REORDER_TIMEOUT / getItemsCount() * 2;
    }

    public int[] getRealCellSize() {
        final int[] cellSize = new int[2];
        if (mIsVerticalBarLayout) {
            calculateCellSize(cellSize, 1, Math.max(getItemsCount(), 1));
        } else {
            calculateCellSize(cellSize, Math.max(getItemsCount(), 1), 1);
            cellSize[0] = Math.min(cellSize[0], getMinCellSize());
        }

        return cellSize;
    }

    public void setVerticalBarLayout(boolean verticalBarLayout) {
        mIsVerticalBarLayout = verticalBarLayout;
    }
}
