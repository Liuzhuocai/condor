package com.condor.launcher.arrange;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.LongSparseArray;

import com.android.launcher3.AllAppsList;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.model.AddWorkspaceItemsTask;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.util.GridOccupancy;

import java.util.ArrayList;

public class ScreenArrangeTask extends AddWorkspaceItemsTask {
    private final long mScreenId;
    private final boolean mAnimate;

    public ScreenArrangeTask(long screenId) {
        this(screenId, false);
    }

    public ScreenArrangeTask(long screenId, boolean animate) {
        super(null);
        mScreenId = screenId;
        mAnimate  = animate;
    }

    @Override
    public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
        final Context context = app.getContext();
        final LongSparseArray<ItemInfo> movedItemsFinal = new LongSparseArray<>();
        try {
            arrangeScreenItems(app, dataModel, movedItemsFinal);
            updateWorkspaceItems(context, movedItemsFinal);
            scheduleCallbackTask(cb -> {cb.bindScreenItemsMoved(mScreenId, movedItemsFinal, mAnimate);});
        } catch (RuntimeException e) {
        }
    }

    public void arrangeScreenItems(LauncherAppState app, BgDataModel dataModel,
                                   LongSparseArray<ItemInfo> movedItemsFinal) {
        synchronized (dataModel) {
            int pageIndex = dataModel.workspaceScreens.indexOf(mScreenId);
            if (pageIndex < mStartPage) {
                return;
            }

            ArrayList<ItemInfo> items = new ArrayList<>();
            for (ItemInfo info : dataModel.itemsIdMap) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                        && info.screenId == mScreenId) {
                    items.add(info);
                }
            }

            if (items.isEmpty()) {
                return;
            }

            final InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            final GridOccupancy occupied = new GridOccupancy(profile.numColumns, profile.numRows);
            final int[] coords = new int[2];

            items.stream().sorted((o1, o2)-> {
                if (o1.cellY != o2.cellY) {
                    return o1.cellY - o2.cellY;
                } else {
                    return o1.cellX - o2.cellX;
                }
            }).forEach(info-> {
                if (occupied.findVacantCell(coords, info.spanX, info.spanY)) {
                    if (info.cellX != coords[0] || info.cellY != coords[1]) {
                        info.cellX = coords[0];
                        info.cellY = coords[1];
                        movedItemsFinal.put(info.id, info);
                    }
                } else {
                    throw new RuntimeException("Arrange screen " + mScreenId + " error");
                }
                occupied.markCells(info, true);
            });
        }
    }

    public void updateWorkspaceItems(final Context context, LongSparseArray<ItemInfo> workspaceItems) {
        final ContentResolver cr = context.getContentResolver();
        LauncherModel.runOnWorkerThread(()-> {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            int count = workspaceItems.size();
            for (int i = 0; i < count; i++) {
                ItemInfo item = workspaceItems.valueAt(i);
                Uri uri = LauncherSettings.Favorites.getContentUri(item.id);
                final ContentValues values = new ContentValues();
                values.put(LauncherSettings.Favorites.CELLX, item.cellX);
                values.put(LauncherSettings.Favorites.CELLY, item.cellY);
                ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
            }

            try {
                cr.applyBatch(LauncherProvider.AUTHORITY, ops);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
