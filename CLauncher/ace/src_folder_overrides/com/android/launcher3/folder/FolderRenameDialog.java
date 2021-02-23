package com.android.launcher3.folder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.folder.Folder;


/**
 * Created by xiaopeng on 18-8-6.
 */

public class FolderRenameDialog implements ExtendedEditText.OnBackKeyListener {
    private final AlertDialog mRenameDialog;
    private final FolderEditText mFolderName;
    private final InputMethodManager mInputManager;
    private final Launcher mLauncher;

    public FolderRenameDialog(Context context, final Folder folder) {
        mLauncher = Launcher.getLauncher(context);
        FrameLayout layout = new FrameLayout(context);

        mInputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int paddingH = Utilities.pxFromDp(20, dm);
        int paddingV = Utilities.pxFromDp(4,  dm);
        layout.setPadding(paddingH, paddingV, paddingH, paddingV);

        mFolderName = new FolderEditText(context);
        mFolderName.setHorizontallyScrolling(true);
        mFolderName.setSingleLine(true);
        mFolderName.setOnBackKeyListener(this);

        mFolderName.setTextChangeListener(new FolderEditText.TextChangeListener() {
            @Override
            public void onTextChanged(String s) {
                if (mRenameDialog != null && mRenameDialog.isShowing()) {
                    boolean isFolderNameAvailable = !TextUtils.isEmpty(s.trim());
                    mRenameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isFolderNameAvailable);
                }
            }
        });

        layout.addView(mFolderName, new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.rename_folder_titles));
        builder.setView(layout);
        builder.setPositiveButton(
                context.getResources().getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        folder.doneEditingFolderName();
                        doneEditFolderName();
                    }
                });
        builder.setNegativeButton(
                context.getResources().getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                doneEditFolderName();
            }
        });
        mRenameDialog = builder.create();
        mRenameDialog.setCanceledOnTouchOutside(false);
        mRenameDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (isSoftInputShow(mRenameDialog)) {
                    return false;
                }
                dialog.dismiss();
                return true;
            }
        });
        WindowManager.LayoutParams lp = mRenameDialog.getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;
    }

    public void startEditFolderName(){
        mRenameDialog.show();
        mRenameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

        mFolderName.selectAll();
        mFolderName.requestFocus();
        mFolderName.postDelayed(new Runnable() {
            @Override
            public void run() {
                mInputManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }, 200);
    }

    public void doneEditFolderName() {
        mInputManager.hideSoftInputFromWindow(mFolderName.getWindowToken(), 0);
        mRenameDialog.dismiss();
    }

    public FolderEditText getFolderName() {
        return mFolderName;
    }

    public boolean isShowing() {
        return mRenameDialog.isShowing();
    }

    @Override
    public boolean onBackKey() {
        if (mInputManager.isActive()) {
            mInputManager.hideSoftInputFromWindow(mFolderName.getWindowToken(), 0);
        } else {
            doneEditFolderName();
        }

        return true;
    }

    public boolean isSoftInputShow(Dialog dialog) {
        if (dialog.isShowing()) {
            DeviceProfile grid = mLauncher.getDeviceProfile();
            View root = dialog.getWindow().getDecorView();
            if (root != null) {
                int[] location = new int[2];
                root.getLocationOnScreen(location);
                final int bottom = location[1] + root.getHeight();
                return bottom + grid.getInsets().bottom < grid.heightPx;
            }
        }

        return false;
    }
}
