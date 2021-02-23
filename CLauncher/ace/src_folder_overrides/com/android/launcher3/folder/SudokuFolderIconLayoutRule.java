package com.android.launcher3.folder;

import android.content.res.Resources;
import android.util.Log;

import com.condor.launcher.theme.bean.ThemeConfigBean;
import com.condor.launcher.util.ThemeUtils;

/**
 * Created by liuzuo on 17-3-9.
 */

public class SudokuFolderIconLayoutRule implements FolderIcon.PreviewLayoutRule {
    static final int MAX_NUM_ITEMS_IN_PREVIEW = 9;
    private final String TAG = "SudokuFolderIconLayoutRule";
    final float MIN_SCALE = 0.48f;
    private  float MAX_SCALE = 0.18f;
    private final  int MAX_ROW = 3;
    private final  int MAX_COLUMN = 3;
    private float folderPaddingLeft;
    private float folderPaddingRight;
    private float folderPaddingTop;
    private float folderPaddingBottom;
    private int folderBgSize;
    private float[] mTmpPoint = new float[2];

    private float mAvailableSpace;
    private float mIconSize;
    private boolean mIsRtl;
    private float mBaselineIconScale;
    @Override
    public void init(int availableSpace, float intrinsicIconSize, boolean rtl ,Resources r ) {
        mAvailableSpace = availableSpace;
        mIconSize = intrinsicIconSize;
        mIsRtl = rtl;
        mBaselineIconScale = availableSpace / (intrinsicIconSize * 1f);
        ThemeConfigBean themeConfigBean = ThemeUtils.getThemeConfigBean();
        if(themeConfigBean!=null){
            folderPaddingLeft = themeConfigBean.getPadding_left();
            folderPaddingRight = themeConfigBean.getPadding_right();
            folderPaddingTop = themeConfigBean.getPadding_top();
            folderPaddingBottom = themeConfigBean.getPadding_bottom();
            MAX_SCALE = themeConfigBean.getScale();
            Log.d("liuzuo99","SudokuFolderIconLayoutRule ThemeConfigBean="+themeConfigBean.toString());
        }
        folderBgSize =  availableSpace;
        folderBgSize = folderBgSize > mIconSize ? (int) mIconSize : folderBgSize;
    }

    @Override
    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index,
                                                                    int curNumItems, PreviewItemDrawingParams params) {

        float totalScale = scaleForNumItems(curNumItems);
        float transX;
        float transY;
        float overlayAlpha = 0;

        // Items beyond those displayed in the preview are animated to the center
        if (index >= MAX_NUM_ITEMS_IN_PREVIEW) {
            transX = transY = mAvailableSpace / 2 - (mIconSize * totalScale) / 2;
        } else {
            getPosition(index, curNumItems, mTmpPoint);
            transX = mTmpPoint[0];
            transY = mTmpPoint[1];
        }

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
        } else {
            params.update(transX, transY, totalScale);
            params.overlayAlpha = overlayAlpha;
        }
        return params;
    }

    private void getPosition(int index, int curNumItems, float[] result) {
//        result[0] =/*mIconSize*getGapScaleX()/2*/+Math.abs(mAvailableSpace - mIconSize) /2+ mIconSize * scaleXForNumItems(index) * (index % 3);
//        result[1] =  /*mIconSize*getGapScaleX()/2*/+Math.abs(mAvailableSpace - mIconSize) / 2 + mIconSize * scaleYForNumItems(index) * (index / 3);
        result[0] = folderBgSize * getGapScaleX() / 2 + folderBgSize * scaleXForNumItems(index) * (mIsRtl ? MAX_ROW - (index % 3) -1: (index % 3))+getPaddingX(index) ;
        result[1] = folderBgSize * getGapScaleY() / 2 + folderBgSize * scaleYForNumItems(index) * (index / 3) + getPaddingY(index);
    }

    private float scaleForNumItems(int numItems) {
            return MAX_SCALE * mBaselineIconScale;
    }
    private float scaleXForNumItems(int numItems) {
            float gapScaleX =getGapScaleX();
            return MAX_SCALE +gapScaleX/2;
    }
    private float scaleYForNumItems(int numItems) {
            float gapScaleY = getGapScaleY();
            return MAX_SCALE + gapScaleY/2;
    }
//    @Override
//    public int numItems() {
//        return MAX_NUM_ITEMS_IN_PREVIEW;
//    }



    @Override
    public float scaleForItem(int index, int totalNumItems) {
        return MAX_SCALE;
    }

    @Override
    public float getIconSize() {
        return mIconSize;
    }

    @Override
    public int maxNumItems() {
        return MAX_NUM_ITEMS_IN_PREVIEW;
    }

    @Override
    public boolean clipToBackground() {
        return true;
    }

    @Override
    public boolean hasEnterExitIndices() {
        return false;
    }

    @Override
    public int getExitIndex() {
        return 0;
    }

    @Override
    public int getEnterIndex() {
        return 0;
    }

    private float getGapScaleX(){
        return  (1 - MAX_COLUMN * MAX_SCALE) / (MAX_COLUMN - 1);
    }
    private float getGapScaleY(){
        return  (1 - MAX_ROW * MAX_SCALE) / (MAX_ROW - 1);
    }

    private float getPaddingX(int index) {
        int i = index%3;
        if(i == 0){
            return folderPaddingLeft*(mIsRtl?-1:1);
        }else if(i == 1){
            return 0;
        }else if(i == 2){
            return folderPaddingRight*(mIsRtl?-1:1);
        }
        return 0;
    }
    private float getPaddingY(int index) {
        int i = index/3;
        if(i == 0){
            return folderPaddingTop;
        }else if(i == 1){
            return 0;
        }else if(i == 2){
            return folderPaddingBottom;
        }
        return 0;
    }
}
