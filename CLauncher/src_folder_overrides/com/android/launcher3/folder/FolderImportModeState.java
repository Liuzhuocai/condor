package com.android.launcher3.folder;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InstallShortcutReceiver;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Workspace;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.userevent.nano.LauncherLogProto;

/**
 * Created by Perry on 19-1-18
 */
public class FolderImportModeState extends LauncherState {
    private static final int STATE_FLAGS = FLAG_MULTI_PAGE |
            FLAG_DISABLE_ACCESSIBILITY | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED |
            FLAG_DISABLE_PAGE_CLIPPING ;

    public FolderImportModeState(int id) {
        super(id, LauncherLogProto.ContainerType.IMPORTMODE, LauncherAnimUtils.EDIT_MODE_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        DeviceProfile grid = launcher.getDeviceProfile();
        Workspace ws = launcher.getWorkspace();
        if (ws.getChildCount() == 0) {
            return super.getWorkspaceScaleAndTranslation(launcher);
        }

        return new float[] {1f, 0, 0};
    }

    @Override
    public float getPageIndicatorTranslationY(Launcher launcher) {
        DeviceProfile grid = launcher.getDeviceProfile();
        int editModeButtonBarHeight = grid.getEditModeButtonBarHeight();
        if (grid.isVerticalBarLayout()) {
            return - editModeButtonBarHeight;
        }
        return grid.hotseatBarSizePx - editModeButtonBarHeight;
    }

    @Override
    public void onStateEnabled(Launcher launcher) {
        Workspace ws = launcher.getWorkspace();
        ws.getPageIndicator().setShouldAutoHide(true);

        // Prevent any Un/InstallShortcutReceivers from updating the db while we are
        // in spring loaded mode
        InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_DRAG_AND_DROP);
        launcher.getRotationHelper().setCurrentStateRequest(RotationHelper.REQUEST_LOCK);

        //launcher.getDragController().addDropTarget(launcher.getEditModePanel());
    }

    @Override
    public int getVisibleElements(Launcher launcher) {
        return NONE ;
    }

    @Override
    public float getWorkspaceScrimAlpha(Launcher launcher) {
        return 0;
    }

    @Override
    public void onStateDisabled(final Launcher launcher) {
        final Workspace ws = launcher.getWorkspace();
        ws.getPageIndicator().setShouldAutoHide(true);

        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_DRAG_AND_DROP, launcher);
    }
}
