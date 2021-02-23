package com.condor.launcher.views;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.launcher3.R;
import com.condor.launcher.adapter.ListPreferenceAdapter;
import com.condor.launcher.util.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by Perry on 19-1-25
 */
public class CondorListPreference extends ListPreference {
    public CondorListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CondorListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CondorListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public CondorListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void showDialog(Bundle state) {
        View contentView = onCreateDialogView();
        if (contentView != null) {
            onBindDialogView(contentView);
        }

        registerOnActivityDestroyListener(this);
        final Dialog dialog = new Dialog(getContext(), R.style.CondorSettingsDialog);
        final Window window = dialog.getWindow();
        final WindowManager.LayoutParams lp = window.getAttributes();
        window.setContentView(contentView);
        lp.gravity = Gravity.BOTTOM;
        lp.width = Utils.getSize(window).x - 2 * Utils.dp2px(getContext(), 10);
        window.setAttributes(lp);
        dialog.setOnDismissListener(this);
        setDialog(dialog);
        if (state != null) {
            dialog.onRestoreInstanceState(state);
        }
        dialog.show();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        TextView title = view.findViewById(R.id.dialog_title);
        title.setText(getDialogTitle());
        RecyclerView content = view.findViewById(R.id.dialog_content);
        content.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        ListPreferenceAdapter adapter = new ListPreferenceAdapter(Arrays.asList(getEntries()), findIndexOfValue(getValue()));
        adapter.setItemClickListener(this::clickItem);
        content.setAdapter(adapter);
    }

    @Override
    protected View onCreateDialogView() {
        return LayoutInflater.from(getContext()).inflate(R.layout.condor_list_dialog,null);
    }

    private void clickItem(int index) {
        setClickedDialogEntryIndex(index);
        Dialog dialog = getDialog();
        if (dialog != null && dialog.isShowing()) {
            onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            dialog.dismiss();
        }
    }

    private void setDialog(Dialog dialog) {
        try {
            Field fDialog = DialogPreference.class.getDeclaredField("mDialog");
            fDialog.setAccessible(true);
            fDialog.set(this, dialog);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setClickedDialogEntryIndex(int index) {
        try {
            Field fClickedDialogEntryIndex = ListPreference.class.getDeclaredField("mClickedDialogEntryIndex");
            fClickedDialogEntryIndex.setAccessible(true);
            fClickedDialogEntryIndex.set(this, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerOnActivityDestroyListener(PreferenceManager.OnActivityDestroyListener listener) {
        try {
            Method method = PreferenceManager.class.getDeclaredMethod("registerOnActivityDestroyListener",
                    listener.getClass());
            method.setAccessible(true);
            method.invoke(getPreferenceManager(), listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
