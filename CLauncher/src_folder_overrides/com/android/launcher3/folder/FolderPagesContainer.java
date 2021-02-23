package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;

import java.util.ArrayList;

/**
 * Created by xiaopeng on 17-8-31.
 */

public class FolderPagesContainer extends AbstractFloatingView implements Insettable {
    private static final String TAG = "FolderPagesContainer";
    private final Launcher mLauncher;
    private final ArrayList<Folder> mFolders = new ArrayList<>();
    private Folder mSelectedFolder;
    private boolean mIsHovering;
    // Chenyee <CY_Bug> <xiaopeng> <20181025> for CSW1809A-162 begin
    private boolean mTouchOutSpace;
    // Chenyee <CY_Bug> <xiaopeng> <20181025> for CSW1809A-162 end

    public static FolderPagesContainer create(Context context) {
        return null;
    }

    public FolderPagesContainer(Context context) {
        this(context, null);
    }

    public FolderPagesContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderPagesContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutDirection(LAYOUT_DIRECTION_LTR);

        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected void handleClose(boolean animate) {

    }

    @Override
    public void logActionCommand(int command) {

    }

    @Override
    protected boolean isOfType(int type) {
        return false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }


    @Override
    public void setInsets(Rect insets) {

    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
    public  void animateClose(Runnable r){

    }
}
