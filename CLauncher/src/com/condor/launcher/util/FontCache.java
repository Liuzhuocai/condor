package com.condor.launcher.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;

import java.util.Hashtable;

/**
 * Created by Perry on 19-2-18
 */
public class FontCache {
    private final static Hashtable<String, Typeface> cache = new Hashtable<>();

    public static Typeface get(Context context, String name) {
        if (context == null || TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("font name or context is null");
        }

        Typeface tf = cache.get(name);
        if (tf == null) {
            tf = Typeface.createFromAsset(context.getAssets(), name);
            cache.put(name, tf);
        }

        return tf;
    }
}
