package com.condor.launcher.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Themes;
import com.condor.launcher.util.Utils;

/**
 * Created by Perry on 19-1-24
 */
public class WaveView extends View {
    private static final float DEFAULT_AMPLITUDE_RATIO = 0.05f;
    private static final float DEFAULT_WATER_LEVEL_RATIO = 0.5f;
    private static final float DEFAULT_WAVE_LENGTH_RATIO = 1.0f;
    private static final float DEFAULT_WAVE_SHIFT_RATIO = 0.0f;

    private static final int DEFAULT_BOTTLE_COLOR = 0x40dddddd;
    private static final int DEFAULT_BEHIND_WAVE_COLOR = 0x3000a6ce;
    private static final int DEFAULT_FRONT_WAVE_COLOR = 0x8000a6ce;
    private static final int DEFAULT_SHADOW_WIDTH = 3;

    private BitmapShader mWaveShader;
    private Matrix mShaderMatrix;
    private Paint mWavePaint;
    private Paint mBottlePaint;
    private Paint mContentPaint;

    private float mWaterLevel;

    private float mAmplitudeRatio = DEFAULT_AMPLITUDE_RATIO;
    private float mWaveLengthRatio = DEFAULT_WAVE_LENGTH_RATIO;
    private float mWaterLevelRatio = DEFAULT_WATER_LEVEL_RATIO;
    private float mWaveShiftRatio = DEFAULT_WAVE_SHIFT_RATIO;

    private int mBottleColor = DEFAULT_BOTTLE_COLOR;
    private int mShadowWidth = DEFAULT_SHADOW_WIDTH;
    private int mBehindWaveColor = DEFAULT_BEHIND_WAVE_COLOR;
    private int mFrontWaveColor = DEFAULT_FRONT_WAVE_COLOR;
    private ContentType mContentType = ContentType.PROGRESS;
    private Drawable mClear;

    public enum ContentType {
        PROGRESS,
        CLEAR
    }

    private ObjectAnimator mWaveShiftAnim;
    private final Rect mBound = new Rect();

    public WaveView(Context context) {
        super(context);
        init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mShaderMatrix = new Matrix();
        mWavePaint = new Paint();
        mWavePaint.setAntiAlias(true);

        mBottlePaint = new Paint();
        mBottlePaint.setAntiAlias(true);
        mBottlePaint.setColor(mBottleColor);
        mBottlePaint.setShadowLayer(mShadowWidth, 0, mShadowWidth,
                Utils.getDarkerColor(mBottleColor));

        mContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mContentPaint.setColor(Themes.getAttrColor(getContext(),
                android.R.attr.textColorSecondary));

        mClear = getResources().getDrawable(R.drawable.ic_clear,
                getContext().getTheme());

        mWaveShiftAnim = ObjectAnimator.ofFloat(
                this, "waveShiftRatio", 0f, 1f);
        mWaveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
        mWaveShiftAnim.setDuration(2000);
        mWaveShiftAnim.setInterpolator(new LinearInterpolator());
    }

    public float getWaveShiftRatio() {
        return mWaveShiftRatio;
    }

    public void setWaveShiftRatio(float waveShiftRatio) {
        if (mWaveShiftRatio != waveShiftRatio) {
            mWaveShiftRatio = waveShiftRatio;
            invalidate();
        }
    }

    public float getWaterLevelRatio() {
        return mWaterLevelRatio;
    }

    public void setWaterLevelRatio(float waterLevelRatio) {
        if (mWaterLevelRatio != waterLevelRatio) {
            mWaterLevelRatio = waterLevelRatio;
            invalidate();
        }
    }

    public float getAmplitudeRatio() {
        return mAmplitudeRatio;
    }

    public void setAmplitudeRatio(float amplitudeRatio) {
        if (mAmplitudeRatio != amplitudeRatio) {
            mAmplitudeRatio = amplitudeRatio;
            invalidate();
        }
    }

    public float getWaveLengthRatio() {
        return mWaveLengthRatio;
    }

    public void setWaveLengthRatio(float waveLengthRatio) {
        mWaveLengthRatio = waveLengthRatio;
    }

    public void setWaveColor(int behindWaveColor, int frontWaveColor) {
        mBehindWaveColor = behindWaveColor;
        mFrontWaveColor = frontWaveColor;

        if (getWidth() > 0 && getHeight() > 0) {
            mWaveShader = null;
            createShader();
            invalidate();
        }
    }

    public void setBottleColor(int bottleColor) {
        if (mBottleColor != bottleColor) {
            mBottleColor = bottleColor;
            mBottlePaint.setShadowLayer(mShadowWidth, 0, mShadowWidth,
                    Utils.getDarkerColor(mBottleColor));
            invalidate();
        }
    }

    public void setContentType(ContentType contentType) {
        if (mContentType != contentType) {
            mContentType = contentType;
            invalidate();
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (getVisibility() == visibility) {
            return;
        }
        if (visibility == GONE || visibility == INVISIBLE) {
            mWaveShiftAnim.cancel();
        } else {
            mWaveShiftAnim.cancel();
            // Once animation is turned on, energy consumption increases,
            // so do not start animation
            //mWaveShiftAnim.start();
        }
        super.setVisibility(visibility);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        createShader();
        int left = (getWidth() - mClear.getIntrinsicWidth()) / 2;
        int top  = (getHeight() - mClear.getIntrinsicHeight()) / 2;
        int right= left + mClear.getIntrinsicWidth();
        int bottom = top + mClear.getIntrinsicHeight();
        mClear.setBounds(left, top, right, bottom);
    }

    private void createShader() {
        double angularFrequency = 2.0f * Math.PI / DEFAULT_WAVE_LENGTH_RATIO / getWidth();
        double amplitude = getHeight() * DEFAULT_AMPLITUDE_RATIO;
        mWaterLevel = getHeight() * DEFAULT_WATER_LEVEL_RATIO;

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint wavePaint = new Paint();
        wavePaint.setStrokeWidth(2);
        wavePaint.setAntiAlias(true);

        final int endX = getWidth() + 1;
        final int endY = getHeight() + 1;

        float[] waveY = new float[endX];

        wavePaint.setColor(mBehindWaveColor);
        for (int beginX = 0; beginX < endX; beginX++) {
            double wx = beginX * angularFrequency;
            float beginY = (float) (mWaterLevel + amplitude * Math.sin(wx));
            canvas.drawLine(beginX, beginY, beginX, endY, wavePaint);

            waveY[beginX] = beginY;
        }

        wavePaint.setColor(mFrontWaveColor);
        final int wave2Shift = getWidth() / 4;
        for (int beginX = 0; beginX < endX; beginX++) {
            canvas.drawLine(beginX, waveY[(beginX + wave2Shift) % endX], beginX, endY, wavePaint);
        }

        mWaveShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
        mWavePaint.setShader(mWaveShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveShader != null) {
            if (mWavePaint.getShader() == null) {
                mWavePaint.setShader(mWaveShader);
            }

            mShaderMatrix.setScale(
                    mWaveLengthRatio / DEFAULT_WAVE_LENGTH_RATIO,
                    mAmplitudeRatio / DEFAULT_AMPLITUDE_RATIO,
                    0,
                    mWaterLevel);
            mShaderMatrix.postTranslate(
                    mWaveShiftRatio * getWidth(),
                    (DEFAULT_WATER_LEVEL_RATIO - mWaterLevelRatio) * getHeight());

            mWaveShader.setLocalMatrix(mShaderMatrix);

            float radius = getWidth() / 2f - mShadowWidth;
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, mBottlePaint);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, mWavePaint);
            drawContent(canvas);
        } else {
            mWavePaint.setShader(null);
        }
    }

    private void drawContent(Canvas canvas) {
        if (mContentType == ContentType.PROGRESS) {
            drawProgress(canvas);
        } else if (mContentType == ContentType.CLEAR) {
            drawClear(canvas);
        }
    }

    private void drawProgress(Canvas canvas) {
        int dp24 = dp2px(24);
        int dp12 = dp2px(12);
        String text = String.valueOf((int) ((1-mWaterLevelRatio) * 100));
        mContentPaint.setTextSize(dp24);
        mContentPaint.getTextBounds(text, 0, text.length(), mBound);
        int textWidth = mBound.width();
        int textHeight = mBound.height();
        mContentPaint.setTextSize(dp12);
        mContentPaint.getTextBounds("%", 0, 1, mBound);
        int left = (getWidth() - textWidth - mBound.width()) / 2;
        int top  = (getHeight() + textHeight) / 2;
        mContentPaint.setTextSize(dp24);
        canvas.drawText(text, left,  top, mContentPaint);
        mContentPaint.setTextSize(dp12);
        canvas.drawText("%", left + textWidth + mBound.width() / 2,
                (getHeight() + mBound.height()) / 2, mContentPaint);
    }

    private void drawClear(Canvas canvas) {
        mClear.draw(canvas);
    }

    private int dp2px(int dp) {
        return Utilities.pxFromDp(dp, getResources().
                getDisplayMetrics());
    }
}
