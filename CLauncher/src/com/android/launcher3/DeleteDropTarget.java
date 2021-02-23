/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.userevent.nano.LauncherLogProto.ControlType;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.condor.launcher.switcher.desktopmode.DesktopModeHelper;

import java.util.ArrayList;

public class DeleteDropTarget extends ButtonDropTarget {

    private int mControlType = ControlType.DEFAULT_CONTROLTYPE;

    public DeleteDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Get the hover color
        mHoverColor = getResources().getColor(R.color.delete_target_hover_tint);

        setDrawable(R.drawable.ic_remove_shadow);
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        super.onDragStart(dragObject, options);
        setTextBasedOnDragSource(dragObject.dragInfo);
        setControlTypeBasedOnDragSource(dragObject.dragInfo);
    }

    /**
     * @return true for items that should have a "Remove" action in accessibility.
     */
    @Override
    public boolean supportsAccessibilityDrop(ItemInfo info, View view) {
        return (info instanceof ShortcutInfo)
                || (info instanceof LauncherAppWidgetInfo)
                || (info instanceof FolderInfo);
    }

    @Override
    public int getAccessibilityAction() {
        return LauncherAccessibilityDelegate.REMOVE;
    }

    // Perry: desktop switch function: start
    @Override
    protected boolean supportsDrop(ItemInfo info) {
        return supportsAccessibleDrop(info);
    }

    /** @return true for items that should have a "Remove" action in accessibility. */
    public static boolean supportsAccessibleDrop(ItemInfo info) {
        /** if in default desktop mode, application can't remove **/
        if (DesktopModeHelper.isDefaultMode() && info instanceof ShortcutInfo
                && info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            return false;
        }

        return true;
    }
    // Perry: desktop switch function: end

    /**
     * Set the drop target's text to either "Remove" or "Cancel" depending on the drag item.
     */
    private void setTextBasedOnDragSource(ItemInfo item) {
        if (!TextUtils.isEmpty(mText)) {
            // Perry: desktop switch function: start
            if (DesktopModeHelper.isDefaultMode()) {
                mText = getResources().getString(item instanceof FolderInfo
                        ? R.string.release_drop_target_label
                        : R.string.remove_drop_target_label);
                // Perry: add extra system shortcuts: start
                if (item instanceof FolderInfo) {
                    setDrawable(R.drawable.ic_release_shadow);
                } else {
                    setDrawable(R.drawable.ic_remove_shadow);
                }
                // Perry: add extra system shortcuts: end
            } else {
                mText = getResources().getString(item.id != ItemInfo.NO_ID
                        ? R.string.remove_drop_target_label
                        : android.R.string.cancel);
                setDrawable(R.drawable.ic_remove_shadow);
            }
            // Perry: desktop switch function: end
            requestLayout();
        }
    }

    /**
     * Set mControlType depending on the drag item.
     */
    private void setControlTypeBasedOnDragSource(ItemInfo item) {
        // Perry: desktop switch function: start
        if (DesktopModeHelper.isDefaultMode()) {
            mControlType = item instanceof FolderInfo ? ControlType.RELEASE_FOLDER
                    : ControlType.REMOVE_TARGET;
        } else {
            mControlType = item.id != ItemInfo.NO_ID ? ControlType.REMOVE_TARGET
                    : ControlType.CANCEL_TARGET;
        }
        // Perry: desktop switch function: end
    }

    @Override
    public void completeDrop(DragObject d) {
        ItemInfo item = d.dragInfo;
        if ((d.dragSource instanceof Workspace) || (d.dragSource instanceof Folder)) {
            onAccessibilityDrop(null, item);
        }
    }

    /**
     * Removes the item from the workspace. If the view is not null, it also removes the view.
     */
    @Override
    public void onAccessibilityDrop(View view, ItemInfo item) {
        // Perry: desktop switch function: start
        removeWorkspaceOrFolderItem(mLauncher, item, view, mControlType == ControlType.RELEASE_FOLDER);
        // Perry: desktop switch function: end
    }

    @Override
    public Target getDropTargetForLogging() {
        Target t = LoggerUtils.newTarget(Target.Type.CONTROL);
        t.controlType = mControlType;
        return t;
    }

    // Perry: desktop switch function: start
    public static void removeWorkspaceOrFolderItem(Launcher launcher, ItemInfo item, View view, boolean release) {
        // Remove the item from launcher and the db, we can ignore the containerInfo in this call
        // because we already remove the drag view from the folder (if the drag originated from
        // a folder) in Folder.beginDrag()
        ArrayList<ItemInfo> addShortcuts = null;
        if (release) {
            addShortcuts = new ArrayList<>(((FolderInfo) item).contents);
        }

        launcher.removeItem(view, item, true /* deleteFromDb */);

        // Perry: Resolve folder's release error: start
        // Perry: Hotseat reorder function: start
        launcher.getHotseat().arrangeContent();
        // Perry: Hotseat reorder function: end
        launcher.getWorkspace().stripEmptyScreens();
        launcher.getDragLayer().announceForAccessibility(launcher.getString(R.string.item_removed));

        if (addShortcuts != null) {
            launcher.getModel().addAndBindItemsToWorkspace(addShortcuts,
                    launcher.getCurrentWorkspaceScreen(), DesktopModeHelper.isDefaultMode());
        }
        // Perry: Resolve folder's release error: end
    }
    // Perry: desktop switch function: end
}
