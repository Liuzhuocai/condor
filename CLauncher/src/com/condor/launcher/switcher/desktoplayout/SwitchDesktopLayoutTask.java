package com.condor.launcher.switcher.desktoplayout;

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
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Workspace;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.AddWorkspaceItemsTask;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.GridOccupancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Perry on 19-1-14.
 */
public class SwitchDesktopLayoutTask extends AddWorkspaceItemsTask {
    public SwitchDesktopLayoutTask() {
        super(null);
    }

    @Override
    public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
        Context context = app.getContext();

        app.getWidgetCache().flush();

        final ArrayList<ItemInfo> addedItemsFinal = new ArrayList<>();

        ArrayList<Long> workspaceScreens = new ArrayList<>();

        arrangeWorkspaceItems(app, dataModel, addedItemsFinal, workspaceScreens);

        updateWorkspaceItems(context, addedItemsFinal);

        updateScreens(context, workspaceScreens);

        // Perry: Optimizing window switching animation for Settings: start
        onTaskExecuted();
        // Perry: Optimizing window switching animation for Settings: end

        scheduleCallbackTask(LauncherModel.Callbacks::relaunch);
    }

    // Perry: Optimizing window switching animation for Settings: start
    protected void onTaskExecuted() {
    }
    // Perry: Optimizing window switching animation for Settings: end

    public void arrangeWorkspaceItems(LauncherAppState app, BgDataModel dataModel,
                                      ArrayList<ItemInfo> addedItemsFinal, ArrayList<Long> workspaceScreens) {
        List<WidgetItem> widgetList = dataModel.widgetsModel.update(app, null);
        HashMap<ComponentKey, WidgetItem> widgets = widgetList.stream().collect(HashMap::new,
                (m, w)-> m.put(new ComponentKey(w.componentName, w.user), w), (m1, m2)->{});
        synchronized (dataModel) {
            LongSparseArray<ArrayList<ItemInfo>> screenItems = new LongSparseArray<>();
            for (ItemInfo info : dataModel.itemsIdMap) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    if (info instanceof LauncherAppWidgetInfo) {
                        ComponentKey key = new ComponentKey(((LauncherAppWidgetInfo) info).providerName, info.user);
                        WidgetItem item = widgets.get(key);
                        if (item != null) {
                            info.spanX = item.spanX;
                            info.spanY = item.spanY;
                        }
                    }

                    ArrayList<ItemInfo> items = screenItems.get(info.screenId);
                    if (items == null) {
                        items = new ArrayList<>();
                        screenItems.put(info.screenId, items);
                    }
                    items.add(info);
                }
            }

            final InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            final GridOccupancy occupied = new GridOccupancy(profile.numColumns, profile.numRows);
            final int[] coords = new int[2];
            for (long screenId : dataModel.workspaceScreens) {
                ArrayList<ItemInfo> items = screenItems.get(screenId);
                if (items == null || items.isEmpty()) {
                    continue;
                }

                occupied.clear();
                if (FeatureFlags.QSB_ON_FIRST_SCREEN && screenId == Workspace.FIRST_SCREEN_ID) {
                    occupied.markCells(0, 0, profile.numColumns, 1, true);
                }

                workspaceScreens.add(screenId);

                items.stream().filter(info-> info instanceof LauncherAppWidgetInfo)
                        .sorted((o1, o2)-> {
                            if (o1.cellY != o2.cellY) {
                                return o1.cellY - o2.cellY;
                            } else {
                                return o1.cellX - o2.cellX;
                            }
                        }).forEach(info-> {
                    long lastScreenId = workspaceScreens.get(workspaceScreens.size()-1);
                    if (occupied.findVacantCell(coords, info.spanX, info.spanY)) {
                        info.cellX = coords[0];
                        info.cellY = coords[1];
                        info.screenId = lastScreenId;
                    } else {
                        occupied.clear();
                        long newScreenId = LauncherSettings.Settings.call(app.getContext().getContentResolver(),
                                LauncherSettings.Settings.METHOD_NEW_SCREEN_ID)
                                .getLong(LauncherSettings.Settings.EXTRA_VALUE);
                        workspaceScreens.add(newScreenId);

                        occupied.findVacantCell(coords, info.spanX, info.spanY);
                        info.cellX = coords[0];
                        info.cellY = coords[1];
                        info.screenId = newScreenId;
                    }
                    occupied.markCells(info, true);
                });

                items.stream().filter(info-> !(info instanceof LauncherAppWidgetInfo))
                        .sorted((o1, o2)-> {
                            if (o1.cellY != o2.cellY) {
                                return o2.cellY - o1.cellY;
                            } else {
                                return o1.cellX - o2.cellX;
                            }
                        }).forEach(info-> {
                    long lastScreenId = workspaceScreens.get(workspaceScreens.size()-1);
                    if (occupied.findVacantCellFromBottom(coords, info.spanX, info.spanY)) {
                        info.cellX = coords[0];
                        info.cellY = coords[1];
                        info.screenId = lastScreenId;
                    } else {
                        occupied.clear();
                        long newScreenId = LauncherSettings.Settings.call(app.getContext().getContentResolver(),
                                LauncherSettings.Settings.METHOD_NEW_SCREEN_ID)
                                .getLong(LauncherSettings.Settings.EXTRA_VALUE);
                        workspaceScreens.add(newScreenId);

                        occupied.findVacantCellFromBottom(coords, info.spanX, info.spanY);
                        info.cellX = coords[0];
                        info.cellY = coords[1];
                        info.screenId = newScreenId;
                    }
                    occupied.markCells(info, true);
                });

                addedItemsFinal.addAll(items);
            }
        }
    }

    public void updateWorkspaceItems(final Context context, ArrayList<ItemInfo> workspaceItems) {
        final ArrayList<ItemInfo> itemsCopy = new ArrayList<>(workspaceItems);
        final ContentResolver cr = context.getContentResolver();
        LauncherModel.runOnWorkerThread(()-> {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            int count = itemsCopy.size();
            for (int i = 0; i < count; i++) {
                ItemInfo item = itemsCopy.get(i);
                Uri uri = LauncherSettings.Favorites.getContentUri(item.id);
                final ContentValues values = new ContentValues();
                values.put(LauncherSettings.Favorites.CONTAINER, item.container);
                values.put(LauncherSettings.Favorites.CELLX, item.cellX);
                values.put(LauncherSettings.Favorites.CELLY, item.cellY);
                values.put(LauncherSettings.Favorites.SPANX, item.spanX);
                values.put(LauncherSettings.Favorites.SPANY, item.spanY);
                values.put(LauncherSettings.Favorites.RANK, item.rank);
                values.put(LauncherSettings.Favorites.SCREEN, item.screenId);
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
