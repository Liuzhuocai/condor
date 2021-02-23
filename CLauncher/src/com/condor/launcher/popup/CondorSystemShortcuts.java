package com.condor.launcher.popup;

import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.DeleteDropTarget;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.popup.SystemShortcut;
import com.condor.launcher.CondorFeatureFlags;
import com.condor.launcher.switcher.desktopmode.DesktopModeHelper;
import com.condor.launcher.util.UninstallUtils;

import static com.android.launcher3.AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME;
import static com.condor.launcher.settings.SettingsPersistence.SCREEN_LOCKED;

/**
 * Created by Perry on 19-1-14
 */
public class CondorSystemShortcuts {
    public static SystemShortcut[] getSystemShortcuts() {
        if (CondorFeatureFlags.SUPPORT_EXTRA_SYSTEM_SHORTCUTS) {
            return new SystemShortcut[] {
                    new SystemShortcut.AppInfo(),
                    new SystemShortcut.Widgets(),
                    new SystemShortcut.Install(),
                    new Uninstall(),
                    new Remove(),
                    new Release()
            };
        }

        return new SystemShortcut[] {
                new SystemShortcut.AppInfo(),
                new SystemShortcut.Widgets(),
                new SystemShortcut.Install()
        };
    }

    public static class Uninstall extends SystemShortcut {
        public Uninstall() {
            super(R.drawable.ic_uninstall_no_shadow, R.string.uninstall_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(BaseDraggingActivity activity, final ItemInfo itemInfo) {
            // Perry: Fixed screen function: start
            if (SCREEN_LOCKED.value()) {
                return null;
            }
            // Perry: Fixed screen function: end

            if (!UninstallUtils.supportUninstall(activity, itemInfo)) {
                return null;
            }

            return (v)-> {
                AbstractFloatingView.closeAllOpenViews(activity);
                UninstallUtils.startUninstallActivity(activity, itemInfo);
            };
        }
    }

    public static class Remove extends SystemShortcut {
        public Remove() {
            super(R.drawable.ic_remove_no_shadow, R.string.remove_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(BaseDraggingActivity activity, final ItemInfo itemInfo) {
            // Perry: Fixed screen function: start
            if (SCREEN_LOCKED.value()) {
                return null;
            }
            // Perry: Fixed screen function: end

            if (!(activity instanceof Launcher)) {
                return null;
            }

            if (itemInfo instanceof com.android.launcher3.AppInfo) {
                return null;
            }

            if (!DeleteDropTarget.supportsAccessibleDrop(itemInfo)) {
                return null;
            }

            if (DesktopModeHelper.isDefaultMode() &&
                    itemInfo instanceof FolderInfo) {
                return null;
            }

            return (v)-> {
                AbstractFloatingView resizeFrame = AbstractFloatingView.getTopOpenViewWithType(activity,
                        TYPE_WIDGET_RESIZE_FRAME);
                if (resizeFrame != null) {
                    resizeFrame.close(true);
                }

                AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(activity);
                if (topView instanceof PopupContainerWithArrow) {
                    topView.close(true);
                    DeleteDropTarget.removeWorkspaceOrFolderItem((Launcher) activity, itemInfo,
                            ((PopupContainerWithArrow)topView).getExtendedTouchView(), false);
                }
            };
        }
    }

    public static class Release extends SystemShortcut {
        public Release() {
            super(R.drawable.ic_release_no_shadow, R.string.release_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(BaseDraggingActivity activity, final ItemInfo itemInfo) {
            // Perry: Fixed screen function: start
            if (SCREEN_LOCKED.value()) {
                return null;
            }
            // Perry: Fixed screen function: end

            if (!(activity instanceof Launcher)) {
                return null;
            }

            if (!(itemInfo instanceof FolderInfo)) {
                return null;
            }

            return (v)-> {
                AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(activity);
                if (topView instanceof PopupContainerWithArrow) {
                    topView.close(true);
                    DeleteDropTarget.removeWorkspaceOrFolderItem((Launcher) activity, itemInfo,
                            ((PopupContainerWithArrow)topView).getExtendedTouchView(), true);
                }
            };
        }
    }
}
