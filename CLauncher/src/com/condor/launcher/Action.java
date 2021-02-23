package com.condor.launcher;

import android.content.Context;
import android.content.Intent;

import com.android.launcher3.Launcher;

import java.util.HashMap;

/**
 * Created by Perry on 19-1-21
 */
public class Action {
    public static final String ACTION_ENTER_EFFECT_PANEL = "action_enter_effect_panel";
    // Perry: Implement frozen apps: start
    public static final String ACTION_ENTER_FROZEN_PANEL = "action_enter_frozen_panel";
    // Perry: Implement frozen apps: end

    private static final String TAG = "ACTION";
    private static final HashMap<String, Action> CACHE = new HashMap<>();
    private String id;
    private ActionHandler handler;

    public String getId() {
        return id;
    }

    public interface ActionHandler {
        void handle(Launcher launcher);
    }

    private Action(String id) {
        this.id = id;
    }

    public Action setHandler(ActionHandler handler) {
        this.handler = handler;
        return this;
    }

    public void attach(Context context) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(context.getPackageName())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(TAG, id);
        context.startActivity(homeIntent);
    }

    public void handle(Launcher launcher) {
        if (handler != null) {
            handler.handle(launcher);
        }
    }

    public static Action create(String id) {
        Action action = CACHE.get(id);
        if (action == null) {
            action = new Action(id);
            CACHE.put(id, action);
        }

        return action;
    }

    public static Action from(Intent intent) {
        String id = intent.getStringExtra(TAG);
        if (id == null) {
            return null;
        }

        return CACHE.get(id);
    }
}
