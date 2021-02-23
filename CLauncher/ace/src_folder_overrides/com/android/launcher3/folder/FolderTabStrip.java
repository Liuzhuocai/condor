package com.android.launcher3.folder;

/**
 * Created by wangqing on 12/23/15.
 */

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;

public class FolderTabStrip extends FolderNameStrip implements Insettable{
    private static final String TAG = "FolderTabStrip";
    private int mIndicatorColor;
    private int mMinPaddingBottom;
    private int mMinTextSpacing;
    private int mMinStripHeight;
    private int mTabPadding;
    private final Paint mTabPaint;
    private boolean mDrawFullUnderline;
    private boolean mDrawFullUnderlineSet;
    private boolean mIgnoreTap;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private int mTouchSlop;

    public FolderTabStrip(Context context) {
        this(context, (AttributeSet)null);
    }

    public FolderTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTabPaint = new Paint();
        mDrawFullUnderline = false;
        mDrawFullUnderlineSet = false;
        mIndicatorColor = mTextColor;
        mTabPaint.setColor(mIndicatorColor);
        float density = context.getResources().getDisplayMetrics().density;
        mMinPaddingBottom = (int)(6.0F * density + 0.5F);
        mMinTextSpacing = (int)(64.0F * density);
        mTabPadding = (int)(16.0F * density + 0.5F);
        mMinStripHeight = (int)(32.0F * density + 0.5F);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        setTextSpacing(getTextSpacing());
        setWillNotDraw(false);
        mPrevText.setFocusable(true);
        mPrevText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() - 1, true);
            }
        });
        mNextText.setFocusable(true);
        mNextText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
            }
        });
        if(getBackground() == null) {
            mDrawFullUnderline = true;
        }
        mDrawFullUnderline = false;

    }

    public void setTabIndicatorColor(int color) {
        mIndicatorColor = color;
        mTabPaint.setColor(mIndicatorColor);
        invalidate();
    }

    public void setTabIndicatorColorResource(int resId) {
        setTabIndicatorColor(getContext().getResources().getColor(resId));
    }

    public int getTabIndicatorColor() {
        return mIndicatorColor;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        if(bottom < mMinPaddingBottom) {
            bottom = mMinPaddingBottom;
        }

        super.setPadding(left, top, right, bottom);
    }

    public void setTextSpacing(int textSpacing) {
        if(textSpacing < mMinTextSpacing) {
            textSpacing = mMinTextSpacing;
        }

        super.setTextSpacing(textSpacing);
    }

    public void setBackgroundDrawable(Drawable d) {
        super.setBackgroundDrawable(d);
        if(!mDrawFullUnderlineSet) {
            mDrawFullUnderline = d == null;
        }

    }

    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        if(!mDrawFullUnderlineSet) {
            mDrawFullUnderline = (color & -0x1000000) == 0;
        }

    }

    public void setBackgroundResource(int resId) {
        super.setBackgroundResource(resId);
        if(!mDrawFullUnderlineSet) {
            mDrawFullUnderline = resId == 0;
        }

    }

    public void setDrawFullUnderline(boolean drawFull) {
        mDrawFullUnderline = drawFull;
        mDrawFullUnderlineSet = true;
        invalidate();
    }

    public boolean getDrawFullUnderline() {
        return mDrawFullUnderline;
    }

    int getMinHeight() {
        return Math.max(super.getMinHeight(), mMinStripHeight);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action != 0 && mIgnoreTap || isForbid) {
            return false;
        } else {
            float x = ev.getX();
            float y = ev.getY();
            switch(action) {
                case MotionEvent.ACTION_DOWN:
                    mInitialMotionX = x;
                    mInitialMotionY = y;
                    mIgnoreTap = false;
                    break;
                case MotionEvent.ACTION_UP:
                    if (mCurrText.hasFocus()) {
                        break;
                    }

                    if(x < (float)(mCurrText.getLeft() - mTabPadding)) {
                        mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                    } else if(x > (float)(mCurrText.getRight() + mTabPadding)) {
                        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(Math.abs(x - mInitialMotionX) > (float)mTouchSlop
                            || Math.abs(y - mInitialMotionY) > (float)mTouchSlop) {
                        mIgnoreTap = true;
                    }
                    break;
                default:
                    break;
            }

            return true;
        }
    }

    @Override
    public void setInsets(Rect insets) {
        DeviceProfile grid = Launcher.getLauncher(getContext()).getDeviceProfile();
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
        if(grid.isVerticalBarLayout()) {
            lp.leftMargin = insets.left;
            lp.rightMargin = insets.right;
        } else {
            lp.leftMargin = 0;
            lp.rightMargin = 0;
        }
    }
}
