package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.PhotoUtils;


/**
 * Created by xiaopeng on 17-11-14.
 */

public class FolderPreviewDrawable {
    private static FolderPreviewDrawable sInstance = null;
    private final Matrix mMatrix;
    private final Rect mPadding, mBoundWithoutAlpha;
    private Bitmap mBitmap;
    private int mColor;
    private int mRoundRadius;
    private float mScale;

    private FolderPreviewDrawable() {
        mMatrix = new Matrix();
        mPadding = new Rect();
        mBoundWithoutAlpha = new Rect();
    }

    public void draw(Canvas canvas, float width, float height, Paint paint) {
        if (!isNull()) {
            mMatrix.reset();
            mMatrix.postScale(width/mBitmap.getWidth(), height/mBitmap.getHeight(),
                    mBitmap.getWidth()/2.0f, mBitmap.getHeight()/2.0f);
            mMatrix.postTranslate((width-mBitmap.getWidth())/2.0f, (width-mBitmap.getHeight())/2.0f);
            canvas.drawBitmap(mBitmap, mMatrix, paint);
        }
    }

    public static FolderPreviewDrawable getInstance() {
        if (sInstance == null) {
            sInstance = new FolderPreviewDrawable();
        }
        return sInstance;
    }

    private void get(Context context) {
        if (Utilities.supportLegacyFolderIcon()) {
            return;
        }

        if (mBitmap != null) {
            return;
        }

        Bitmap bmp = PhotoUtils.drawable2bitmap(context.getDrawable(R.drawable.bg_folder));
        if (bmp == null) {
            return;
        }

        mBitmap = bmp;//LauncherIcons.obtain(context).createIconBitmapWithShadow(bmp, false);
        mColor = mBitmap.getPixel(mBitmap.getWidth() / 2, mBitmap.getHeight() / 2);
        mRoundRadius = getBitmapAlphaOffset(bmp, mPadding);
        mBoundWithoutAlpha.set(mPadding.left, mPadding.top, bmp.getWidth()-mPadding.right, bmp.getHeight()-mPadding.bottom);
        mScale = mBoundWithoutAlpha.width() / (float)bmp.getWidth();
    }

    public void reset(Context context) {
        if (!isNull()) {
            mBitmap.recycle();
        }
        mBitmap = null;
        get(context);
    }

    public boolean isNull() {
        return mBitmap == null || mBitmap.isRecycled();
    }

    public int getWidth() {
        if (isNull()) {
            return 0;
        }

        return mBitmap.getWidth();
    }

    public int getHeight() {
        if (isNull()) {
            return 0;
        }

        return mBitmap.getHeight();
    }

    public int getColor() {
        return mColor;
    }

    public Rect getAlphaPadding() {
        return mPadding;
    }

    public int getRoundRadius() {
        return mRoundRadius;
    }

    public Rect getBoundWithoutAlpha() {
        return mBoundWithoutAlpha;
    }

    public float getNoAlphaScale() {
        return mScale;
    }

    private int getBitmapAlphaOffset(Bitmap bitmap, Rect padding) {
        return Utilities.getBitmapAlphaOffset(bitmap, padding, 0);
    }
}
