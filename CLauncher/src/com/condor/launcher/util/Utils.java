package com.condor.launcher.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.os.Environment;
import androidx.core.graphics.PathParser;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.android.launcher3.Utilities;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.graphics.IconShapeOverride;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by Perry on 19-1-11
 */
public class Utils {
    private static final String TAG = "Utils";
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final String CHARSET = "UTF-8";

    public static boolean hasGmsVersion() {
        return FeatureFlags.MINUS_ONE;
    }

    public static boolean isTestEnvironment() {
        String state = Environment.getExternalStorageState();
        if (state != null && state.equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
            File file = new File(path + Constants.TEST_EVN_FOLDER_NAME + File.separator);
            return file.exists();
        } else {
            return false;
        }
    }

    public static boolean isDebugEnvironment() {
        String state = Environment.getExternalStorageState();
        if (state != null && state.equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
            File file = new File(path + Constants.DEBUG_EVN_FOLDER_NAME + File.separator);
            return file.exists();
        } else {
            return false;
        }
    }

    // Perry: Adjust recents UI: start
    public static boolean swipeDownToExitRecents() {
        return true;
    }
    // Perry: Adjust recents UI: end

    // Perry: Add predicted applications: start
    public static String getText(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(DEFAULT_BUFFER_SIZE, is.available()));
        copyTo(is, buffer);
        return buffer.toString(CHARSET);
    }

    public static long escapedTime(long start) {
        return System.currentTimeMillis() - start;
    }

    private static long copyTo(InputStream in, OutputStream out) throws IOException {
        long bytesCopied = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytes = in.read(buffer);
        while (bytes >= 0) {
            out.write(buffer, 0, bytes);
            bytesCopied += bytes;
            bytes = in.read(buffer);
        }
        return bytesCopied;
    }
    // Perry: Add predicted applications: end

    // Perry: Add clear all button to recents: start
    public static int getDarkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = hsv[1] + 0.1f;
        hsv[2] = hsv[2] - 0.1f;
        return Color.HSVToColor(hsv);
    }
    // Perry: Add clear all button to recents: end

    // Perry: adjust settings UI: start
    public static Point getSize(Window window) {
        final Point point = new Point();
        WindowManager windowManager = window.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        display.getSize(point);
        return point;
    }

    public static int dp2px(Context context, int dp) {
        return Utilities.pxFromDp(dp, context.getResources().getDisplayMetrics());
    }
    // Perry: adjust settings UI: end

    // Perry: Icon shape override for different desktop modes: start
    public static void resetAdaptiveIconMask() {
        try {
            Method m = PathParser.class.getDeclaredMethod("createPathFromPathData",
                    String.class);
            m.setAccessible(true);
            Object path = m.invoke(null, Resources.getSystem().getString(IconShapeOverride.getConfigResId()));

            Field f = AdaptiveIconDrawable.class.getDeclaredField("sMask");
            f.setAccessible(true);
            f.set(null, path);
        } catch (Exception e) {
            Logger.e(TAG, "resetAdaptiveIconMask " + e.getMessage());
        }
    }
    // Perry: Icon shape override for different desktop modes: end

    // Perry: Add live icon: start
    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static int getTextHeight(Paint paint)
    {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int)Math.ceil(fm.descent - fm.ascent)  ;
    }

    public static int getResourceId(Context context, String text) {
        Resources res = context.getResources();
        if (text.contains(":")) {
            return res.getIdentifier(text, null, null);
        } else if (text.startsWith("@")){
            int index = text.indexOf('/');
            String type = text.substring(1, index);
            String name = text.substring(index + 1);
            return res.getIdentifier(name, type, context.getPackageName());
        }

        return -1;
    }
    // Perry: Add live icon: end
}
