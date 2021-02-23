package com.condor.launcher.util;

import android.os.HandlerThread;
import android.util.Log;

import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Perry on 19-1-15
 */
public class ThreadUtils {
    private static final String TAG = "ThreadUtils";
    private static final HashMap<String, WeakReference<HandlerThread>> sThreads = new HashMap<>();

    public static synchronized HandlerThread get(String name) {
        WeakReference<HandlerThread> ref = sThreads.get(name);
        if (ref != null && ref.get() != null) {
            HandlerThread thread = ref.get();
            if (thread.isAlive()) {
                thread.quit();
                thread = new HandlerThread(name);
                sThreads.put(name, new WeakReference<>(thread));
            }
            return thread;
        }
        HandlerThread thread = new HandlerThread(name);
        sThreads.put(name, new WeakReference<>(thread));

        return thread;
    }

    public static HandlerThread get(Object obj, String name) {
        return get(obj + "#" + name);
    }

    public static synchronized void remove(String name) {
        WeakReference<HandlerThread> ref = sThreads.get(name);
        if (ref != null && ref.get() != null) {
            if (ref.get().isAlive()) {
                ref.get().quit();
            }
        }
        sThreads.remove(name);
    }

    public static void remove(Object obj, String name) {
        remove(obj + "#" + name);
    }

    public static synchronized void clear() {
        for (WeakReference<HandlerThread> ref : sThreads.values()) {
            if (ref != null && ref.get() != null) {
                if (ref.get().isAlive()) {
                    ref.get().quit();
                }
            }
        }
        sThreads.clear();
    }

    public static synchronized void dump(PrintWriter writer) {
        StringBuilder msg = new StringBuilder();
        for (Map.Entry<String, WeakReference<HandlerThread>> entry : sThreads.entrySet()) {
            String name = entry.getKey();
            WeakReference<HandlerThread> ref = entry.getValue();
            if (ref != null && ref.get() != null) {
                msg.append(String.format("Thread '%s' id: %s", name, ref.get().getThreadId()));
                if (ref.get().isAlive()) {
                    msg.append(" is active");
                } else {
                    msg.append(" is not active");
                }
                writer.println(msg);
                msg.setLength(0);
            }
        }
    }
}
