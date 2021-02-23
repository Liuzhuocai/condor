/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.launcher3.model;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.LongSparseArray;
import android.util.Pair;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.AppInfo;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherModel.CallbackTask;
import com.android.launcher3.LauncherModel.Callbacks;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.GridOccupancy;
import com.condor.launcher.switcher.desktopmode.DesktopModeHelper;
import com.condor.launcher.switcher.desktopmode.StartPageHelper;

import java.util.ArrayList;
import java.util.List;

import static com.condor.launcher.switcher.desktopmode.StartPageHelper.INVALID_START_PAGE;

/**
 * Task to add auto-created workspace items.
 */
public class AddWorkspaceItemsTask extends BaseModelUpdateTask {
    protected final int mStartPage;
    private final List<Pair<ItemInfo, Object>> mItemList;

    /**
     * @param itemList items to add on the workspace
     */
    public AddWorkspaceItemsTask(List<Pair<ItemInfo, Object>> itemList) {
        this(itemList, getDefaultStartPage());
    }

    public AddWorkspaceItemsTask(List<Pair<ItemInfo, Object>> itemList, int startPage) {
        mItemList = itemList;
        mStartPage = startPage == INVALID_START_PAGE ? getDefaultStartPage() : startPage;
    }

    @Override
    public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
        if (mItemList.isEmpty()) {
            return;
        }
        Context context = app.getContext();

        final ArrayList<ItemInfo> addedItemsFinal = new ArrayList<>();
        final ArrayList<Long> addedWorkspaceScreensFinal = new ArrayList<>();

        // Get the list of workspace screens.  We need to append to this list and
        // can not use sBgWorkspaceScreens because loadWorkspace() may not have been
        // called.
        ArrayList<Long> workspaceScreens = LauncherModel.loadWorkspaceScreensDb(context);
        synchronized(dataModel) {

            List<ItemInfo> filteredItems = new ArrayList<>();
            for (Pair<ItemInfo, Object> entry : mItemList) {
                ItemInfo item = entry.first;
                if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                        item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                    // Short-circuit this logic if the icon exists somewhere on the workspace
                    if (shortcutExists(dataModel, item.getIntent(), item.user)) {
                        continue;
                    }
                }

                if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                    if (item instanceof AppInfo) {
                        item = ((AppInfo) item).makeShortcut();
                    }
                }
                if (item != null) {
                    filteredItems.add(item);
                }
            }

            for (ItemInfo item : filteredItems) {
                // Find appropriate space for the item.
                Pair<Long, int[]> coords = findSpaceForItem(app, dataModel, workspaceScreens,
                        addedWorkspaceScreensFinal, item.spanX, item.spanY);
                long screenId = coords.first;
                int[] cordinates = coords.second;

                ItemInfo itemInfo;
                if (item instanceof ShortcutInfo || item instanceof FolderInfo ||
                        item instanceof LauncherAppWidgetInfo) {
                    itemInfo = item;
                } else if (item instanceof AppInfo) {
                    itemInfo = ((AppInfo) item).makeShortcut();
                } else {
                    throw new RuntimeException("Unexpected info type");
                }

                // Add the shortcut to the db
                getModelWriter().addItemToDatabase(itemInfo,
                        LauncherSettings.Favorites.CONTAINER_DESKTOP, screenId,
                        cordinates[0], cordinates[1]);

                // Save the ShortcutInfo for binding in the workspace
                addedItemsFinal.add(itemInfo);
            }
        }

        // Update the workspace screens
        updateScreens(context, workspaceScreens);

        if (!addedItemsFinal.isEmpty()) {
            scheduleCallbackTask(new CallbackTask() {
                @Override
                public void execute(Callbacks callbacks) {
                    final ArrayList<ItemInfo> addAnimated = new ArrayList<>();
                    final ArrayList<ItemInfo> addNotAnimated = new ArrayList<>();
                    if (!addedItemsFinal.isEmpty()) {
                        ItemInfo info = addedItemsFinal.get(addedItemsFinal.size() - 1);
                        long lastScreenId = info.screenId;
                        for (ItemInfo i : addedItemsFinal) {
                            if (i.screenId == lastScreenId) {
                                addAnimated.add(i);
                            } else {
                                addNotAnimated.add(i);
                            }
                        }
                    }
                    if (supportAnimated()) {
                        callbacks.bindAppsAdded(addedWorkspaceScreensFinal,
                                addNotAnimated, addAnimated);
                    } else {
                        addNotAnimated.addAll(addAnimated);
                        callbacks.bindAppsAdded(addedWorkspaceScreensFinal,
                                addNotAnimated, null);
                    }
                }
            });
        }
    }

    protected boolean supportAnimated() {
        return true;
    }

    protected void updateScreens(Context context, ArrayList<Long> workspaceScreens) {
        LauncherModel.updateWorkspaceScreenOrder(context, workspaceScreens);
    }

    /**
     * Returns true if the shortcuts already exists on the workspace. This must be called after
     * the workspace has been loaded. We identify a shortcut by its intent.
     */
    protected boolean shortcutExists(BgDataModel dataModel, Intent intent, UserHandle user) {
        final String compPkgName, intentWithPkg, intentWithoutPkg;
        if (intent == null) {
            // Skip items with null intents
            return true;
        }
        if (intent.getComponent() != null) {
            // If component is not null, an intent with null package will produce
            // the same result and should also be a match.
            compPkgName = intent.getComponent().getPackageName();
            if (intent.getPackage() != null) {
                intentWithPkg = intent.toUri(0);
                intentWithoutPkg = new Intent(intent).setPackage(null).toUri(0);
            } else {
                intentWithPkg = new Intent(intent).setPackage(compPkgName).toUri(0);
                intentWithoutPkg = intent.toUri(0);
            }
        } else {
            compPkgName = null;
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = intent.toUri(0);
        }

        boolean isLauncherAppTarget = Utilities.isLauncherAppTarget(intent);
        synchronized (dataModel) {
            for (ItemInfo item : dataModel.itemsIdMap) {
                if (item instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) item;
                    if (item.getIntent() != null && info.user.equals(user)) {
                        Intent copyIntent = new Intent(item.getIntent());
                        copyIntent.setSourceBounds(intent.getSourceBounds());
                        String s = copyIntent.toUri(0);
                        if (intentWithPkg.equals(s) || intentWithoutPkg.equals(s)) {
                            return true;
                        }

                        // checking for existing promise icon with same package name
                        if (isLauncherAppTarget
                                && info.isPromise()
                                && info.hasStatusFlag(ShortcutInfo.FLAG_AUTOINSTALL_ICON)
                                && info.getTargetComponent() != null
                                && compPkgName != null
                                && compPkgName.equals(info.getTargetComponent().getPackageName())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find a position on the screen for the given size or adds a new screen.
     * @return screenId and the coordinates for the item.
     */
    protected Pair<Long, int[]> findSpaceForItem(
            LauncherAppState app, BgDataModel dataModel,
            ArrayList<Long> workspaceScreens,
            ArrayList<Long> addedWorkspaceScreensFinal,
            int spanX, int spanY) {
        LongSparseArray<ArrayList<ItemInfo>> screenItems = new LongSparseArray<>();

        // Use sBgItemsIdMap as all the items are already loaded.
        synchronized (dataModel) {
            for (ItemInfo info : dataModel.itemsIdMap) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    ArrayList<ItemInfo> items = screenItems.get(info.screenId);
                    if (items == null) {
                        items = new ArrayList<>();
                        screenItems.put(info.screenId, items);
                    }
                    items.add(info);
                }
            }
        }

        // Find appropriate space for the item.
        long screenId = 0;
        int[] cordinates = new int[2];
        boolean found = false;

        int screenCount = workspaceScreens.size();
        // First check the preferred screen.
        int preferredScreenIndex = workspaceScreens.isEmpty() ? 0 : mStartPage;
        if (preferredScreenIndex < screenCount) {
            screenId = workspaceScreens.get(preferredScreenIndex);
          // Perry: fix occupancy errors: start
            found = findNextAvailableIconSpaceInScreen(
                    app, screenItems.get(screenId), screenId, cordinates, spanX, spanY);
          // Perry: fix occupancy errors: end
        }

        if (!found) {
            // Search on any of the screens starting from the first screen.
            for (int screen = mStartPage; screen < screenCount; screen++) {
                screenId = workspaceScreens.get(screen);
              // Perry: fix occupancy errors: start
                if (findNextAvailableIconSpaceInScreen(
                        app, screenItems.get(screenId), screenId, cordinates, spanX, spanY)) {
                    // We found a space for it
                    found = true;
                    break;
                }
              // Perry: fix occupancy errors: end
            }
        }

        if (!found) {
            // Still no position found. Add a new screen to the end.
            screenId = LauncherSettings.Settings.call(app.getContext().getContentResolver(),
                    LauncherSettings.Settings.METHOD_NEW_SCREEN_ID)
                    .getLong(LauncherSettings.Settings.EXTRA_VALUE);

            // Save the screen id for binding in the workspace
            workspaceScreens.add(screenId);
            addedWorkspaceScreensFinal.add(screenId);

            // If we still can't find an empty space, then God help us all!!!
          // Perry: fix occupancy errors: start
            if (!findNextAvailableIconSpaceInScreen(
                    app, screenItems.get(screenId), screenId, cordinates, spanX, spanY)) {
                throw new RuntimeException("Can't find space to add the item");
            }
          // Perry: fix occupancy errors: end
        }
        return Pair.create(screenId, cordinates);
    }

    private boolean findNextAvailableIconSpaceInScreen(
            LauncherAppState app, ArrayList<ItemInfo> occupiedPos, long screenId,
            int[] xy, int spanX, int spanY) {
        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();

        GridOccupancy occupied = new GridOccupancy(profile.numColumns, profile.numRows);

        // Perry: fix occupancy errors: start
        if (FeatureFlags.QSB_ON_FIRST_SCREEN && screenId == Workspace.FIRST_SCREEN_ID) {
            occupied.markCells(0, 0, profile.numColumns, 1, true);
        }
        // Perry: fix occupancy errors:  end

        if (occupiedPos != null) {
            for (ItemInfo r : occupiedPos) {
                occupied.markCells(r, true);
            }
        }
        return occupied.findVacantCell(xy, spanX, spanY);
    }

    private static int getDefaultStartPage() {
        if (DesktopModeHelper.isDefaultMode()) {
            int startPage = StartPageHelper.getStartPage();
            if (startPage != INVALID_START_PAGE) {
                return startPage;
            }
        }

        return 1;
    }

}
