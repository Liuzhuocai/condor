package com.condor.launcher.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Perry on 19-1-21
 */
public class ToastHelper {
    private static Toast sToast;

    private static void setToast(Context context, String msg) {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
    }

    public static void showMessage(Context context, int resid) {
        String string = context.getString(resid);
        setToast(context, string);
        sToast.setDuration(Toast.LENGTH_SHORT);
        sToast.setText(resid);
        sToast.show();
    }

    public static void showMessage(Context context, String msg) {
        setToast(context, msg);
        sToast.setDuration(Toast.LENGTH_SHORT);
        sToast.setText(msg);
        sToast.show();
    }

    public static void cancelMessage() {
        if (sToast == null) {
            return;
        }
        sToast.cancel();
    }

}
