package com.condor.launcher;

/**
 * Created by Perry on 19-1-24
 */
public interface AllTaskClearClient {
    public interface OnAllTaskClearListener {
        void dismissAllTasks(Runnable completeRunnable);
        void onAllTasksRemoved();
    }

    void setOnAllTaskClearListener(OnAllTaskClearListener listener);
    // Perry: Optimizing memory info: start
    void updateMemoryInfo();
    // Perry: Optimizing memory info: end
}
