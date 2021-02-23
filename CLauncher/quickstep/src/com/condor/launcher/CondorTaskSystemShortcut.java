package com.condor.launcher;

import android.content.ComponentName;
import android.os.UserHandle;
import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.R;
import com.android.quickstep.TaskSystemShortcut;
import com.android.quickstep.views.TaskView;
import com.android.systemui.shared.recents.model.Task;
import com.condor.launcher.locktask.LockTaskManager;

/**
 * Created by Perry on 19-1-24
 */
public class CondorTaskSystemShortcut {
    public static TaskSystemShortcut[] getTaskSystemShortcuts() {
        return new TaskSystemShortcut[] {
                new TaskSystemShortcut.AppInfo(),
                new TaskSystemShortcut.SplitScreen(),
                new TaskSystemShortcut.Pin(),
                new TaskSystemShortcut.Install(),
                new Lock(),
                new Unlock()
        };
    }

    public static class Lock extends TaskSystemShortcut {
        public Lock() {
            super(R.drawable.ic_lock, R.string.label_lock_task);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, TaskView taskView) {
            Task task = taskView.getTask();
            final ComponentName component = task.getTopComponent();
            final UserHandle user = UserHandle.of(task.key.userId);
            if (component == null || user == null) {
                return null;
            }
            if (LockTaskManager.obtain().isTaskLocked(component, user)) {
                return null;
            }

            return v -> {
                LockTaskManager.obtain().addLockTask(taskView.getContext(),
                        component, user);
                taskView.updateLockFlag();
                AbstractFloatingView.closeAllOpenViews(activity);
            };
        }
    }

    public static class Unlock extends TaskSystemShortcut {
        public Unlock() {
            super(R.drawable.ic_unlock, R.string.label_unlock_task);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, TaskView taskView) {
            Task task = taskView.getTask();
            final ComponentName component = task.getTopComponent();
            final UserHandle user = UserHandle.of(task.key.userId);
            if (component == null || user == null) {
                return null;
            }
            if (!LockTaskManager.obtain().isTaskLocked(component, user)) {
                return null;
            }

            return v -> {
                LockTaskManager.obtain().removeLockTask(taskView.getContext(),
                        component, user);
                taskView.updateLockFlag();
                AbstractFloatingView.closeAllOpenViews(activity);

            };
        }
    }
}
