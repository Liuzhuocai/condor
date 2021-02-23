package com.condor.launcher.editmode;

import android.graphics.Rect;

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
public class EditModeState extends LauncherState {
    private static final int STATE_FLAGS = FLAG_MULTI_PAGE |
            FLAG_DISABLE_ACCESSIBILITY | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED |
            FLAG_DISABLE_PAGE_CLIPPING | FLAG_PAGE_BACKGROUNDS;

    public EditModeState(int id) {
        super(id, LauncherLogProto.ContainerType.EDITMODE, LauncherAnimUtils.EDIT_MODE_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        DeviceProfile grid = launcher.getDeviceProfile();
        Workspace ws = launcher.getWorkspace();
        if (ws.getChildCount() == 0) {
            return super.getWorkspaceScaleAndTranslation(launcher);
        }

        Rect insets = launcher.getDragLayer().getInsets();
        // Perry: Adjust UI: start
        int topPadding = insets.top + (grid.isVerticalBarLayout() ? 0 : grid.dropTargetBarSizePx);
        int bottomPadding = grid.getEditModeButtonBarHeight()
                + insets.bottom + launcher.getWorkspace().getPageIndicator().getMeasuredHeight();
        float scaledHeight = grid.heightPx - topPadding - bottomPadding +
                (grid.isVerticalBarLayout() && grid.isMultiWindowMode ? insets.top : 0);
        float scale = scaledHeight / ws.getNormalChildHeight();
        float halfHeight = ws.getHeight() / 2;
        float myCenter = ws.getTop() + halfHeight;
        float cellTopFromCenter = halfHeight - ws.getChildAt(0).getTop();
        float actualCellTop = myCenter - cellTopFromCenter * scale;

        return new float[] { scale, 0, topPadding - actualCellTop};
        // Perry: Adjust UI: end
    }

    @Override
    public float getPageIndicatorTranslationY(Launcher launcher) {
        DeviceProfile grid = launcher.getDeviceProfile();
        int editModeButtonBarHeight = grid.getEditModeButtonBarHeight();
        if (grid.isVerticalBarLayout()) {
            return - editModeButtonBarHeight;
        }
        // Perry: Adjust hotseat bar size: start
        return (grid.hotseatBarSizePx - editModeButtonBarHeight);
        // Perry: Adjust hotseat bar size: end
    }

    @Override
    public void onStateEnabled(Launcher launcher) {
        Workspace ws = launcher.getWorkspace();
        ws.getPageIndicator().setShouldAutoHide(true);

        // Prevent any Un/InstallShortcutReceivers from updating the db while we are
        // in spring loaded mode
        InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_DRAG_AND_DROP);
        launcher.getRotationHelper().setCurrentStateRequest(RotationHelper.REQUEST_LOCK);

        launcher.getDragController().addDropTarget(launcher.getEditModePanel());
    }

    @Override
    public int getVisibleElements(Launcher launcher) {
        // Perry: Adjust UI: start
        return EDIT_MODE_PANEL | PAGE_INDICATOR;
        // Perry: Adjust UI: end
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

        launcher.getDragController().removeDropTarget(launcher.getEditModePanel());
        // Perry: Implement sliding effect function: start
        launcher.getEditModePanel().resetPanels();
        // Perry: Implement sliding effect function: end
    }
}
